package com.digitalvault.core.accessibility

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IncognitoNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshFromActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isIncognitoNotification(sbn)) {
            isChromeIncognitoActive = true
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isIncognitoNotification(sbn)) {
            refreshFromActiveNotifications()
        }
    }

    private fun refreshFromActiveNotifications() {
        isChromeIncognitoActive = activeNotifications?.any { isIncognitoNotification(it) } == true
    }

    private fun isIncognitoNotification(sbn: StatusBarNotification): Boolean =
        sbn.packageName == CHROME_PACKAGE_NAME && sbn.tag == INCOGNITO_NOTIFICATION_TAG

    companion object {
        private const val CHROME_PACKAGE_NAME = "com.android.chrome"
        private const val INCOGNITO_NOTIFICATION_TAG = "incognito_tabs_open"

        @Volatile
        var isChromeIncognitoActive: Boolean = false
            private set
    }
}
