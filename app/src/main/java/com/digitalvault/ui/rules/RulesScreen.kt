package com.digitalvault.ui.rules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.core.accessibility.matcher.SurfaceMatchers
import com.digitalvault.core.data.model.AppRule
import com.digitalvault.core.data.model.BlockMode
import com.digitalvault.ui.components.ScrollEdge
import com.digitalvault.ui.components.ScrollEdgeShadow
import com.digitalvault.ui.theme.VaultTheme
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val YOUTUBE_SHORTS_PACKAGES = setOf("com.google.android.youtube", "app.rvx.android.youtube")
private val GRACE_SECONDS_STOPS = listOf(0, 1, 5, 10, 15, 20, 30)
private val HOME_SCROLL_THRESHOLD_STOPS = listOf(0.1f, 0.25f, 0.5f, 1f, 2.5f, 5f)
private const val INSTAGRAM_PACKAGE_NAME = "com.instagram.android"

private const val MINUTES_PER_DAY = 24 * 60
private const val AUTO_SAVE_DEBOUNCE_MILLIS = 300L

private enum class TimeField { DAILY_LIMIT, SCHEDULE_START, SCHEDULE_END }

private fun formatClock(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

private fun formatDuration(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

private fun formatUsage(minutes: Long): String =
    if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${"%02d".format(minutes % 60)}m"

private fun formatHomeScrollThreshold(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val state = viewModel.uiState
    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isPickerVisible by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AppRule?>(null) }
    var showDeletedApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel = rememberUpdatedState(viewModel)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentViewModel.value.loadInstalledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appByPackage = remember(state.installedApps) {
        state.installedApps.associateBy { it.packageName }
    }
    val labelByPackage = remember(state.installedApps) {
        state.installedApps.associate { it.packageName to it.label }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Rules",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "WHAT GETS BLOCKED, AND HOW",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
        )

        Spacer(Modifier.height(24.dp))
        if (state.rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nothing is blocked yet.\nAdd an app to set your first rule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (state.installedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.brass)
            }
        } else {
            val (installedRules, deletedRules) = remember(state.rules, appByPackage, labelByPackage) {
                val (installed, deleted) = state.rules.partition { it.packageName in appByPackage }
                installed.sortedBy { (labelByPackage[it.packageName] ?: it.packageName).lowercase() } to
                    deleted.sortedBy { it.packageName.lowercase() }
            }

            val rulesListState = rememberLazyListState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = rulesListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(installedRules, key = { it.packageName }) { rule ->
                        RuleRow(
                            rule = rule,
                            label = labelByPackage[rule.packageName] ?: rule.packageName,
                            icon = appByPackage[rule.packageName]?.icon,
                            onClick = { editingRule = rule },
                            onRemove = { viewModel.removeRule(rule.packageName) },
                        )
                    }

                    if (deletedRules.isNotEmpty()) {
                        item(key = "deleted-apps-toggle") {
                            Surface(
                                onClick = { showDeletedApps = !showDeletedApps },
                                shape = VaultTheme.shapes.medium,
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "+${deletedRules.size} more",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.textMuted,
                                    )
                                    val chevronRotation by animateFloatAsState(
                                        targetValue = if (showDeletedApps) 180f else 0f,
                                        label = "chevron",
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = if (showDeletedApps) "Hide deleted apps" else "Show deleted apps",
                                        tint = colors.brass,
                                        modifier = Modifier.rotate(chevronRotation),
                                    )
                                }
                            }
                        }

                        if (showDeletedApps) {
                            items(deletedRules, key = { it.packageName }) { rule ->
                                RuleRow(
                                    rule = rule,
                                    label = rule.packageName,
                                    icon = null,
                                    onClick = { editingRule = rule },
                                    onRemove = { viewModel.removeRule(rule.packageName) },
                                    showIcon = false,
                                )
                            }
                        }
                    }
                }
                ScrollEdgeShadow(
                    edge = ScrollEdge.BOTTOM,
                    listState = rulesListState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { isPickerVisible = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brass,
                contentColor = colors.ink,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(
                text = "Add app to block",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }

    if (isPickerVisible) {
        val blockedPackages = remember(state.rules) { state.rules.map { it.packageName }.toSet() }

        ModalBottomSheet(
            onDismissRequest = { isPickerVisible = false },
            sheetState = pickerSheetState,
            containerColor = colors.surface,
        ) {
            AppPicker(
                apps = state.installedApps.filterNot { it.packageName in blockedPackages },
                isLoading = state.isLoadingApps,
                onSelect = { packageName ->
                    viewModel.blockApp(packageName)
                    isPickerVisible = false
                },
            )
        }
    }

    val ruleBeingEdited = editingRule
    if (ruleBeingEdited != null) {
        ModalBottomSheet(
            onDismissRequest = { editingRule = null },
            sheetState = editorSheetState,
            containerColor = colors.surface,
        ) {
            RuleEditor(
                rule = ruleBeingEdited,
                label = labelByPackage[ruleBeingEdited.packageName] ?: ruleBeingEdited.packageName,
                icon = appByPackage[ruleBeingEdited.packageName]?.icon,
                usedSecondsToday = state.usageSecondsToday[ruleBeingEdited.packageName] ?: 0L,
                onSave = viewModel::updateRule,
            )
        }
    }
}

@Composable
private fun RuleRow(
    rule: AppRule,
    label: String,
    icon: ImageBitmap?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    showIcon: Boolean = true,
) {
    val colors = VaultTheme.colors
    val isChromeIncognitoRule = rule.packageName == "com.android.chrome"
    val isYouTubeShortsRule = rule.packageName in YOUTUBE_SHORTS_PACKAGES
    val modeLabel = when (rule.mode) {
        BlockMode.FULL_BLOCK -> "FULL BLOCK"
        BlockMode.SURFACE_BLOCK -> when {
            isChromeIncognitoRule -> "INCOGNITO BLOCK"
            isYouTubeShortsRule -> "SHORTS BLOCK · ${rule.graceSeconds}S GRACE"
            else -> "FEED BLOCK · ${rule.graceSeconds}S GRACE"
        }
        BlockMode.UNRESTRICTED -> "TIME LIMITS ONLY"
    }
    val timeLabels = buildList {
        if (rule.mode != BlockMode.FULL_BLOCK && rule.isDailyLimitEnabled) {
            add("${formatDuration(rule.dailyLimitMinutes)} DAILY")
        }
        if (rule.mode != BlockMode.FULL_BLOCK && rule.isScheduleEnabled) {
            add("${formatClock(rule.scheduleStartMinute)}-${formatClock(rule.scheduleEndMinute)}")
        }
    }
    val ruleSummary = (listOf(modeLabel) + timeLabels).joinToString(" · ")
    val modeColor = when (rule.mode) {
        BlockMode.FULL_BLOCK -> colors.rust
        BlockMode.SURFACE_BLOCK -> colors.brass
        BlockMode.UNRESTRICTED -> colors.sage
    }

    Surface(
        onClick = onClick,
        shape = VaultTheme.shapes.medium,
        color = colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showIcon) {
                AppIcon(icon = icon, label = label, size = 40.dp)
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ruleSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = modeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove rule for $label",
                    tint = colors.textMuted,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditor(
    rule: AppRule,
    label: String,
    icon: ImageBitmap?,
    usedSecondsToday: Long,
    onSave: (AppRule) -> Unit,
) {
    val colors = VaultTheme.colors
    val supportsSurfaces = SurfaceMatchers.supportsSurfaceBlock(rule.packageName)
    val isChromeIncognitoRule = rule.packageName == "com.android.chrome"
    val isYouTubeShortsRule = rule.packageName in YOUTUBE_SHORTS_PACKAGES
    val isInstantBlockRule = isChromeIncognitoRule
    val isInstagramRule = rule.packageName == INSTAGRAM_PACKAGE_NAME
    var mode by remember { mutableStateOf(rule.mode) }
    var graceStepIndex by remember {
        mutableStateOf(
            GRACE_SECONDS_STOPS.indices.minBy { index -> abs(GRACE_SECONDS_STOPS[index] - rule.graceSeconds) }.toFloat(),
        )
    }
    val graceSeconds = GRACE_SECONDS_STOPS[graceStepIndex.roundToInt()]
    var homeScrollThresholdStepIndex by remember {
        mutableStateOf(
            HOME_SCROLL_THRESHOLD_STOPS.indices.minBy { index ->
                abs(HOME_SCROLL_THRESHOLD_STOPS[index] - rule.instagramHomeScrollThresholdScreens)
            }.toFloat(),
        )
    }
    val homeScrollThreshold = HOME_SCROLL_THRESHOLD_STOPS[homeScrollThresholdStepIndex.roundToInt()]
    var allowBreak by remember { mutableStateOf(rule.allowBreak) }
    var isDailyLimitEnabled by remember { mutableStateOf(rule.isDailyLimitEnabled) }
    var dailyLimitMinutes by remember { mutableStateOf(rule.dailyLimitMinutes) }
    var isScheduleEnabled by remember { mutableStateOf(rule.isScheduleEnabled) }
    var scheduleStartMinute by remember { mutableStateOf(rule.scheduleStartMinute) }
    var scheduleEndMinute by remember { mutableStateOf(rule.scheduleEndMinute) }
    var editingTimeField by remember { mutableStateOf<TimeField?>(null) }

    val isInstantModeSelected = isInstantBlockRule && mode == BlockMode.SURFACE_BLOCK
    val isNoBreakModeSelected = isChromeIncognitoRule && mode == BlockMode.SURFACE_BLOCK
    val editedRule = rule.copy(
        mode = mode,
        graceSeconds = if (isInstantModeSelected) 0 else graceSeconds,
        targetSurfaces = if (mode == BlockMode.SURFACE_BLOCK) {
            SurfaceMatchers.defaultSurfaceIds(rule.packageName)
        } else {
            emptyList()
        },
        allowBreak = if (isNoBreakModeSelected) false else allowBreak,
        instagramHomeScrollThresholdScreens = homeScrollThreshold,
        isDailyLimitEnabled = isDailyLimitEnabled,
        dailyLimitMinutes = dailyLimitMinutes,
        isScheduleEnabled = isScheduleEnabled,
        scheduleStartMinute = scheduleStartMinute,
        scheduleEndMinute = scheduleEndMinute,
    )
    var lastSavedRule by remember { mutableStateOf(rule) }
    val latestEditedRule by rememberUpdatedState(editedRule)
    val latestOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(editedRule) {
        if (editedRule == lastSavedRule) {
            return@LaunchedEffect
        }
        delay(AUTO_SAVE_DEBOUNCE_MILLIS)
        lastSavedRule = editedRule
        latestOnSave(editedRule)
    }
    DisposableEffect(Unit) {
        onDispose {
            if (latestEditedRule != lastSavedRule) {
                latestOnSave(latestEditedRule)
            }
        }
    }

    val editorScrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon = icon, label = label, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = rule.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .verticalScroll(editorScrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 20.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Used today ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary)) {
                        append(formatUsage(usedSecondsToday / 60))
                    }
                    if (isDailyLimitEnabled) {
                        append(" of ${formatUsage(dailyLimitMinutes.toLong())}")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textMuted,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "MODE",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            Spacer(Modifier.height(12.dp))
            ModeOption(
                title = "Full block",
                detail = "The app closes the moment it opens.",
                isSelected = mode == BlockMode.FULL_BLOCK,
                onSelect = { mode = BlockMode.FULL_BLOCK },
            )
            if (supportsSurfaces) {
                Spacer(Modifier.height(8.dp))
                if (isChromeIncognitoRule) {
                    ModeOption(
                        title = "Incognito block",
                        detail = "Chrome closes immediately the moment an Incognito tab is opened.",
                        isSelected = mode == BlockMode.SURFACE_BLOCK,
                        onSelect = { mode = BlockMode.SURFACE_BLOCK },
                    )
                } else if (isYouTubeShortsRule) {
                    ModeOption(
                        title = "Shorts block",
                        detail = "Only Shorts is blocked. The rest of YouTube is unaffected.",
                        isSelected = mode == BlockMode.SURFACE_BLOCK,
                        onSelect = { mode = BlockMode.SURFACE_BLOCK },
                    )
                } else {
                    ModeOption(
                        title = "Feed block",
                        detail = "The feed is blocked, but videos from friends are still allowed.",
                        isSelected = mode == BlockMode.SURFACE_BLOCK,
                        onSelect = { mode = BlockMode.SURFACE_BLOCK },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ModeOption(
                title = "Time limits only",
                detail = "The app stays open until the daily limit or schedule below kicks in.",
                isSelected = mode == BlockMode.UNRESTRICTED,
                onSelect = { mode = BlockMode.UNRESTRICTED },
            )

            if (mode == BlockMode.SURFACE_BLOCK && !isInstantBlockRule) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "GRACE PERIOD · ${graceSeconds}S",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
                val sliderInteractionSource = remember { MutableInteractionSource() }
                Slider(
                    value = graceStepIndex,
                    onValueChange = { graceStepIndex = it },
                    valueRange = 0f..(GRACE_SECONDS_STOPS.size - 1).toFloat(),
                    steps = GRACE_SECONDS_STOPS.size - 2,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    interactionSource = sliderInteractionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = sliderInteractionSource,
                            thumbSize = DpSize(4.dp, 20.dp),
                            colors = SliderDefaults.colors(thumbColor = colors.brass),
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.brass,
                        activeTrackColor = colors.brass,
                        inactiveTrackColor = colors.surfaceRaised,
                    ),
                )
            }

            if (mode == BlockMode.SURFACE_BLOCK && isInstagramRule) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "HOME SCROLL BLOCK · ${formatHomeScrollThreshold(homeScrollThreshold)}X",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
                val homeScrollSliderInteractionSource = remember { MutableInteractionSource() }
                Slider(
                    value = homeScrollThresholdStepIndex,
                    onValueChange = { homeScrollThresholdStepIndex = it },
                    valueRange = 0f..(HOME_SCROLL_THRESHOLD_STOPS.size - 1).toFloat(),
                    steps = HOME_SCROLL_THRESHOLD_STOPS.size - 2,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    interactionSource = homeScrollSliderInteractionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = homeScrollSliderInteractionSource,
                            thumbSize = DpSize(4.dp, 20.dp),
                            colors = SliderDefaults.colors(thumbColor = colors.brass),
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.brass,
                        activeTrackColor = colors.brass,
                        inactiveTrackColor = colors.surfaceRaised,
                    ),
                )
            }

            if (mode != BlockMode.FULL_BLOCK) {
                Spacer(Modifier.height(12.dp))
                SettingSwitchRow(
                    title = "DAILY TIME LIMIT",
                    detail = "Block the app after using it this long today.",
                    isChecked = isDailyLimitEnabled,
                    onCheckedChange = { isDailyLimitEnabled = it },
                )
                if (isDailyLimitEnabled) {
                    Spacer(Modifier.height(8.dp))
                    TimeValueButton(
                        label = "LIMIT",
                        value = formatDuration(dailyLimitMinutes),
                        onClick = { editingTimeField = TimeField.DAILY_LIMIT },
                    )
                }

                Spacer(Modifier.height(12.dp))
                SettingSwitchRow(
                    title = "SCHEDULED BLOCK",
                    detail = "Block the app completely during these hours.",
                    isChecked = isScheduleEnabled,
                    onCheckedChange = { isScheduleEnabled = it },
                )
                if (isScheduleEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeValueButton(
                            label = "FROM",
                            value = formatClock(scheduleStartMinute),
                            onClick = { editingTimeField = TimeField.SCHEDULE_START },
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .size(width = 14.dp, height = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(colors.textMuted),
                        )
                        TimeValueButton(
                            label = "TO",
                            value = formatClock(scheduleEndMinute),
                            onClick = { editingTimeField = TimeField.SCHEDULE_END },
                        )
                    }
                }
            }

            if (!isNoBreakModeSelected) {
                Spacer(Modifier.height(12.dp))
                SettingSwitchRow(
                    title = "ALLOW 5-MINUTE BREAK",
                    detail = "Unlock this app for 5 minutes, once a day.",
                    isChecked = allowBreak,
                    onCheckedChange = { allowBreak = it },
                )
            }

        }
        ScrollEdgeShadow(
            edge = ScrollEdge.TOP,
            scrollState = editorScrollState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    val timeField = editingTimeField
    if (timeField != null) {
        val initialMinutes = when (timeField) {
            TimeField.DAILY_LIMIT -> dailyLimitMinutes
            TimeField.SCHEDULE_START -> scheduleStartMinute
            TimeField.SCHEDULE_END -> scheduleEndMinute
        }
        TimeValueDialog(
            title = when (timeField) {
                TimeField.DAILY_LIMIT -> "Daily limit"
                TimeField.SCHEDULE_START -> "Block from"
                TimeField.SCHEDULE_END -> "Block until"
            },
            initialMinutes = initialMinutes,
            onDismiss = { editingTimeField = null },
            onConfirm = { minutes ->
                when (timeField) {
                    TimeField.DAILY_LIMIT -> dailyLimitMinutes = minutes.coerceAtLeast(1)
                    TimeField.SCHEDULE_START -> scheduleStartMinute = minutes
                    TimeField.SCHEDULE_END -> scheduleEndMinute = minutes
                }
                editingTimeField = null
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    detail: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = VaultTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.brass,
                checkedTrackColor = colors.surfaceRaised,
            ),
        )
    }
}

@Composable
private fun TimeValueButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VaultTheme.colors

    Surface(
        onClick = onClick,
        shape = VaultTheme.shapes.medium,
        color = colors.surfaceRaised,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.brass,
            )
        }
    }
}

