package com.cmstudio.player.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.ui.platform.LocalContext
import com.cmstudio.player.ui.player.SubtitleFontDirectorySync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.player.ChannelMixMath
import com.cmstudio.player.ui.player.EqualizerPresetsCatalog
import com.cmstudio.player.ui.player.MpvxSeekbarCanvas
import com.cmstudio.player.ui.player.PlayerControlCatalog
import com.cmstudio.player.ui.player.VideoChapterItem
import com.cmstudio.player.ui.theme.extendedColors
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Live animated seekbar preview selector rendering all 6 styles via the real shared [MpvxSeekbarCanvas].
 */
@Composable
fun SeekbarStyleLivePreviewCard() {
    val activeStyle by AppSettings.seekbarStyle.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    val wavePhase by rememberInfiniteTransition(label = "settingsSeekPreview").animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "settingsWavePhase"
    )
    val sampleChapters = remember {
        listOf(
            VideoChapterItem("Intro", 15_000L, 45_000L),
            VideoChapterItem("Scene 2", 62_000L, 100_000L)
        )
    }

    SettingsCard {
        Text("Seekbar Style (Live Preview)", fontWeight = FontWeight.Bold)
        Text(
            "Tap any preview to customize the player progress bar look.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayerControlCatalog.SEEKBAR_STYLES.forEach { (id, label) ->
                val selected = activeStyle.equals(id, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0E131B),
                    border = BorderStroke(
                        width = if (selected) 1.6.dp else 1.dp,
                        color = if (selected) accent else Color.White.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { AppSettings.setSeekbarStyle(id) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (selected) accent else Color.White.copy(alpha = 0.45f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) accent else Color.White
                                )
                            }
                            if (selected) {
                                Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
                            }
                        }
                        MpvxSeekbarCanvas(
                            seekbarStyle = id,
                            progressFraction = 0.56f,
                            bufferedFraction = 0.78f,
                            durationMs = 100_000L,
                            accentColor = accent,
                            wavePhase = wavePhase,
                            chapters = sampleChapters,
                            abLoopStartMs = 20_000L,
                            abLoopEndMs = 45_000L,
                            abLoopActive = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerHudAppearanceCard() {
    val hudTheme by AppSettings.playerControlsTheme.collectAsState()
    val hideBtnBg by AppSettings.hidePlayerButtonsBg.collectAsState()
    val forceDarkBg by AppSettings.forceDarkPlayerButtonsBg.collectAsState()
    val showSeekOuter by AppSettings.showSeekbarOuterContainer.collectAsState()
    val portraitPos by AppSettings.portraitControlsPosition.collectAsState()
    val clockFmt by AppSettings.playerClockFormat.collectAsState()
    val autoHideMs by AppSettings.hudAutoHideMs.collectAsState()
    val ctrlSize by AppSettings.playerControlSize.collectAsState()
    val customSkipSec by AppSettings.customSkipDurationSec.collectAsState()
    val showStatusBar by AppSettings.showSystemStatusBar.collectAsState()
    val showNavBar by AppSettings.showSystemNavBar.collectAsState()
    val safeArea by AppSettings.safeAreaWindow.collectAsState()
    val showLoading by AppSettings.showLoadingCircle.collectAsState()
    val telemetry by AppSettings.showHudTelemetry.collectAsState()

    SettingsCard {
        Text("HUD Visual Style", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CLASSIC" to "Classic", "GLASS" to "Glass", "GLOSSY" to "Glossy").forEach { (id, label) ->
                FilterChip(selected = hudTheme == id, onClick = { AppSettings.setPlayerControlsTheme(id) }, label = { Text(label) })
            }
        }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)

        Text("Button Size", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("COMPACT" to "Compact", "NORMAL" to "Balanced", "LARGE" to "Large").forEach { (id, label) ->
                FilterChip(selected = ctrlSize == id, onClick = { AppSettings.setPlayerControlSize(id) }, label = { Text(label) })
            }
        }

        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        SettingsSwitchRow("Hide Button Backgrounds", "Transparent floating icons cleanly over video", hideBtnBg) {
            AppSettings.setHidePlayerButtonsBg(it)
        }
        SettingsSwitchRow("Force Dark Button Capsules", "High-contrast dark pill fill regardless of light theme", forceDarkBg) {
            AppSettings.setForceDarkPlayerButtonsBg(it)
        }
        SettingsSwitchRow("Seekbar Outer Capsule", "Draw frosted pill container around seekbar & timestamps", showSeekOuter) {
            AppSettings.setShowSeekbarOuterContainer(it)
        }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)

        Text("Portrait Controls Placement", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CENTER" to "Center Screen", "BELOW_SEEKBAR" to "Below Seekbar").forEach { (id, label) ->
                FilterChip(selected = portraitPos == id, onClick = { AppSettings.setPortraitControlsPosition(id) }, label = { Text(label) })
            }
        }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)

        Text("HUD Clock Format", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SYSTEM" to "System", "12H" to "12-Hour", "24H" to "24-Hour").forEach { (id, label) ->
                FilterChip(selected = clockFmt == id, onClick = { AppSettings.setPlayerClockFormat(id) }, label = { Text(label) })
            }
        }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("HUD Auto-Hide Timeout", fontWeight = FontWeight.SemiBold)
            Text(String.format(Locale.US, "%.1fs", autoHideMs / 1000f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = autoHideMs.toFloat().coerceIn(1500f, 15000f),
            onValueChange = { AppSettings.setHudAutoHideMs(((it / 500f).roundToInt() * 500).coerceIn(1500, 15000)) },
            valueRange = 1500f..15000f
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Custom Skip Step (Intro/ED)", fontWeight = FontWeight.SemiBold)
            Text("+${customSkipSec}s", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = customSkipSec.toFloat().coerceIn(5f, 300f),
            onValueChange = { AppSettings.setCustomSkipDurationSec(((it / 5f).roundToInt() * 5).coerceIn(5, 300)) },
            valueRange = 5f..300f
        )

        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        SettingsSwitchRow("Runtime Telemetry Strip", "Show codec, resolution, HDR, FPS & dropped frames", telemetry) {
            AppSettings.setShowHudTelemetry(it)
        }
        SettingsSwitchRow("Show System Status Bar", "Keep top time/battery bar visible in player", showStatusBar) {
            AppSettings.setShowSystemStatusBar(it)
        }
        SettingsSwitchRow("Show Navigation Bar", "Keep bottom Android navigation bar visible in player", showNavBar) {
            AppSettings.setShowSystemNavBar(it)
        }
        SettingsSwitchRow("Extra Safe-Area Margins", "Inset HUD controls inside notched & curved edges", safeArea) {
            AppSettings.setSafeAreaWindow(it)
        }
        SettingsSwitchRow("Show Buffering Indicator", "Display spinner overlay when network buffering", showLoading) {
            AppSettings.setShowLoadingCircle(it)
        }
    }
}

