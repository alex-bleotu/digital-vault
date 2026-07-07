package com.digitalvault.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.ui.theme.VaultTheme

@Composable
fun UpdateDialog(viewModel: UpdateViewModel = viewModel()) {
    val colors = VaultTheme.colors
    val info = viewModel.updateInfo ?: return

    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        containerColor = colors.surface,
        title = {
            Text(text = "Update available", color = colors.textPrimary)
        },
        text = {
            Column {
                Text(
                    text = "Digital Vault ${info.versionTag} is ready to install.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                if (viewModel.status == UpdateStatus.DOWNLOADING) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = colors.brass)
                        Spacer(Modifier.width(16.dp))
                        Text(text = "Downloading…", color = colors.textMuted)
                    }
                }
                if (viewModel.status == UpdateStatus.FAILED) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Download failed. Try again later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.rust,
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = viewModel::downloadAndInstall,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
            ) {
                Text(text = "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismiss) {
                Text(text = "Later", color = colors.textMuted)
            }
        },
    )
}
