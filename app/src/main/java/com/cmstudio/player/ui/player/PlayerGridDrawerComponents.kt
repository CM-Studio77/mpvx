package com.cmstudio.player.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmstudio.player.ui.AppSettings
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Clickable and swipe-inward `<` mid-right edge handle pill to summon the 3-column drawer.
 */
@Composable
fun PlayerControlEdgeHandle(
    visible: Boolean,
    glassFill: Color,
    glassBorder: Color,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumPx by remember { mutableFloatStateOf(0f) }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            color = glassFill.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, glassBorder),
            shadowElevation = 4.dp,
            modifier = Modifier
                .width(20.dp)
                .height(46.dp)
                .pointerInput(Unit) {
                    var opened = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            opened = false
                            dragAccumPx = 0f
                        },
                        onDragEnd = { dragAccumPx = 0f },
                        onDragCancel = { dragAccumPx = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            if (dragAmount < 0f) {
                                change.consume()
                                dragAccumPx += -dragAmount
                                if (!opened && dragAccumPx > 24f) {
                                    opened = true
                                    onOpenDrawer()
                                }
                            }
                        }
                    )
                }
                .clickable(onClick = onOpenDrawer)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Open More Controls",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class GridDrawerTileItem(
    val label: String,
    val icon: ImageVector,
    val active: Boolean = false,
    val badge: String = "",
    val onClick: () -> Unit
)

