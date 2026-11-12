package com.cmstudio.player.ui.theme

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmstudio.player.ui.AppSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 3-way Dark Mode selector (`System` | `Dark` | `Light`) triggering circular radial reveal.
 */
@Composable
fun DarkModeSegmentedRow(
    currentMode: String,
    onModeSelected: (String, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple("SYSTEM", "System", Icons.Rounded.SettingsBrightness),
        Triple("DARK", "Dark", Icons.Rounded.DarkMode),
        Triple("LIGHT", "Light", Icons.Rounded.LightMode)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label, icon) ->
            val selected = currentMode.equals(key, ignoreCase = true)
            var origin by remember { mutableStateOf(Offset.Zero) }
            Surface(
                shape = AppShapeScale.large,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords -> origin = coords.boundsInWindow().center }
                    .clip(AppShapeScale.large)
                    .pointerInput(key) {
                        detectTapGestures { local ->
                            onModeSelected(key, origin)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Horizontal `90x140dp` mini-UI theme preview carousel.
 * Favorites bubble to the front; tapping any card emits global window coordinates for the reveal animation.
 */
@Composable
fun ThemePreviewCarousel(
    currentPreset: ThemePreset,
    darkMode: String,
    isAmoled: Boolean,
    favoritePresets: Set<String>,
    onToggleFavorite: (ThemePreset) -> Unit,
    onSelectTheme: (ThemePreset, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemDark = isSystemInDarkTheme()
    val previewDark = when (darkMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }
    val orderedPresets = remember(favoritePresets) {
        val favs = ThemePreset.entries.filter { it.name in favoritePresets }
        val rest = ThemePreset.entries.filterNot { it.name in favoritePresets }
        favs + rest
    }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val idx = orderedPresets.indexOf(currentPreset)
        if (idx > 1) listState.animateScrollToItem(idx - 1)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(orderedPresets, key = { it.name }) { preset ->
            MiniThemePreviewCard(
                preset = preset,
                isSelected = preset == currentPreset,
                isFavorite = preset.name in favoritePresets,
                isDark = previewDark,
                isAmoled = isAmoled,
                onToggleFavorite = { onToggleFavorite(preset) },
                onClick = { offset -> onSelectTheme(preset, offset) }
            )
        }
    }
}

@Composable
private fun MiniThemePreviewCard(
    preset: ThemePreset,
    isSelected: Boolean,
    isFavorite: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: (Offset) -> Unit
) {
    val context = LocalContext.current
    val previewScheme = remember(preset, isDark, isAmoled) {
        resolveAppColorScheme(context = context, themePreset = preset, darkTheme = isDark, isAmoled = isAmoled)
    }
    var cardTopLeft by remember { mutableStateOf(Offset.Zero) }
    val selectionColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .width(96.dp)
            .onGloballyPositioned { coords -> cardTopLeft = coords.boundsInWindow().topLeft }
            .pointerInput(preset) {
                detectTapGestures { local ->
                    onClick(cardTopLeft + local)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 140.dp)
                .shadow(
                    elevation = if (isSelected) 8.dp else 2.dp,
                    shape = AppShapeScale.large,
                    ambientColor = if (isSelected) selectionColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.2f),
                    spotColor = if (isSelected) selectionColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.2f)
                )
                .clip(AppShapeScale.large)
                .background(previewScheme.background)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) selectionColor else previewScheme.outlineVariant.copy(alpha = 0.55f),
                    shape = AppShapeScale.large
                )
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top bar preview + favorite star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(AppShapeScale.extraSmall)
                            .background(previewScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) previewScheme.primary else previewScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onToggleFavorite)
                    )
                }

                // Middle expressiveness card: toggle + tertiary dot
                Surface(
                    shape = AppShapeScale.small,
                    color = previewScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 12.dp)
                                .clip(AppShapeScale.full)
                                .background(previewScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(previewScheme.tertiary)
                        )
                    }
                }

                // Content placeholder lines
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(8.dp)
                            .clip(AppShapeScale.extraSmall)
                            .background(previewScheme.surfaceContainer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(8.dp)
                            .clip(AppShapeScale.extraSmall)
                            .background(previewScheme.surfaceContainer)
                    )
                }

                // Bottom pill navigation mock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(AppShapeScale.full)
                        .background(previewScheme.surfaceContainerHigh),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 18.dp, height = 8.dp)
                            .clip(AppShapeScale.full)
                            .background(previewScheme.primaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(previewScheme.secondary)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(previewScheme.onSurfaceVariant.copy(alpha = 0.45f))
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = preset.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) selectionColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Interactive Photo / Video Wallpaper Customizer & Accent Color Swatch Editor.
 */
