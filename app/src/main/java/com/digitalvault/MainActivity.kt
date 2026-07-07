package com.digitalvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.digitalvault.ui.VaultRoot
import com.digitalvault.ui.theme.DigitalVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalVaultTheme {
                VaultRoot()
            }
        }
    }
}
