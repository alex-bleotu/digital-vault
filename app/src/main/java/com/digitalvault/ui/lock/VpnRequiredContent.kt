package com.digitalvault.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.digitalvault.ui.theme.DigitalVaultTheme
import com.digitalvault.ui.theme.VaultTheme

@Composable
fun VpnRequiredContent(onActivate: () -> Unit) {
    val colors = VaultTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "VPN protection",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "TURN ON THE VPN TO CONTINUE",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onActivate,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brass, contentColor = colors.ink),
        ) {
            Text(text = "Activate VPN", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun VpnRequiredContentPreview() {
    DigitalVaultTheme {
        VpnRequiredContent(onActivate = {})
    }
}
