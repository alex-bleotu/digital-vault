package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo
import com.digitalvault.core.accessibility.BrowserGuard

object ChromeIncognitoMatcher : SurfaceMatcher {

    override val id = "chrome_incognito"
    override val packageName = "com.android.chrome"
    override val surfaceLabel = "Incognito tabs"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean =
        BrowserGuard.isIncognitoActive(root)
}
