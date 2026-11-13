package com.digitalvault

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.digitalvault.core.service.VaultNotifications
import com.digitalvault.core.vpn.VaultVpnService
import com.digitalvault.ui.VaultRoot
import com.digitalvault.ui.theme.DigitalVaultTheme

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VaultVpnService.restart(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalVaultTheme {
                VaultRoot()
            }
        }
        if (savedInstanceState == null) {
            handleRestoreVpn(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRestoreVpn(intent)
    }

    private fun handleRestoreVpn(intent: Intent?) {
        if (intent?.getBooleanExtra(VaultNotifications.EXTRA_RESTORE_VPN, false) != true) {
            return
        }
        intent.removeExtra(VaultNotifications.EXTRA_RESTORE_VPN)
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent == null) {
            VaultVpnService.restart(this)
        } else {
            try {
                vpnPermissionLauncher.launch(prepareIntent)
            } catch (_: ActivityNotFoundException) {
            }
        }
    }
}
