package com.digitalvault.ui.shield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.digitalvault.ui.theme.VaultTheme

@Composable
fun ShieldScreen(
    modifier: Modifier = Modifier,
    viewModel: ShieldViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val state = viewModel.uiState

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Shield",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (state.blockedDomains.isNotEmpty()) {
                "${state.blockedDomains.size} DOMAINS BLOCKED"
            } else {
                "NO DOMAINS BLOCKED YET"
            },
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
        )

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.newDomainDraft,
                onValueChange = viewModel::updateNewDomainDraft,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                textStyle = MaterialTheme.typography.labelLarge,
                placeholder = {
                    Text(
                        text = "domain.com",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textMuted,
                    )
                },
                singleLine = true,
                shape = VaultTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.addBlockedDomain() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.brass,
                    unfocusedBorderColor = colors.surfaceRaised,
                    cursorColor = colors.brass,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = viewModel::addBlockedDomain,
                shape = VaultTheme.shapes.medium,
                color = colors.brass,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add domain",
                        tint = colors.ink,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (state.blockedDomains.isEmpty()) {
            Surface(
                shape = VaultTheme.shapes.medium,
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Add a domain to block it.\nEnforced by the same engine that blocks apps, no VPN needed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Column {
                state.blockedDomains.forEachIndexed { index, domain ->
                    if (index > 0) {
                        Spacer(Modifier.height(8.dp))
                    }
                    DomainRow(domain = domain, onRemove = { viewModel.removeBlockedDomain(domain) })
                }
            }
        }
    }
}

@Composable
private fun DomainRow(domain: String, onRemove: () -> Unit) {
    val colors = VaultTheme.colors
    val context = LocalContext.current

    Surface(
        shape = VaultTheme.shapes.medium,
        color = colors.surfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("https://www.google.com/s2/favicons?sz=64&domain=$domain")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = domain,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove $domain",
                    tint = colors.textMuted,
                )
            }
        }
    }
}