/**
 * Visual Landscape & Portrait Control Layout summary cards for MPVX Player.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerControlsVisualSummaryCard(
    activeSlot: String,
    onSelectSlotToEdit: (String) -> Unit
) {
    val topRightCsv by AppSettings.topRightHudControls.collectAsState()
    val bottomLeftCsv by AppSettings.bottomLeftHudControls.collectAsState()
    val bottomRightCsv by AppSettings.bottomRightHudControls.collectAsState()
    val portraitCsv by AppSettings.portraitBottomHudControls.collectAsState()

    val landscapeSlots = listOf(
        Triple(PlayerControlCatalog.SLOT_TOP_RIGHT, "Top Right", topRightCsv),
        Triple(PlayerControlCatalog.SLOT_BOTTOM_LEFT, "Bottom Left", bottomLeftCsv),
        Triple(PlayerControlCatalog.SLOT_BOTTOM_RIGHT, "Bottom Right", bottomRightCsv)
    )

    SettingsCard {
        Text("Landscape Controls", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            landscapeSlots.forEach { (slotId, title, csv) ->
                val isSelected = activeSlot == slotId
                val tokens = PlayerControlCatalog.parseSlotCsv(csv)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isSelected) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectSlotToEdit(slotId) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                tokens.forEach { tok ->
                                    val lbl = PlayerControlCatalog.ALL_CONTROLS.find { it.id == tok }?.shortLabel ?: tok
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest
                                    ) {
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { onSelectSlotToEdit(slotId) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit $title", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    SettingsCard {
        val pSelected = activeSlot == PlayerControlCatalog.SLOT_PORTRAIT_BOTTOM
        val pTokens = PlayerControlCatalog.parseSlotCsv(portraitCsv)
        Text("Portrait Controls", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (pSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (pSelected) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelectSlotToEdit(PlayerControlCatalog.SLOT_PORTRAIT_BOTTOM) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Portrait Bottom Strip", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        pTokens.forEach { tok ->
                            val lbl = PlayerControlCatalog.ALL_CONTROLS.find { it.id == tok }?.shortLabel ?: tok
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest
                            ) {
                                Text(lbl, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
                IconButton(onClick = { onSelectSlotToEdit(PlayerControlCatalog.SLOT_PORTRAIT_BOTTOM) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Portrait Controls", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Interactive 4-Slot Reorderable Control Layout Editor backed by `sh.calvin.reorderable`,
 * enforcing landscape cross-slot mutual exclusion, reset confirmation, and icons legend.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlLayoutEditorCard(
    activeSlot: String = PlayerControlCatalog.SLOT_TOP_RIGHT,
    onActiveSlotChange: ((String) -> Unit)? = null
) {
    val topRightCsv by AppSettings.topRightHudControls.collectAsState()
    val bottomLeftCsv by AppSettings.bottomLeftHudControls.collectAsState()
    val bottomRightCsv by AppSettings.bottomRightHudControls.collectAsState()
    val portraitCsv by AppSettings.portraitBottomHudControls.collectAsState()

    var internalSlot by remember(activeSlot) { mutableStateOf(activeSlot) }
    val resolvedSlot = if (onActiveSlotChange != null) activeSlot else internalSlot
    val selectSlot: (String) -> Unit = { s ->
        internalSlot = s
        onActiveSlotChange?.invoke(s)
    }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }

    val slotMeta = listOf(
        PlayerControlCatalog.SLOT_TOP_RIGHT to "Top Right",
        PlayerControlCatalog.SLOT_BOTTOM_LEFT to "Bottom Left",
        PlayerControlCatalog.SLOT_BOTTOM_RIGHT to "Bottom Right",
        PlayerControlCatalog.SLOT_PORTRAIT_BOTTOM to "Portrait"
    )
    val currentCsv = when (resolvedSlot) {
        PlayerControlCatalog.SLOT_TOP_RIGHT -> topRightCsv
        PlayerControlCatalog.SLOT_BOTTOM_LEFT -> bottomLeftCsv
        PlayerControlCatalog.SLOT_BOTTOM_RIGHT -> bottomRightCsv
        else -> portraitCsv
    }
    val activeTokens = remember(currentCsv) { PlayerControlCatalog.parseSlotCsv(currentCsv) }

    // Map buttons assigned in OTHER landscape slots to enforce cross-slot exclusivity
    val usedInOtherSlotMap = remember(resolvedSlot, topRightCsv, bottomLeftCsv, bottomRightCsv) {
        if (resolvedSlot == PlayerControlCatalog.SLOT_PORTRAIT_BOTTOM) {
            emptyMap()
        } else {
            val map = linkedMapOf<String, String>()
            val others = listOf(
                PlayerControlCatalog.SLOT_TOP_RIGHT to topRightCsv,
                PlayerControlCatalog.SLOT_BOTTOM_LEFT to bottomLeftCsv,
                PlayerControlCatalog.SLOT_BOTTOM_RIGHT to bottomRightCsv
            ).filter { it.first != resolvedSlot }
            others.forEach { (slotId, csv) ->
                val slotLabel = slotMeta.firstOrNull { it.first == slotId }?.second ?: slotId
                PlayerControlCatalog.parseSlotCsv(csv).forEach { tok -> map[tok] = slotLabel }
            }
            map
        }
    }

    val gridState = rememberLazyGridState()
    val reorderableGridState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromToken = from.key as? String ?: return@rememberReorderableLazyGridState
        val toToken = to.key as? String ?: return@rememberReorderableLazyGridState
        val list = activeTokens.toMutableList()
        val fromIdx = list.indexOf(fromToken)
        val toIdx = list.indexOf(toToken)
        if (fromIdx in list.indices && toIdx in list.indices && fromIdx != toIdx) {
            val moved = list.removeAt(fromIdx)
            list.add(toIdx, moved)
            AppSettings.assignHudSlotControls(resolvedSlot, list, enforceLandscapeExclusivity = true)
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Control Layout?", fontWeight = FontWeight.Bold) },
            text = { Text("Restore default Top Right, Bottom Left, Bottom Right, and Portrait button slots?") },
            confirmButton = {
                Button(onClick = {
                    AppSettings.resetHudLayoutToDefault()
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Drag & Drop Control Layout", fontWeight = FontWeight.Bold)
                Text(
                    text = "Drag handle or use arrows; adding from another landscape slot moves it cleanly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { showResetConfirm = true }) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            slotMeta.forEach { (id, label) ->
                FilterChip(selected = resolvedSlot == id, onClick = { selectSlot(id) }, label = { Text(label) })
            }
        }

        Text(
            "Active Order (${activeTokens.size}) • Drag to Reorder",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height((((activeTokens.size + 1) / 2).coerceAtLeast(1) * 54).coerceIn(72, 240).dp)
        ) {
            items(count = activeTokens.size, key = { idx -> activeTokens[idx] }) { idx ->
                val token = activeTokens[idx]
                val spec = PlayerControlCatalog.ALL_CONTROLS.find { it.id == token }
                ReorderableItem(reorderableGridState, key = token) { isDragging ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = if (isDragging) 6.dp else 0.dp,
                        border = if (isDragging) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragIndicator,
                                contentDescription = "Drag",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .draggableHandle()
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = spec?.shortLabel ?: token,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { AppSettings.toggleHudControlToken(resolvedSlot, token) },
                                enabled = activeTokens.size > 1,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Available Buttons", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { showLegend = !showLegend }) {
                Text(if (showLegend) "Hide Legend" else "Icons Legend")
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PlayerControlCatalog.ALL_CONTROLS
                .filter { resolvedSlot in it.slots && it.id !in activeTokens }
                .forEach { spec ->
                    val otherSlotName = usedInOtherSlotMap[spec.id]
                    OutlinedButton(
                        onClick = {
                            AppSettings.assignHudSlotControls(
                                slot = resolvedSlot,
                                tokens = activeTokens + spec.id,
                                enforceLandscapeExclusivity = true
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (otherSlotName != null) "${spec.shortLabel} (in $otherSlotName)" else spec.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (otherSlotName != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
        }

        if (showLegend) {
            HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Control Token Legend", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                PlayerControlCatalog.ALL_CONTROLS.forEach { spec ->
                    Text(
                        text = "• ${spec.label} (${spec.id}) — Slots: ${spec.slots.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverlayAndScreenshotSettingsCards() {
    val showRotation by AppSettings.showScreenRotationOverlay.collectAsState()
    val showAspect by AppSettings.showAspectRatioOverlay.collectAsState()
    val showSpeed by AppSettings.showPlaybackSpeedOverlay.collectAsState()
    val showDecoder by AppSettings.showDecoderOverlay.collectAsState()
    val showShuffle by AppSettings.showShuffleOverlay.collectAsState()
    val showLoop by AppSettings.showLoopOverlay.collectAsState()
    val showPip by AppSettings.showPipOverlay.collectAsState()
    val showShot by AppSettings.showScreenshotOverlay.collectAsState()
    val controlsDrawerEnabled by AppSettings.controlsDrawerEnabled.collectAsState()
    val showSeekRipple by AppSettings.showSeekRipple.collectAsState()
    val showSeekTimeInHud by AppSettings.showSeekTimeInHud.collectAsState()
    val showBufferedRange by AppSettings.showBufferedRangeOnSeekbar.collectAsState()
    val showChapters by AppSettings.showChaptersOnSeekbar.collectAsState()
    val reducePlayerAnim by AppSettings.reducePlayerAnimation.collectAsState()
    val showHoldSpeedOv by AppSettings.showHoldSpeedOverlay.collectAsState()
    val showAspectOv by AppSettings.showAspectFeedbackOverlay.collectAsState()
    val showZoomOv by AppSettings.showZoomFeedbackOverlay.collectAsState()
    val showRepeatShuffleOv by AppSettings.showRepeatShuffleOverlay.collectAsState()
    val showActionPills by AppSettings.showActionFeedbackPills.collectAsState()

    val shotFormat by AppSettings.screenshotFormat.collectAsState()
    val shotQuality by AppSettings.screenshotQuality.collectAsState()
    val shotFolder by AppSettings.screenshotFolder.collectAsState()
    val includeSubsInShot by AppSettings.includeSubtitlesInScreenshot.collectAsState()
    val pngCompression by AppSettings.pngCompressionLevel.collectAsState()
    val shotTemplate by AppSettings.screenshotFilenameTemplate.collectAsState()

    SettingsCard {
        Text("Player Overlay Control Toggles", fontWeight = FontWeight.Bold)
        Text("Enable or hide quick overlay triggers & seekbar elements", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsSwitchRow("Edge Controls Drawer Handle", "Show clickable & swipeable < drawer handle on right side", controlsDrawerEnabled) { AppSettings.setControlsDrawerEnabled(it) }
        SettingsSwitchRow("Double-Tap Seek Ripple Arc", "Animate circular arc ripple & counter when seeking", showSeekRipple) { AppSettings.setShowSeekRipple(it) }
        SettingsSwitchRow("Show Seek Timestamps", "Display elapsed & remaining timestamps flanking seekbar", showSeekTimeInHud) { AppSettings.setShowSeekTimeInHud(it) }
        SettingsSwitchRow("Buffered Range on Seekbar", "Render cached/buffered progress segment on seekbar", showBufferedRange) { AppSettings.setShowBufferedRangeOnSeekbar(it) }
        SettingsSwitchRow("Chapter Marks on Seekbar", "Draw chapter notches along timeline", showChapters) { AppSettings.setShowChaptersOnSeekbar(it) }
        SettingsSwitchRow("Reduce Player Animations", "Minimize HUD and ripple motion during playback", reducePlayerAnim) { AppSettings.setReducePlayerAnimation(it) }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        SettingsSwitchRow("Screen Rotation Overlay", "Show quick orientation cycle chip", showRotation) { AppSettings.setShowScreenRotationOverlay(it) }
        SettingsSwitchRow("Aspect Ratio Overlay", "Show Fit/Crop/16:9 ratio trigger", showAspect) { AppSettings.setShowAspectRatioOverlay(it) }
        SettingsSwitchRow("Playback Speed Overlay", "Show interactive speed multiplier pill", showSpeed) { AppSettings.setShowPlaybackSpeedOverlay(it) }
        SettingsSwitchRow("Decoder & Engine Badge", "Show active MPV/Exo HW badge trigger", showDecoder) { AppSettings.setShowDecoderOverlay(it) }
        SettingsSwitchRow("Shuffle Toggle Overlay", "Show queue shuffle button on player HUD", showShuffle) { AppSettings.setShowShuffleOverlay(it) }
        SettingsSwitchRow("Repeat / Loop Overlay", "Show Repeat One/All/A-B toggle button", showLoop) { AppSettings.setShowLoopOverlay(it) }
        SettingsSwitchRow("Picture-in-Picture Button", "Show floating PiP trigger on HUD", showPip) { AppSettings.setShowPipOverlay(it) }
        SettingsSwitchRow("Screenshot Camera Trigger", "Show instant frame capture button", showShot) { AppSettings.setShowScreenshotOverlay(it) }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        SettingsSwitchRow("Hold Speed Feedback Pill", "Display speed pill during long-press fast forward", showHoldSpeedOv) { AppSettings.setShowHoldSpeedOverlay(it) }
        SettingsSwitchRow("Aspect Ratio Feedback Pill", "Display banner feedback when cycling aspect ratios", showAspectOv) { AppSettings.setShowAspectFeedbackOverlay(it) }
        SettingsSwitchRow("Zoom Level Feedback Pill", "Display live zoom percentage pill & reset", showZoomOv) { AppSettings.setShowZoomFeedbackOverlay(it) }
        SettingsSwitchRow("Repeat & Shuffle Feedback", "Display banner when toggling repeat or shuffle", showRepeatShuffleOv) { AppSettings.setShowRepeatShuffleOverlay(it) }
        SettingsSwitchRow("Action Feedback Pills", "Show subtle top toast pills (Ambience Mode ON/OFF, etc.)", showActionPills) { AppSettings.setShowActionFeedbackPills(it) }
    }

    SettingsCard {
        Text("Screenshot Capture Settings", fontWeight = FontWeight.Bold)
        SettingsSwitchRow("Include Subtitles in Screenshots", "Burn active captions into captured video frames cleanly", includeSubsInShot) {
            AppSettings.setIncludeSubtitlesInScreenshot(it)
        }
        Text("Image Format", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("PNG" to "PNG (Lossless)", "JPEG" to "JPEG (Compact)", "WEBP" to "WebP (Modern)").forEach { (fmt, lbl) ->
                FilterChip(selected = shotFormat == fmt, onClick = { AppSettings.setScreenshotFormat(fmt) }, label = { Text(lbl) })
            }
        }
        if (shotFormat == "PNG") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PNG Compression Effort", fontWeight = FontWeight.Medium)
                Text("Level $pngCompression", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = pngCompression.toFloat().coerceIn(0f, 9f),
                onValueChange = { AppSettings.setPngCompressionLevel(it.roundToInt()) },
                valueRange = 0f..9f
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Compression Quality", fontWeight = FontWeight.Medium)
                Text("$shotQuality%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = shotQuality.toFloat().coerceIn(60f, 100f),
                onValueChange = { AppSettings.setScreenshotQuality(it.roundToInt()) },
                valueRange = 60f..100f
            )
        }
        OutlinedTextField(
            value = shotFolder,
            onValueChange = { AppSettings.setScreenshotFolder(it) },
            label = { Text("Pictures Subfolder Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = shotTemplate,
            onValueChange = { AppSettings.setScreenshotFilenameTemplate(it) },
            label = { Text("Filename Template ({title}, {time}, {date})") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedAudioSettingsExtrasCard() {
    val eqPreset by AppSettings.eqPreset.collectAsState()
    val customBandsCsv by AppSettings.eqCustomBandsDb.collectAsState()
    val channelMode by AppSettings.audioChannelMode.collectAsState()
    val volumeNorm by AppSettings.volumeNormalization.collectAsState()
    val volumeCap by AppSettings.volumeBoostCapPct.collectAsState()
    val currentBands = remember(eqPreset, customBandsCsv) {
        EqualizerPresetsCatalog.bandsForPreset(eqPreset, customBandsCsv)
    }

    SettingsCard {
        Text("Volume Leveling & Cap (Both Engines)", fontWeight = FontWeight.Bold)
        SettingsSwitchRow(
            "Volume Normalization",
            "Automatically level quiet dialogue & loud action peaks across MPV and ExoPlayer",
            volumeNorm
        ) { AppSettings.setVolumeNormalization(it) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Maximum Volume Boost Cap", fontWeight = FontWeight.Medium)
            Text("$volumeCap%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 125, 150, 175, 200).forEach { cap ->
                FilterChip(selected = volumeCap == cap, onClick = { AppSettings.setVolumeBoostCapPct(cap) }, label = { Text("${cap}%") })
            }
        }
    }

    SettingsCard {
        Text("11-Preset & 5-Band Equalizer", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EqualizerPresetsCatalog.PRESETS.keys.forEach { preset ->
                FilterChip(selected = eqPreset == preset, onClick = { AppSettings.setEqualizerPreset(preset) }, label = { Text(preset) })
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        EqualizerPresetsCatalog.BAND_LABELS.forEachIndexed { idx, freq ->
            val db = currentBands.getOrElse(idx) { 0f }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(freq, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(44.dp))
                Slider(
                    value = db,
                    onValueChange = { v ->
                        val next = currentBands.toMutableList()
                        next[idx] = ((v * 2f).roundToInt() / 2f).coerceIn(-12f, 12f)
                        AppSettings.setEqCustomBandsDb(EqualizerPresetsCatalog.serializeBands(next))
                        AppSettings.setEqualizerPreset("Custom")
                    },
                    valueRange = -12f..12f,
                    modifier = Modifier.weight(1f)
                )
                Text("${if (db > 0f) "+" else ""}${db.roundToInt()}dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(42.dp))
            }
        }

        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        Text("Audio Channel Output Mode", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ChannelMixMath.MODE_AUTO to "Auto",
                ChannelMixMath.MODE_MONO to "Mono",
                ChannelMixMath.MODE_STEREO to "Stereo",
                ChannelMixMath.MODE_REVERSE_STEREO to "Reverse Stereo"
            ).forEach { (id, label) ->
                FilterChip(selected = channelMode == id, onClick = { AppSettings.setAudioChannelMode(id) }, label = { Text(label) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSubtitleSettingsExtrasCard() {
    val context = LocalContext.current
    val speedMilli by AppSettings.subtitleSpeedMilli.collectAsState()
    val dualSubs by AppSettings.dualSubtitles.collectAsState()
    val italic by AppSettings.subtitleItalic.collectAsState()
    val alignment by AppSettings.subtitleAlignment.collectAsState()
    val fontName by AppSettings.subtitleCustomFontName.collectAsState()
    val overrideAssMode by AppSettings.subOverrideAssMode.collectAsState()
    val scaleByWindow by AppSettings.subScaleByWindow.collectAsState()
    val fontsDirLabel by AppSettings.subFontsDirLabel.collectAsState()
    val fontsDirUri by AppSettings.subFontsDirUri.collectAsState()
    val holdDragMovesSubs by AppSettings.holdDragMovesSubtitles.collectAsState()
    val swipeSeekDialog by AppSettings.swipeSubtitlesToSeekDialog.collectAsState()
    val invertSwipeDialog by AppSettings.invertSwipeSubtitlesDirection.collectAsState()

    val fontsDirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            SubtitleFontDirectorySync.syncFromTreeUri(context, uri)
        }
    }

    SettingsCard {
        Text("ASS/SSA & Dual-Engine Caption Behavior", fontWeight = FontWeight.Bold)
        Text("Override ASS/SSA Subtitle Styles", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("SCALE" to "Scale", "NO" to "Original", "YES" to "Override", "FORCE" to "Force", "STRIP" to "Strip Tags").forEach { (id, lbl) ->
                FilterChip(selected = overrideAssMode == id, onClick = { AppSettings.setSubOverrideAssMode(id) }, label = { Text(lbl) })
            }
        }
        SettingsSwitchRow("Scale Subtitles by Window Size", "Keep caption size proportional across orientations", scaleByWindow) {
            AppSettings.setSubScaleByWindow(it)
        }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Custom Fonts Folder", fontWeight = FontWeight.SemiBold)
                Text(fontsDirLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { fontsDirPicker.launch(null) }) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pick")
                }
                if (fontsDirUri.isNotBlank()) {
                    TextButton(onClick = { SubtitleFontDirectorySync.clearFontsDirectory(context) }) {
                        Text("Clear")
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        SettingsSwitchRow("Drag Bottom Area to Move Subtitles", "Swipe vertically along bottom edge in player to reposition subs", holdDragMovesSubs) {
            AppSettings.setHoldDragMovesSubtitles(it)
        }
        SettingsSwitchRow("Swipe Subtitles to Seek Dialogue", "Swipe horizontally along subtitle strip to jump dialogue", swipeSeekDialog) {
            AppSettings.setSwipeSubtitlesToSeekDialog(it)
        }
        SettingsSwitchRow("Invert Dialogue Swipe Direction", "Reverse forward/backward direction of subtitle swipe seek", invertSwipeDialog) {
            AppSettings.setInvertSwipeSubtitlesDirection(it)
        }
    }

    SettingsCard {
        Text("Advanced Caption Timing & Dual Subtitles", fontWeight = FontWeight.Bold)
        SettingsSwitchRow("Enable Dual Subtitles Overlay", "Display a secondary .srt/.vtt/.ass file cleanly above primary captions", dualSubs) {
            AppSettings.setDualSubtitles(it)
        }
        SettingsSwitchRow("Italic Subtitle Style", "Render captions in italic emphasis", italic) {
            AppSettings.setSubtitleItalic(it)
        }
        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Default Subtitle Speed", fontWeight = FontWeight.SemiBold)
            Text(String.format(Locale.US, "%.2fx", speedMilli / 1000f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = speedMilli.toFloat().coerceIn(500f, 2000f),
            onValueChange = { AppSettings.setSubtitleSpeedMilli(((it / 50f).roundToInt() * 50).coerceIn(100, 10000)) },
            valueRange = 500f..2000f
        )
        Text("Text Alignment", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LEFT" to "Left", "CENTER" to "Center", "RIGHT" to "Right").forEach { (id, label) ->
                FilterChip(selected = alignment == id, onClick = { AppSettings.setSubtitleAlignment(id) }, label = { Text(label) })
            }
        }
        Text("Active Font: $fontName (load custom .ttf inside Video Player Subtitle sheet)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
