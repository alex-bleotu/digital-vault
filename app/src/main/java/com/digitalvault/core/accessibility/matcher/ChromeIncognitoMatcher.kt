package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.BrowserGuard
import com.digitalvault.core.accessibility.ScreenNode

object ChromeIncognitoMatcher : SurfaceMatcher {

    override val id = "chrome_incognito"
    override val packageName = "com.android.chrome"
    override val surfaceLabel = "Incognito tabs"

    override fun isTargetSurface(root: ScreenNode): Boolean =
        BrowserGuard.isIncognitoActive(root)
}
