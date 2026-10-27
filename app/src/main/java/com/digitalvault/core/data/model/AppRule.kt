package com.digitalvault.core.data.model

enum class BlockMode {
    FULL_BLOCK,
    SURFACE_BLOCK,
    UNRESTRICTED,
}

data class AppRule(
    val packageName: String,
    val mode: BlockMode,
    val graceSeconds: Int = 5,
    val targetSurfaces: List<String> = emptyList(),
    val allowBreak: Boolean = true,
    val instagramHomeScrollThresholdScreens: Float = 0.5f,
    val isDailyLimitEnabled: Boolean = false,
    val dailyLimitMinutes: Int = 30,
    val isScheduleEnabled: Boolean = false,
    val scheduleStartMinute: Int = 0,
    val scheduleEndMinute: Int = 360,
) {
    val hasTimeRestrictions: Boolean
        get() = mode != BlockMode.FULL_BLOCK && (isDailyLimitEnabled || isScheduleEnabled)

    fun isScheduleActiveAt(minuteOfDay: Int): Boolean {
        if (!isScheduleEnabled || scheduleStartMinute == scheduleEndMinute) {
            return false
        }

        return if (scheduleStartMinute < scheduleEndMinute) {
            minuteOfDay in scheduleStartMinute until scheduleEndMinute
        } else {
            minuteOfDay >= scheduleStartMinute || minuteOfDay < scheduleEndMinute
        }
    }
}