/**
 * Translucent dark/frosted 3-column right-side rounded square grid drawer (`More`) with
 * swipe-right dismiss, 24 quick actions, and expandable HUD overlay switches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerControlGridDrawer(
    visible: Boolean,
    accent: Color,
    isMirrored: Boolean,
    isFlippedVertical: Boolean,
    isShuffleEnabled: Boolean,
    customSkipSec: Int,
    sleepTimerRemainingMs: Long,
    activeEngineLabel: String,
    aspectRatioMode: String,
    currentSpeed: Float,
    repeatMode: String,
    zoomScale: Float,
    abLoopActive: Boolean,
    subtitlesDisabled: Boolean,
    ambientEnabled: Boolean,
    bgPlaybackEnabled: Boolean,
    controlsDrawerEnabled: Boolean,
    onDismiss: () -> Unit,
    onOpenEngine: () -> Unit,
    onOpenAspect: () -> Unit,
    onOpenSpeed: () -> Unit,
    onCycleRotation: () -> Unit,
    onOpenFrameNav: () -> Unit,
    onOpenZoomReset: () -> Unit,
    onOpenPip: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenQuality: () -> Unit,
    onToggleMirror: () -> Unit,
    onToggleVerticalFlip: () -> Unit,
    onOpenAbLoop: () -> Unit,
    onCustomSkipForward: () -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onToggleAmbient: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onOpenCast: () -> Unit
) {
    val showRotOv by AppSettings.showScreenRotationOverlay.collectAsState()
    val showAspectOv by AppSettings.showAspectRatioOverlay.collectAsState()
    val showSpeedOv by AppSettings.showPlaybackSpeedOverlay.collectAsState()
    val showDecoderOv by AppSettings.showDecoderOverlay.collectAsState()
    val showShuffleOv by AppSettings.showShuffleOverlay.collectAsState()
    val showLoopOv by AppSettings.showLoopOverlay.collectAsState()
    val showPipOv by AppSettings.showPipOverlay.collectAsState()
    val showShotOv by AppSettings.showScreenshotOverlay.collectAsState()
    val showSeekRipple by AppSettings.showSeekRipple.collectAsState()
    var showOverlayToggles by remember { mutableStateOf(false) }

    val tiles = listOf(
        GridDrawerTileItem("Playback\nSpeed", Icons.Rounded.Speed, abs(currentSpeed - 1f) > 0.02f, String.format(Locale.US, "%.2gx", currentSpeed)) { onDismiss(); onOpenSpeed() },
        GridDrawerTileItem("Decoder", Icons.Rounded.Memory, true, activeEngineLabel) { onDismiss(); onOpenEngine() },
        GridDrawerTileItem("Rotation", Icons.Rounded.ScreenRotation, false) { onCycleRotation() },
        GridDrawerTileItem("Frame\nNavigation", Icons.Rounded.FastForward, false) { onDismiss(); onOpenFrameNav() },
        GridDrawerTileItem("Video Zoom", Icons.Rounded.ZoomIn, abs(zoomScale - 1f) > 0.03f, "${((zoomScale - 1f) * 100f).roundToInt()}%") { onOpenZoomReset() },
        GridDrawerTileItem("Picture in\nPicture", Icons.Rounded.PictureInPictureAlt, false) { onDismiss(); onOpenPip() },
        GridDrawerTileItem("Aspect Ratio", Icons.Rounded.AspectRatio, aspectRatioMode != "Fit", aspectRatioMode) { onDismiss(); onOpenAspect() },
        GridDrawerTileItem("Lock\nControls", Icons.Rounded.Lock, false) { onDismiss(); onToggleLock() },
        GridDrawerTileItem("Audio Track", Icons.Rounded.Audiotrack, false) { onDismiss(); onOpenAudio() },
        GridDrawerTileItem("Subtitles", Icons.Rounded.Subtitles, !subtitlesDisabled) { onDismiss(); onOpenSubtitles() },
        GridDrawerTileItem("Repeat Mode", if (repeatMode == "ONE") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, repeatMode != "OFF", repeatMode) { onCycleRepeat() },
        GridDrawerTileItem("Shuffle", Icons.Rounded.Shuffle, isShuffleEnabled) { onToggleShuffle() },
        GridDrawerTileItem("Chapters", Icons.Rounded.Bookmarks, false) { onDismiss(); onOpenChapters() },
        GridDrawerTileItem("Quality", Icons.Rounded.HighQuality, false) { onDismiss(); onOpenQuality() },
        GridDrawerTileItem("Mirror", Icons.Rounded.Flip, isMirrored) { onToggleMirror() },
        GridDrawerTileItem("Vertical Flip", Icons.Rounded.FlipCameraAndroid, isFlippedVertical) { onToggleVerticalFlip() },
        GridDrawerTileItem("A-B Loop", Icons.Rounded.Loop, abLoopActive) { onDismiss(); onOpenAbLoop() },
        GridDrawerTileItem("Custom\nSkip", Icons.Rounded.Forward10, false, "+${customSkipSec}s") { onCustomSkipForward() },
        GridDrawerTileItem("Background\nPlay", Icons.Rounded.Headphones, bgPlaybackEnabled) { onToggleBackgroundPlay() },
        GridDrawerTileItem("Ambience\nMode", Icons.Rounded.AutoAwesome, ambientEnabled) { onToggleAmbient() },
        GridDrawerTileItem("Equalizer", Icons.Rounded.GraphicEq, false) { onDismiss(); onOpenEqualizer() },
        GridDrawerTileItem("Sleep Timer", Icons.Rounded.Bedtime, sleepTimerRemainingMs > 0L) { onDismiss(); onOpenSleepTimer() },
        GridDrawerTileItem("Screenshot", Icons.Rounded.CameraAlt, false) { onDismiss(); onCaptureScreenshot() },
        GridDrawerTileItem("Cast", Icons.Rounded.Cast, false) { onDismiss(); onOpenCast() }
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp),
                color = Color(0xCC0D1118),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(342.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .pointerInput(Unit) {
                        var dragOut = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragOut = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                if (dragAmount > 0f) {
                                    change.consume()
                                    dragOut += dragAmount
                                    if (dragOut > 72f) onDismiss()
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("More", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val gap = 8.dp
                            val tileW = ((maxWidth - gap * 2f) / 3f).coerceAtLeast(80.dp)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gap),
                                verticalArrangement = Arrangement.spacedBy(gap),
                                maxItemsInEachRow = 3
                            ) {
                                tiles.forEach { tile ->
                                    Surface(
                                        onClick = tile.onClick,
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (tile.active) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(
                                            width = if (tile.active) 1.2.dp else 0.7.dp,
                                            color = if (tile.active) accent else Color.White.copy(alpha = 0.14f)
                                        ),
                                        modifier = Modifier
                                            .width(tileW)
                                            .height(86.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(7.dp)) {
                                            if (tile.badge.isNotBlank()) {
                                                Text(
                                                    text = tile.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tile.active) accent else Color.White.copy(alpha = 0.75f),
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                )
                                            }
                                            Column(
                                                modifier = Modifier.align(Alignment.Center),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = tile.icon,
                                                    contentDescription = tile.label.replace("\n", " "),
                                                    tint = if (tile.active) accent else Color.White,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                                Text(
                                                    text = tile.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    maxLines = 2,
                                                    lineHeight = 13.sp,
                                                    textAlign = TextAlign.Center,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                        Surface(
                            onClick = { showOverlayToggles = !showOverlayToggles },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("HUD Overlay Toggles", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent)
                                Icon(
                                    imageVector = if (showOverlayToggles) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }

                        if (showOverlayToggles) {
                            GridDrawerSwitchRow("Screen Rotation", showRotOv, accent) { AppSettings.setShowScreenRotationOverlay(it) }
                            GridDrawerSwitchRow("Aspect Ratio", showAspectOv, accent) { AppSettings.setShowAspectRatioOverlay(it) }
                            GridDrawerSwitchRow("Playback Speed", showSpeedOv, accent) { AppSettings.setShowPlaybackSpeedOverlay(it) }
                            GridDrawerSwitchRow("Decoder Badge", showDecoderOv, accent) { AppSettings.setShowDecoderOverlay(it) }
                            GridDrawerSwitchRow("Shuffle Button", showShuffleOv, accent) { AppSettings.setShowShuffleOverlay(it) }
                            GridDrawerSwitchRow("Loop Button", showLoopOv, accent) { AppSettings.setShowLoopOverlay(it) }
                            GridDrawerSwitchRow("PiP Button", showPipOv, accent) { AppSettings.setShowPipOverlay(it) }
                            GridDrawerSwitchRow("Screenshot Button", showShotOv, accent) { AppSettings.setShowScreenshotOverlay(it) }
                            GridDrawerSwitchRow("Seek Ripple Arc", showSeekRipple, accent) { AppSettings.setShowSeekRipple(it) }
                            GridDrawerSwitchRow("Edge Drawer Handle", controlsDrawerEnabled, accent) { AppSettings.setControlsDrawerEnabled(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridDrawerSwitchRow(
    label: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent)
        )
    }
}

/**
 * Circular semi-transparent arc ripple displayed on Left/Right double-tap seeks (`-10s` / `+20s`).
 */
@Composable
fun DoubleTapSeekArcOverlay(
    side: String,
    accumulatedSec: Int,
    modifier: Modifier = Modifier
) {
    val isLeft = side == "LEFT"
    val shape = if (isLeft) {
        RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
    } else {
        RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.42f)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isLeft) Icons.Rounded.Replay10 else Icons.Rounded.Forward10,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLeft) "-${accumulatedSec}s" else "+${accumulatedSec}s",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}