@Composable
private fun TimeValueDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = VaultTheme.colors
    val clampedMinutes = initialMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
    var hoursValue by remember { mutableStateOf(twoDigitFieldValue(clampedMinutes / 60)) }
    var minutesValue by remember { mutableStateOf(twoDigitFieldValue(clampedMinutes % 60)) }
    val minutesFocusRequester = remember { FocusRequester() }

    fun handleHoursChange(newValue: TextFieldValue) {
        val previousText = hoursValue.text
        val isCursorAtEnd = hoursValue.selection.collapsed && hoursValue.selection.start == previousText.length
        val overflowDigit = newValue.text.lastOrNull()
        val isOverflowTyping = previousText.length == 2 &&
            isCursorAtEnd &&
            newValue.text.length == 3 &&
            newValue.text.startsWith(previousText) &&
            overflowDigit?.isDigit() == true
        if (isOverflowTyping) {
            val digit = overflowDigit.toString()
            minutesValue = TextFieldValue(text = digit, selection = TextRange(digit.length))
            minutesFocusRequester.requestFocus()

            return
        }
        hoursValue = sanitizeTimeFieldValue(newValue)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = VaultTheme.shapes.large,
            color = colors.surface,
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeNumberField(
                        value = hoursValue,
                        onValueChange = ::handleHoursChange,
                        accessibilityLabel = "Hours",
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    TimeNumberField(
                        value = minutesValue,
                        onValueChange = { minutesValue = sanitizeTimeFieldValue(it) },
                        accessibilityLabel = "Minutes",
                        modifier = Modifier.focusRequester(minutesFocusRequester),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = colors.textMuted)
                    }
                    TextButton(
                        onClick = {
                            val hours = (hoursValue.text.toIntOrNull() ?: 0).coerceIn(0, 23)
                            val minutes = (minutesValue.text.toIntOrNull() ?: 0).coerceIn(0, 59)
                            onConfirm(hours * 60 + minutes)
                        },
                    ) {
                        Text(text = "Set", color = colors.brass)
                    }
                }
            }
        }
    }
}

