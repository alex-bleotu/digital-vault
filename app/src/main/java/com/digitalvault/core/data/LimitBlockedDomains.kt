package com.digitalvault.core.data

data class LimitBlockedDomain(val domain: String, val label: String)

object LimitBlockedDomains {

    private val YOUTUBE = listOf(
        LimitBlockedDomain("youtube.com", "time limit"),
        LimitBlockedDomain("youtu.be", "time limit"),
    )

    val BY_PACKAGE: Map<String, List<LimitBlockedDomain>> = mapOf(
        "com.instagram.android" to listOf(LimitBlockedDomain("instagram.com", "time limit")),
        "com.google.android.youtube" to YOUTUBE,
        "app.rvx.android.youtube" to YOUTUBE,
        "com.zhiliaoapp.musically" to listOf(LimitBlockedDomain("tiktok.com", "time limit")),
    )
}
