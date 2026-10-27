package com.digitalvault.ui.update

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.ui.theme.VaultTheme
import java.util.Locale

private const val BYTES_PER_MEGABYTE = 1_048_576.0

private fun formatMegabytes(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / BYTES_PER_MEGABYTE)

@Composable
fun UpdateDialog(viewModel: UpdateViewModel = viewModel()) {
    val colors = VaultTheme.colors
    val info = viewModel.updateInfo ?: return
    val isDownloading = viewModel.status == UpdateStatus.DOWNLOADING
    val downloadFraction = if (viewModel.totalBytes > 0) {
        (viewModel.downloadedBytes.toFloat() / viewModel.totalBytes).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(targetValue = downloadFraction, label = "updateDownloadProgress")

    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        containerColor = colors.surface,
        title = {
            Text(text = "Update available", color = colors.textPrimary)
        },
        text = {
            Column {
                Text(
                    text = when {
                        isDownloading -> "Downloading Digital Vault ${info.versionTag}."
                        viewModel.isDownloaded -> "Digital Vault ${info.versionTag} is downloaded and ready to install."
                        else -> "Digital Vault ${info.versionTag} is ready to install."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                if (isDownloading) {
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        color = colors.brass,
                        trackColor = colors.surfaceRaised,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (viewModel.totalBytes > 0) {
                                "${formatMegabytes(viewModel.downloadedBytes)} of ${formatMegabytes(viewModel.totalBytes)}"
                            } else {
                                formatMegabytes(viewModel.downloadedBytes)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${(downloadFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.brass,
                        )
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
                enabled = !isDownloading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
            ) {
                Text(
                    text = when {
                        isDownloading -> "Downloading"
                        viewModel.isDownloaded -> "Install"
                        viewModel.status == UpdateStatus.FAILED -> "Retry"
                        else -> "Update"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismiss) {
                Text(text = "Later", color = colors.textMuted)
            }
        },
    )
}