private fun twoDigitFieldValue(number: Int): TextFieldValue {
    val text = "%02d".format(number)

    return TextFieldValue(text = text, selection = TextRange(text.length))
}

private fun sanitizeTimeFieldValue(value: TextFieldValue): TextFieldValue {
    val digits = value.text.filter { it.isDigit() }.take(2)
    if (digits == value.text) {
        return value
    }

    return TextFieldValue(text = digits, selection = TextRange(digits.length))
}

@Composable
private fun TimeNumberField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = VaultTheme.colors

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            color = colors.brass,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(colors.brass),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.semantics { contentDescription = accessibilityLabel },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 60.dp)
                    .clip(VaultTheme.shapes.medium)
                    .background(colors.surfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun ModeOption(
    title: String,
    detail: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = VaultTheme.colors

    Surface(
        onClick = onSelect,
        shape = VaultTheme.shapes.medium,
        color = if (isSelected) colors.surfaceRaised else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.brass else colors.surfaceRaised,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) colors.brass else colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun AppPicker(
    apps: List<InstalledApp>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = VaultTheme.colors
    var showAllApps by remember { mutableStateOf(false) }

    val popularApps = remember(apps) {
        val order = POPULAR_APP_PACKAGES.withIndex().associate { (index, pkg) -> pkg to index }
        apps.filter { it.packageName in order }.sortedBy { order.getValue(it.packageName) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Text(
                text = "Choose an app",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.brass)
                }
            }

            return@LazyColumn
        }

        if (popularApps.isNotEmpty()) {
            item {
                Text(
                    text = "POPULAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(12.dp))
                popularApps.chunked(5).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { app ->
                            PopularAppTile(
                                app = app,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelect(app.packageName) },
                            )
                        }
                        repeat(5 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            Surface(
                onClick = { showAllApps = !showAllApps },
                shape = VaultTheme.shapes.medium,
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "All apps",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                    )
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (showAllApps) 180f else 0f,
                        label = "chevron",
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (showAllApps) "Hide all apps" else "Show all apps",
                        tint = colors.brass,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (showAllApps) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(app.packageName) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(icon = app.icon, label = app.label, size = 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularAppTile(
    app: InstalledApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = VaultTheme.colors

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(icon = app.icon, label = app.label, size = 48.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AppIcon(
    icon: ImageBitmap?,
    label: String,
    size: Dp,
) {
    val colors = VaultTheme.colors
    val shape = RoundedCornerShape(size / 4)

    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(colors.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textMuted,
            )
        }
    }
}
