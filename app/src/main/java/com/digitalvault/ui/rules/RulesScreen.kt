package com.digitalvault.ui.rules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.core.accessibility.matcher.SurfaceMatchers
import com.digitalvault.core.data.model.AppRule
import com.digitalvault.core.data.model.BlockMode
import com.digitalvault.ui.theme.VaultTheme
import kotlin.math.roundToInt

private val YOUTUBE_SHORTS_PACKAGES = setOf("com.google.android.youtube", "app.rvx.android.youtube")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val state = viewModel.uiState
    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isPickerVisible by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AppRule?>(null) }
    var showDeletedApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    val appByPackage = remember(state.installedApps) {
        state.installedApps.associateBy { it.packageName }
    }
    val labelByPackage = remember(state.installedApps) {
        state.installedApps.associate { it.packageName to it.label }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Rules",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "WHAT GETS BLOCKED, AND HOW",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
        )

        Spacer(Modifier.height(24.dp))
        if (state.rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nothing is blocked yet.\nAdd an app to set your first rule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (state.installedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.brass)
            }
        } else {
            val (installedRules, deletedRules) = remember(state.rules, appByPackage) {
                state.rules.partition { it.packageName in appByPackage }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(installedRules, key = { it.packageName }) { rule ->
                    RuleRow(
                        rule = rule,
                        label = labelByPackage[rule.packageName] ?: rule.packageName,
                        icon = appByPackage[rule.packageName]?.icon,
                        onClick = { editingRule = rule },
                        onRemove = { viewModel.removeRule(rule.packageName) },
                    )
                }

                if (deletedRules.isNotEmpty()) {
                    item(key = "deleted-apps-toggle") {
                        Surface(
                            onClick = { showDeletedApps = !showDeletedApps },
                            shape = VaultTheme.shapes.medium,
                            color = colors.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "+${deletedRules.size} more",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textMuted,
                                )
                                val chevronRotation by animateFloatAsState(
                                    targetValue = if (showDeletedApps) 180f else 0f,
                                    label = "chevron",
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (showDeletedApps) "Hide deleted apps" else "Show deleted apps",
                                    tint = colors.brass,
                                    modifier = Modifier.rotate(chevronRotation),
                                )
                            }
                        }
                    }

                    if (showDeletedApps) {
                        items(deletedRules, key = { it.packageName }) { rule ->
                            RuleRow(
                                rule = rule,
                                label = rule.packageName,
                                icon = null,
                                onClick = { editingRule = rule },
                                onRemove = { viewModel.removeRule(rule.packageName) },
                                showIcon = false,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { isPickerVisible = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brass,
                contentColor = colors.ink,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(
                text = "Add app to block",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }

    if (isPickerVisible) {
        val blockedPackages = remember(state.rules) { state.rules.map { it.packageName }.toSet() }

        ModalBottomSheet(
            onDismissRequest = { isPickerVisible = false },
            sheetState = pickerSheetState,
            containerColor = colors.surface,
        ) {
            AppPicker(
                apps = state.installedApps.filterNot { it.packageName in blockedPackages },
                isLoading = state.isLoadingApps,
                onSelect = { packageName ->
                    viewModel.blockApp(packageName)
                    isPickerVisible = false
                },
            )
        }
    }

    val ruleBeingEdited = editingRule
    if (ruleBeingEdited != null) {
        ModalBottomSheet(
            onDismissRequest = { editingRule = null },
            sheetState = editorSheetState,
            containerColor = colors.surface,
        ) {
            RuleEditor(
                rule = ruleBeingEdited,
                label = labelByPackage[ruleBeingEdited.packageName] ?: ruleBeingEdited.packageName,
                onSave = { updated ->
                    viewModel.updateRule(updated)
                    editingRule = null
                },
            )
        }
    }
}

@Composable
private fun RuleRow(
    rule: AppRule,
    label: String,
    icon: ImageBitmap?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    showIcon: Boolean = true,
) {
    val colors = VaultTheme.colors
    val isChromeIncognitoRule = rule.packageName == "com.android.chrome"
    val isYouTubeShortsRule = rule.packageName in YOUTUBE_SHORTS_PACKAGES
    val modeLabel = when (rule.mode) {
        BlockMode.FULL_BLOCK -> "FULL BLOCK"
        BlockMode.SURFACE_BLOCK -> when {
            isChromeIncognitoRule -> "INCOGNITO BLOCK"
            isYouTubeShortsRule -> "SHORTS BLOCK · ${rule.graceSeconds}S GRACE"
            else -> "FEED BLOCK · ${rule.graceSeconds}S GRACE"
        }
        BlockMode.UNRESTRICTED -> "UNRESTRICTED"
    }
    val modeColor = when (rule.mode) {
        BlockMode.FULL_BLOCK -> colors.rust
        BlockMode.SURFACE_BLOCK -> colors.brass
        BlockMode.UNRESTRICTED -> colors.sage
    }

    Surface(
        onClick = onClick,
        shape = VaultTheme.shapes.medium,
        color = colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showIcon) {
                AppIcon(icon = icon, label = label, size = 40.dp)
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = modeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove rule for $label",
                    tint = colors.textMuted,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditor(
    rule: AppRule,
    label: String,
    onSave: (AppRule) -> Unit,
) {
    val colors = VaultTheme.colors
    val supportsSurfaces = SurfaceMatchers.supportsSurfaceBlock(rule.packageName)
    val isChromeIncognitoRule = rule.packageName == "com.android.chrome"
    val isYouTubeShortsRule = rule.packageName in YOUTUBE_SHORTS_PACKAGES
    val isInstantBlockRule = isChromeIncognitoRule
    var mode by remember { mutableStateOf(rule.mode) }
    var graceSeconds by remember { mutableStateOf(rule.graceSeconds.toFloat()) }
    var allowBreak by remember { mutableStateOf(rule.allowBreak) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = rule.packageName,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "MODE",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(12.dp))
        ModeOption(
            title = "Full block",
            detail = "The app closes the moment it opens.",
            isSelected = mode == BlockMode.FULL_BLOCK,
            onSelect = { mode = BlockMode.FULL_BLOCK },
        )
        if (supportsSurfaces) {
            Spacer(Modifier.height(8.dp))
            if (isChromeIncognitoRule) {
                ModeOption(
                    title = "Incognito block",
                    detail = "Chrome closes immediately the moment an Incognito tab is opened.",
                    isSelected = mode == BlockMode.SURFACE_BLOCK,
                    onSelect = { mode = BlockMode.SURFACE_BLOCK },
                )
            } else if (isYouTubeShortsRule) {
                ModeOption(
                    title = "Shorts block",
                    detail = "Only Shorts is blocked. The rest of YouTube is unaffected.",
                    isSelected = mode == BlockMode.SURFACE_BLOCK,
                    onSelect = { mode = BlockMode.SURFACE_BLOCK },
                )
            } else {
                val surfaceNames = SurfaceMatchers.forPackage(rule.packageName)
                    .joinToString(", ") { it.surfaceLabel }
                ModeOption(
                    title = "Feed block",
                    detail = "Only $surfaceNames is blocked. One opened link is fine, but scrolling to the next one is not.",
                    isSelected = mode == BlockMode.SURFACE_BLOCK,
                    onSelect = { mode = BlockMode.SURFACE_BLOCK },
                )
            }
        }

        if (mode == BlockMode.SURFACE_BLOCK && !isInstantBlockRule) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "GRACE PERIOD · ${graceSeconds.roundToInt()}S",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            val sliderInteractionSource = remember { MutableInteractionSource() }
            Slider(
                value = graceSeconds,
                onValueChange = { graceSeconds = it },
                valueRange = 0f..30f,
                steps = 5,
                modifier = Modifier.padding(horizontal = 6.dp),
                interactionSource = sliderInteractionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = sliderInteractionSource,
                        thumbSize = DpSize(4.dp, 20.dp),
                        colors = SliderDefaults.colors(thumbColor = colors.brass),
                    )
                },
                colors = SliderDefaults.colors(
                    thumbColor = colors.brass,
                    activeTrackColor = colors.brass,
                    inactiveTrackColor = colors.surfaceRaised,
                ),
            )
        }

        val isInstantModeSelected = isInstantBlockRule && mode == BlockMode.SURFACE_BLOCK
        val isNoBreakModeSelected = isChromeIncognitoRule && mode == BlockMode.SURFACE_BLOCK

        if (!isNoBreakModeSelected) {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ALLOW 5-MINUTE BREAK",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Once per day, this app can be temporarily unlocked from the block screen for 5 minutes. When off, it can't be unlocked at all.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = allowBreak,
                    onCheckedChange = { allowBreak = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.brass,
                        checkedTrackColor = colors.surfaceRaised,
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val targetSurfaces = if (mode == BlockMode.SURFACE_BLOCK) {
                    SurfaceMatchers.defaultSurfaceIds(rule.packageName)
                } else {
                    emptyList()
                }
                onSave(
                    rule.copy(
                        mode = mode,
                        graceSeconds = if (isInstantModeSelected) 0 else graceSeconds.roundToInt(),
                        targetSurfaces = targetSurfaces,
                        allowBreak = if (isNoBreakModeSelected) false else allowBreak,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brass,
                contentColor = colors.ink,
            ),
        ) {
            Text(text = "Save rule", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ModeOption(
    title: String,
    detail: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = VaultTheme.colors

    Surface(
        onClick = onSelect,
        shape = VaultTheme.shapes.medium,
        color = if (isSelected) colors.surfaceRaised else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.brass else colors.surfaceRaised,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) colors.brass else colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun AppPicker(
    apps: List<InstalledApp>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = VaultTheme.colors
    var showAllApps by remember { mutableStateOf(false) }

    val popularApps = remember(apps) {
        val order = POPULAR_APP_PACKAGES.withIndex().associate { (index, pkg) -> pkg to index }
        apps.filter { it.packageName in order }.sortedBy { order.getValue(it.packageName) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Text(
                text = "Choose an app",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.brass)
                }
            }

            return@LazyColumn
        }

        if (popularApps.isNotEmpty()) {
            item {
                Text(
                    text = "POPULAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(12.dp))
                popularApps.chunked(5).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { app ->
                            PopularAppTile(
                                app = app,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelect(app.packageName) },
                            )
                        }
                        repeat(5 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            Surface(
                onClick = { showAllApps = !showAllApps },
                shape = VaultTheme.shapes.medium,
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "All apps",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                    )
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (showAllApps) 180f else 0f,
                        label = "chevron",
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (showAllApps) "Hide all apps" else "Show all apps",
                        tint = colors.brass,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (showAllApps) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(app.packageName) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(icon = app.icon, label = app.label, size = 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularAppTile(
    app: InstalledApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = VaultTheme.colors

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(icon = app.icon, label = app.label, size = 48.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AppIcon(
    icon: ImageBitmap?,
    label: String,
    size: Dp,
) {
    val colors = VaultTheme.colors
    val shape = RoundedCornerShape(size / 4)

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(colors.surfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(shape),
            )
        } else {
            Text(
                text = label.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textMuted,
            )
        }
    }
}
