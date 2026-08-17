package com.digitalvault.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.digitalvault.R
import com.digitalvault.core.accessibility.matcher.InstagramReelsMatcher
import com.digitalvault.core.accessibility.matcher.InstagramShareMatcher
import com.digitalvault.core.accessibility.matcher.SurfaceMatcher
import com.digitalvault.core.accessibility.matcher.SurfaceMatchers
import com.digitalvault.core.accessibility.matcher.YouTubeRvxShortsMatcher
import com.digitalvault.core.accessibility.matcher.YouTubeShortsMatcher
import com.digitalvault.core.accessibility.matcher.findVisibleNodesByText
import com.digitalvault.core.accessibility.matcher.hasDescendantWithExactText
import com.digitalvault.core.accessibility.matcher.hasVisibleNodeWithExactText
import com.digitalvault.core.data.BreakUsageRepository
import com.digitalvault.core.data.DnsRepository
import com.digitalvault.core.data.RulesRepository
import com.digitalvault.core.data.VaultRepository
import com.digitalvault.core.data.model.AppRule
import com.digitalvault.core.data.model.BlockMode
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.ui.lock.BlockOverlayController
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val DISMISS_MINUTES = 5
private const val HOLD_MILLIS = 1600
private const val ICON_SIZE_PX = 144
private const val SETTINGS_UNLOCK_SECONDS = 60L
private const val SELF_TRIGGERED_HOME_GUARD_MILLIS = 1_000L
private const val INSTAGRAM_SETTLE_GUARD_MILLIS = 350L
private const val FAST_TRIGGER_SETTLE_MILLIS = 400L
private val FAST_TRIGGER_SURFACE_IDS = setOf("instagram_share")
private const val HOME_SCROLL_TRIGGER_SCREEN_HEIGHT_FRACTION = 1.5
private const val HOME_SCROLL_REPEAT_TRIGGER_SCREEN_HEIGHT_FRACTION = 0.1
private const val TIKTOK_PACKAGE_NAME = "com.zhiliaoapp.musically"
private val AUDIO_STOP_PACKAGES = setOf(YouTubeShortsMatcher.packageName, YouTubeRvxShortsMatcher.packageName)

class VaultAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var overlayController: BlockOverlayController? = null
    private var vaultRepository: VaultRepository? = null
    private var breakUsageRepository: BreakUsageRepository? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    @Volatile
    private var breakLastUsedAtMillisByPackage: Map<String, Long> = emptyMap()

    @Volatile
    private var fullBlockRules: Map<String, AppRule> = emptyMap()

    @Volatile
    private var surfaceRules: Map<String, AppRule> = emptyMap()

    @Volatile
    private var isPasswordSet: Boolean = false

    @Volatile
    private var settingsUnlockedUntilMillis: Long = 0L

    @Volatile
    private var blockedDomains: Set<String> = emptySet()

    private val allowedUntil = mutableMapOf<String, Long>()
    private val surfaceEntries = mutableMapOf<String, SurfaceEntry>()
    private var activeBlockedPackage: String? = null
    private var systemDialogsReceiver: BroadcastReceiver? = null

    @Volatile
    private var isInstagramSettingsOrProfile: Boolean = false

    @Volatile
    private var allowedInstagramReelIdentity: String? = null

    @Volatile
    private var isTikTokBackNavigated: Boolean = false

    @Volatile
    private var instagramBackReelLockedIdentity: String? = null

    @Volatile
    private var isInstagramBackReelExempt: Boolean = false

    @Volatile
    private var isInstagramReelContext: Boolean = false

    @Volatile
    private var isInstagramHomeFeed: Boolean = true

    @Volatile
    private var instagramHomeScrollAccumulatedPx: Int = 0

    @Volatile
    private var hasInstagramHomeScrollTriggeredOnce: Boolean = false

    @Volatile
    private var selfTriggeredHomeAtMillis: Long = 0L

    private class SurfaceEntry {
        var isInTarget: Boolean = false
        var graceJob: Job? = null
    }

    private val selfTriggeredHomeGuardRetryJobs = mutableMapOf<String, Job>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayController = BlockOverlayController(this)
        audioManager = ContextCompat.getSystemService(this, AudioManager::class.java)
        val repository = VaultRepository(vaultDataStore)
        vaultRepository = repository
        val breakUsage = BreakUsageRepository(vaultDataStore)
        breakUsageRepository = breakUsage

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val sinceSelfTriggeredHome = System.currentTimeMillis() - selfTriggeredHomeAtMillis
                if (sinceSelfTriggeredHome < SELF_TRIGGERED_HOME_GUARD_MILLIS) {
                    return
                }
                if (overlayController?.isShowing == true) {
                    activeBlockedPackage = null
                    overlayController?.hide()
                    abandonAudioFocus()
                }
            }
        }
        systemDialogsReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        serviceScope.launch {
            RulesRepository(vaultDataStore).rules.collectLatest { rules ->
                fullBlockRules = rules
                    .filter { it.mode == BlockMode.FULL_BLOCK }
                    .associateBy { it.packageName }
                surfaceRules = rules
                    .filter { it.mode == BlockMode.SURFACE_BLOCK }
                    .associateBy { it.packageName }
                recomputeMonitoredPackages()
            }
        }
        serviceScope.launch {
            repository.config.collectLatest { config ->
                isPasswordSet = config.isPasswordSet
                settingsUnlockedUntilMillis = config.unlockedForSettingsUntil?.toEpochMilli() ?: 0L
                if (System.currentTimeMillis() < settingsUnlockedUntilMillis) {
                    activeBlockedPackage = null
                    overlayController?.hide()
                }
                recomputeMonitoredPackages()
            }
        }
        serviceScope.launch {
            DnsRepository(vaultDataStore).config.collectLatest { config ->
                blockedDomains = config.blockedDomains
                recomputeMonitoredPackages()
            }
        }
        serviceScope.launch {
            breakUsage.lastUsedAtMillisByPackage.collectLatest { usage ->
                breakLastUsedAtMillisByPackage = usage
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) {
            return
        }
        if (System.currentTimeMillis() < settingsUnlockedUntilMillis) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            -> {
                if (packageName == InstagramZoneGuard.PACKAGE_NAME) {
                    matchedRoot(packageName)?.let { updateInstagramZone(it) }
                    matchedRoot(packageName)?.let { updateInstagramBackReelZone(it) }
                    matchedRoot(packageName)?.let { updateInstagramReelContext(it) }
                    matchedRoot(packageName)?.let { updateInstagramHomeFeedContext(it) }
                }
                if (packageName == TIKTOK_PACKAGE_NAME) {
                    matchedRoot(packageName)?.let { updateTikTokZone(it) }
                }
                if (packageName in SettingsGuard.watchedPackages) {
                    guardSettingsScreen()
                } else {
                    val isBrowserWatched = packageName in BrowserGuard.watchedPackages
                    if (isBrowserWatched) {
                        guardBrowser(packageName)
                    }
                    val rule = surfaceRules[packageName]
                    when {
                        rule != null -> evaluateSurface(packageName, rule)
                        fullBlockRules.containsKey(packageName) ->
                            blockFullApp(packageName, fullBlockRules.getValue(packageName))
                        !isBrowserWatched && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                            activeBlockedPackage = null
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (packageName == InstagramZoneGuard.PACKAGE_NAME) {
                    matchedRoot(packageName)?.let { updateInstagramBackReelZone(it) }
                    matchedRoot(packageName)?.let { updateInstagramReelContext(it) }
                    matchedRoot(packageName)?.let { updateInstagramHomeFeedContext(it) }
                    handleInstagramHomeFeedScroll(event)
                }
                val rule = surfaceRules[packageName] ?: return
                evaluateSurface(packageName, rule)
            }
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        systemDialogsReceiver?.let { runCatching { unregisterReceiver(it) } }
        systemDialogsReceiver = null
        overlayController?.hide()
        overlayController = null
        abandonAudioFocus()
        serviceScope.cancel()
    }

    private fun blockFullApp(packageName: String, rule: AppRule) {
        if (isTemporarilyAllowed(packageName)) {
            return
        }
        if (overlayController?.isShowing == true) {
            return
        }
        if (activeBlockedPackage == packageName) {
            return
        }
        if (isWithinSelfTriggeredHomeGuard()) {
            return
        }
        if (isSuppressedInstagramZone(packageName)) {
            return
        }
        if (isSuppressedInstagramBackReelZone(packageName)) {
            return
        }
        activeBlockedPackage = packageName
        goHome(packageName)
        val usedToday = hasUsedBreakToday(packageName)
        showTimeoutWall(
            loadAppLabel(packageName),
            loadAppIcon(packageName),
            allowBreak = rule.allowBreak && !usedToday,
            breakUsedToday = rule.allowBreak && usedToday,
        ) {
            dismissForBreak(packageName)
        }
    }

    private fun guardBrowser(packageName: String) {
        if (overlayController?.isShowing == true) {
            return
        }
        if (isWithinSelfTriggeredHomeGuard()) {
            return
        }
        val root = rootInActiveWindow ?: return

        if (blockedDomains.isEmpty()) {
            return
        }
        val domain = BrowserGuard.findBlockedDomain(root, blockedDomains, packageName)
        if (domain == null) {
            if (activeBlockedPackage == packageName) {
                activeBlockedPackage = null
            }

            return
        }
        if (activeBlockedPackage == packageName) {
            return
        }
        activeBlockedPackage = packageName
        goHome(packageName)
        showTimeoutWall(
            domain,
            loadAppIcon(packageName),
            allowBreak = false,
            breakUsedToday = false,
            isDomainBlock = true,
        ) {}
    }

    private fun updateInstagramZone(root: AccessibilityNodeInfo) {
        if (InstagramZoneGuard.isMainReelsTab(root)) {
            isInstagramSettingsOrProfile = false
            allowedInstagramReelIdentity = null

            return
        }
        if (InstagramZoneGuard.isSettingsOrOwnProfile(root)) {
            isInstagramSettingsOrProfile = true
            allowedInstagramReelIdentity = null

            return
        }
        if (!isInstagramSettingsOrProfile) {
            return
        }
        val reelIdentity = InstagramZoneGuard.findReelIdentity(root) ?: return
        val lockedIdentity = allowedInstagramReelIdentity
        if (lockedIdentity == null) {
            allowedInstagramReelIdentity = reelIdentity
        } else if (lockedIdentity != reelIdentity) {
            isInstagramSettingsOrProfile = false
            allowedInstagramReelIdentity = null
        }
    }

    private fun isSuppressedInstagramZone(packageName: String): Boolean =
        packageName == InstagramZoneGuard.PACKAGE_NAME && isInstagramSettingsOrProfile

    private fun updateTikTokZone(root: AccessibilityNodeInfo) {
        val isOnMainTabBar = root.findVisibleNodesByText("For You").isNotEmpty() &&
            root.findVisibleNodesByText("Following").isNotEmpty()
        if (isOnMainTabBar) {
            isTikTokBackNavigated = false

            return
        }
        if (root.hasDescendantWithExactText("Back")) {
            isTikTokBackNavigated = true
        }
    }

    private fun isSuppressedTikTokZone(packageName: String): Boolean =
        packageName == TIKTOK_PACKAGE_NAME && isTikTokBackNavigated

    private fun updateInstagramBackReelZone(root: AccessibilityNodeInfo) {
        if (InstagramZoneGuard.isMainReelsTab(root) || isInstagramGridScreen(root)) {
            instagramBackReelLockedIdentity = null
            isInstagramBackReelExempt = false

            return
        }
        val identity = InstagramZoneGuard.findReelIdentity(root) ?: return
        val lockedIdentity = instagramBackReelLockedIdentity
        if (lockedIdentity == null) {
            instagramBackReelLockedIdentity = identity
            isInstagramBackReelExempt = true
        } else if (lockedIdentity != identity) {
            instagramBackReelLockedIdentity = identity
        }
    }

    private fun isInstagramGridScreen(root: AccessibilityNodeInfo): Boolean =
        countDescendantsWithExactDescription(root, "Image of Post") >= 2

    private fun countDescendantsWithExactDescription(node: AccessibilityNodeInfo, target: String): Int {
        var count = if (node.contentDescription?.toString() == target) 1 else 0
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            count += countDescendantsWithExactDescription(child, target)
            if (count >= 2) {
                return count
            }
        }

        return count
    }

    private fun isSuppressedInstagramBackReelZone(packageName: String): Boolean =
        packageName == InstagramZoneGuard.PACKAGE_NAME && isInstagramBackReelExempt

    private fun updateInstagramReelContext(root: AccessibilityNodeInfo) {
        if (InstagramShareMatcher.isTargetSurface(root)) {
            return
        }
        isInstagramReelContext = InstagramReelsMatcher.isTargetSurface(root)
    }

    private fun updateInstagramHomeFeedContext(root: AccessibilityNodeInfo) {
        if (InstagramShareMatcher.isTargetSurface(root)) {
            return
        }
        if (InstagramZoneGuard.isHomeFeed(root)) {
            isInstagramHomeFeed = true

            return
        }
        val hasMainTabBar = root.hasVisibleNodeWithExactText("Home") && root.hasVisibleNodeWithExactText("Profile")
        val isImmersiveReelViewer = !hasMainTabBar && InstagramZoneGuard.findReelIdentity(root) != null
        val isKnownOtherScreen = InstagramZoneGuard.isMainReelsTab(root) ||
            InstagramZoneGuard.isSettingsOrOwnProfile(root) ||
            isInstagramGridScreen(root) ||
            isImmersiveReelViewer
        if (isKnownOtherScreen && isInstagramHomeFeed) {
            isInstagramHomeFeed = false
            instagramHomeScrollAccumulatedPx = 0
        }
    }

    private fun handleInstagramHomeFeedScroll(event: AccessibilityEvent) {
        if (!isInstagramHomeFeed || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }
        val deltaY = event.scrollDeltaY
        if (deltaY <= 0) {
            return
        }
        instagramHomeScrollAccumulatedPx += deltaY
        val triggerFraction = if (hasInstagramHomeScrollTriggeredOnce) {
            HOME_SCROLL_REPEAT_TRIGGER_SCREEN_HEIGHT_FRACTION
        } else {
            HOME_SCROLL_TRIGGER_SCREEN_HEIGHT_FRACTION
        }
        val triggerThresholdPx = (resources.displayMetrics.heightPixels * triggerFraction).toInt()
        if (instagramHomeScrollAccumulatedPx < triggerThresholdPx) {
            return
        }
        instagramHomeScrollAccumulatedPx = 0
        hasInstagramHomeScrollTriggeredOnce = true
        triggerInstagramHomeFeedScrollBlock()
    }

    private fun triggerInstagramHomeFeedScrollBlock() {
        val packageName = InstagramZoneGuard.PACKAGE_NAME
        val rule = surfaceRules[packageName] ?: fullBlockRules[packageName] ?: return
        if (isTemporarilyAllowed(packageName)) {
            return
        }
        if (overlayController?.isShowing == true) {
            return
        }
        if (activeBlockedPackage == packageName) {
            return
        }
        if (isWithinSelfTriggeredHomeGuard()) {
            return
        }
        activeBlockedPackage = packageName
        goHome(packageName)
        val usedToday = hasUsedBreakToday(packageName)
        showTimeoutWall(
            loadAppLabel(packageName),
            loadAppIcon(packageName),
            allowBreak = rule.allowBreak && !usedToday,
            breakUsedToday = rule.allowBreak && usedToday,
        ) {
            dismissForBreak(packageName)
        }
    }

    private fun matchedRoot(packageName: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null

        return if (root.packageName?.toString() == packageName) root else null
    }

    private fun isSurfaceMatcherActive(matcher: SurfaceMatcher, root: AccessibilityNodeInfo): Boolean {
        if (matcher.id in FAST_TRIGGER_SURFACE_IDS && !isInstagramReelContext) {
            return false
        }

        return matcher.isTargetSurface(root)
    }

    private fun matchersFor(packageName: String, rule: AppRule): List<SurfaceMatcher> =
        SurfaceMatchers.forPackage(packageName).filter { matcher ->
            rule.targetSurfaces.isEmpty() || matcher.id in rule.targetSurfaces
        }

    private fun evaluateSurface(packageName: String, rule: AppRule) {
        if (isTemporarilyAllowed(packageName)) {
            resetSurface(packageName)

            return
        }
        if (isWithinSelfTriggeredHomeGuard()) {
            resetSurface(packageName)
            scheduleSelfTriggeredHomeGuardRetry(packageName, rule)

            return
        }
        if (isSuppressedInstagramZone(packageName)) {
            resetSurface(packageName)

            return
        }
        if (isSuppressedTikTokZone(packageName)) {
            resetSurface(packageName)

            return
        }
        if (isSuppressedInstagramBackReelZone(packageName)) {
            resetSurface(packageName)

            return
        }
        val root = matchedRoot(packageName) ?: return
        val matchers = matchersFor(packageName, rule)
        if (matchers.isEmpty()) {
            return
        }
        val matchedMatchers = matchers.filter { isSurfaceMatcherActive(it, root) }
        val isInTarget = matchedMatchers.isNotEmpty()
        val entry = surfaceEntries.getOrPut(packageName) { SurfaceEntry() }

        if (isInTarget && !entry.isInTarget) {
            entry.isInTarget = true
            val graceMillis = rule.graceSeconds * 1_000L
            val settleMillis = if (matchedMatchers.any { it.id in FAST_TRIGGER_SURFACE_IDS }) {
                FAST_TRIGGER_SETTLE_MILLIS
            } else if (packageName == InstagramZoneGuard.PACKAGE_NAME) {
                maxOf(graceMillis, INSTAGRAM_SETTLE_GUARD_MILLIS)
            } else {
                graceMillis
            }
            entry.graceJob = serviceScope.launch {
                delay(settleMillis)
                if (isSuppressedTikTokZone(packageName)) {
                    resetSurface(packageName)
                    return@launch
                }
                if (isSuppressedInstagramBackReelZone(packageName)) {
                    resetSurface(packageName)
                    return@launch
                }
                val currentRoot = matchedRoot(packageName)
                val isStillInTarget = currentRoot != null && matchers.any { isSurfaceMatcherActive(it, currentRoot) }
                if (isStillInTarget) {
                    blockSurface(packageName, rule)
                } else {
                    resetSurface(packageName)
                }
            }
        } else if (!isInTarget && entry.isInTarget) {
            resetSurface(packageName)
        }
    }

    private fun blockSurface(packageName: String, rule: AppRule) {
        resetSurface(packageName)
        if (overlayController?.isShowing == true) {
            return
        }
        if (isWithinSelfTriggeredHomeGuard()) {
            return
        }
        if (isSuppressedInstagramZone(packageName)) {
            return
        }
        if (isSuppressedTikTokZone(packageName)) {
            return
        }
        if (isSuppressedInstagramBackReelZone(packageName)) {
            return
        }
        activeBlockedPackage = packageName
        goHome(packageName)
        val usedToday = hasUsedBreakToday(packageName)
        showTimeoutWall(
            loadAppLabel(packageName),
            loadAppIcon(packageName),
            allowBreak = rule.allowBreak && !usedToday,
            breakUsedToday = rule.allowBreak && usedToday,
        ) {
            dismissForBreak(packageName)
        }
    }

    private fun goHome(packageName: String) {
        selfTriggeredHomeAtMillis = System.currentTimeMillis()
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (packageName in AUDIO_STOP_PACKAGES) {
            requestAudioFocus()
        }
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener {}
            .build()
        audioFocusRequest = request
        manager.requestAudioFocus(request)
        dispatchPauseKeyEvent(manager)
        manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
    }

    private fun dispatchPauseKeyEvent(manager: AudioManager) {
        val eventTime = SystemClock.uptimeMillis()
        manager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
        manager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
        val request = audioFocusRequest ?: return
        manager.abandonAudioFocusRequest(request)
        audioFocusRequest = null
    }

    private fun scheduleSelfTriggeredHomeGuardRetry(packageName: String, rule: AppRule) {
        if (selfTriggeredHomeGuardRetryJobs[packageName]?.isActive == true) {
            return
        }
        selfTriggeredHomeGuardRetryJobs[packageName] = serviceScope.launch {
            delay(SELF_TRIGGERED_HOME_GUARD_MILLIS)
            evaluateSurface(packageName, rule)
        }
    }

    private fun resetSurface(packageName: String) {
        surfaceEntries[packageName]?.let { entry ->
            entry.graceJob?.cancel()
            entry.graceJob = null
            entry.isInTarget = false
        }
    }

    private fun showTimeoutWall(
        appLabel: String,
        appIcon: ImageBitmap?,
        allowBreak: Boolean,
        breakUsedToday: Boolean,
        isDomainBlock: Boolean = false,
        onDismissForBreak: () -> Unit,
    ) {
        overlayController?.showTimeoutWall(
            appLabel = appLabel,
            appIcon = appIcon,
            dismissMinutes = DISMISS_MINUTES,
            holdMillis = HOLD_MILLIS,
            allowBreak = allowBreak,
            breakUsedToday = breakUsedToday,
            isDomainBlock = isDomainBlock,
            onDismissForBreak = onDismissForBreak,
            onExit = ::exitToHome,
        )
    }

    private fun exitToHome() {
        selfTriggeredHomeAtMillis = System.currentTimeMillis()
        activeBlockedPackage = null
        overlayController?.hide()
        performGlobalAction(GLOBAL_ACTION_HOME)
        abandonAudioFocus()
    }

    private fun isWithinSelfTriggeredHomeGuard(): Boolean =
        System.currentTimeMillis() - selfTriggeredHomeAtMillis < SELF_TRIGGERED_HOME_GUARD_MILLIS

    private fun guardSettingsScreen() {
        if (!isPasswordSet) {
            return
        }
        if (System.currentTimeMillis() < settingsUnlockedUntilMillis) {
            return
        }
        if (overlayController?.isShowing == true) {
            return
        }
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() !in SettingsGuard.watchedPackages) {
            return
        }
        if (!SettingsGuard.isProtectedScreen(root, getString(R.string.app_name))) {
            return
        }
        val repository = vaultRepository ?: return
        overlayController?.showVaultPrompt(
            message = "ENTER PASSCODE TO CHANGE PROTECTED SETTINGS",
            onVerify = { repository.verifyPassword(it) },
            onSuccess = {
                settingsUnlockedUntilMillis =
                    System.currentTimeMillis() + SETTINGS_UNLOCK_SECONDS * 1_000L
                serviceScope.launch {
                    repository.unlockSettingsFor(Duration.ofSeconds(SETTINGS_UNLOCK_SECONDS))
                }
                overlayController?.hide()
            },
            onLeave = {
                overlayController?.hide()
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
        )
    }

    private fun dismissForBreak(targetPackage: String) {
        val now = System.currentTimeMillis()
        allowedUntil[targetPackage] = now + DISMISS_MINUTES * 60_000L
        activeBlockedPackage = null
        overlayController?.hide()
        abandonAudioFocus()
        launchApp(targetPackage)
        serviceScope.launch {
            breakUsageRepository?.recordUsed(targetPackage, now)
        }
    }

    private fun isTemporarilyAllowed(targetPackage: String): Boolean {
        val until = allowedUntil[targetPackage] ?: return false
        if (until <= System.currentTimeMillis()) {
            allowedUntil.remove(targetPackage)

            return false
        }

        return true
    }

    private fun hasUsedBreakToday(targetPackage: String): Boolean {
        val lastUsedAtMillis = breakLastUsedAtMillisByPackage[targetPackage] ?: return false
        val zone = ZoneId.systemDefault()
        val lastUsedDate = Instant.ofEpochMilli(lastUsedAtMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()

        return lastUsedDate == today
    }

    private fun recomputeMonitoredPackages() {
        val info = serviceInfo ?: return
        val monitored = buildSet {
            addAll(fullBlockRules.keys)
            addAll(surfaceRules.keys)
            if (isPasswordSet) {
                addAll(SettingsGuard.watchedPackages)
            }
            if (blockedDomains.isNotEmpty()) {
                addAll(BrowserGuard.watchedPackages)
            }
        }
        info.packageNames = if (monitored.isEmpty()) {
            arrayOf(packageName)
        } else {
            monitored.toTypedArray()
        }
        serviceInfo = info
    }

    private fun launchApp(targetPackage: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage) ?: return
        runCatching { startActivity(launchIntent) }
    }

    private fun loadAppLabel(targetPackage: String): String =
        runCatching {
            val applicationInfo = packageManager.getApplicationInfo(targetPackage, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(targetPackage)

    private fun loadAppIcon(targetPackage: String) =
        runCatching {
            packageManager.getApplicationIcon(targetPackage)
                .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                .asImageBitmap()
        }.getOrNull()

    companion object {
        fun componentName(packageName: String): String =
            "$packageName/${VaultAccessibilityService::class.java.name}"
    }
}