@Composable
fun CustomThemeWallpaperSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallpaperUri by AppSettings.customThemeWallpaperUri.collectAsState()
    val wallpaperIsVideo by AppSettings.customThemeWallpaperIsVideo.collectAsState()
    val blurDp by AppSettings.customThemeWallpaperBlur.collectAsState()
    val brightness by AppSettings.customThemeWallpaperBrightness.collectAsState()
    val saturation by AppSettings.customThemeWallpaperSaturation.collectAsState()
    val scrimAlpha by AppSettings.customThemeWallpaperScrimAlpha.collectAsState()
    val surfaceOpacity by AppSettings.customThemeSurfaceOpacity.collectAsState()
    val customPrimary by AppSettings.customPrimaryColor.collectAsState()
    val customAccent by AppSettings.customAccentColor.collectAsState()
    val customBg by AppSettings.customBackgroundColor.collectAsState()
    val customSurface by AppSettings.customSurfaceColor.collectAsState()

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val isVid = mime.startsWith("video/")
            val copiedPath = CustomThemeMediaHelper.copyMediaToAppStorage(context, uri, isVid)
            val target = copiedPath ?: uri.toString()
            AppSettings.setCustomThemeWallpaper(target, isVid)
            CustomThemeMediaHelper.samplePrimaryColorArgb(target, isVid)?.let { sampled ->
                AppSettings.setCustomPrimaryColor(sampled)
            }
        }
    }

    val swatches = listOf(
        null to "Preset",
        0xFF70ECE4L to "Cyan",
        0xFF3B82F6L to "Cobalt",
        0xFFA855F7L to "Violet",
        0xFFF43F5EL to "Crimson",
        0xFF10B981L to "Emerald",
        0xFFF59E0BL to "Amber"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Accent Color Override",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(swatches) { (argb, label) ->
                val selected = customPrimary == argb
                val circleColor = if (argb != null) Color(argb) else MaterialTheme.colorScheme.primary
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(circleColor)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable { AppSettings.setCustomPrimaryColor(argb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = label,
                                tint = if (circleColor.toArgb() == Color.Black.toArgb()) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Custom Secondary / Surface Overrides + Live Custom Theme Preview
        val secondarySwatches = listOf(
            null to "Auto",
            0xFF38BDF8L to "Sky",
            0xFFC084FCL to "Orchid",
            0xFF34D399L to "Mint",
            0xFFFB7185L to "Rose",
            0xFFFBBF24L to "Gold"
        )
        Text(
            text = "Secondary Accent & Custom Theme Creator",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(secondarySwatches) { (argb, label) ->
                val selected = customAccent == argb
                val circleColor = if (argb != null) Color(argb) else MaterialTheme.colorScheme.secondary
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(circleColor)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable { AppSettings.setCustomAccentColor(argb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = label, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                null to "Preset Surface",
                0xFF0B0E14L to "Midnight",
                0xFF14111FL to "Plum",
                0xFFF5F3FFL to "Soft Lilac"
            ).forEach { (bgVal, lbl) ->
                val active = customBg == bgVal
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f).clickable {
                        AppSettings.setCustomBackgroundColor(bgVal)
                        AppSettings.setCustomSurfaceColor(bgVal)
                    }
                ) {
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Live Custom Theme Preview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Contrast & surface preview updates instantaneously", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val ctaBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(ctaBrush)
                    .clickable { mediaPicker.launch(arrayOf("image/*", "video/*")) }
                    .padding(vertical = 13.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (wallpaperIsVideo) Icons.Rounded.Wallpaper else Icons.Rounded.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (wallpaperUri.isBlank()) "Add photo or video theme" else "Change photo or video theme",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            if (wallpaperUri.isNotBlank()) {
                IconButton(
                    onClick = {
                        AppSettings.setCustomThemeWallpaper("", false)
                        AppSettings.setCustomPrimaryColor(null)
                    }
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Remove wallpaper", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (wallpaperUri.isNotBlank()) {
            WallpaperSliderRow("Wallpaper Blur", "${blurDp.roundToInt()} dp", blurDp, 0f..32f) {
                AppSettings.setCustomThemeWallpaperBlur(it)
            }
            WallpaperSliderRow("Brightness", "${(brightness * 100).roundToInt()}%", brightness, 0.35f..1.45f) {
                AppSettings.setCustomThemeWallpaperBrightness(it)
            }
            WallpaperSliderRow("Saturation", "${(saturation * 100).roundToInt()}%", saturation, 0f..1.8f) {
                AppSettings.setCustomThemeWallpaperSaturation(it)
            }
            WallpaperSliderRow("Scrim Darkness", "${(scrimAlpha * 100).roundToInt()}%", scrimAlpha, 0f..0.85f) {
                AppSettings.setCustomThemeWallpaperScrimAlpha(it)
            }
            WallpaperSliderRow("Panel Opacity", "${(surfaceOpacity * 100).roundToInt()}%", surfaceOpacity, 0.35f..1.0f) {
                AppSettings.setCustomThemeSurfaceOpacity(it)
            }
        }
    }
}

@Composable
private fun WallpaperSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
