package com.cmstudio.player.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cmstudio.player.ui.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        badgeBlueBg = SoftSkyPillBg,
        badgeBlueText = SoftSkyPillText,
        streakOrange = SoftAmberPillText,
        streakOrangeBg = SoftAmberPillBg,
        badgeGreyBg = BadgeGreyBgLight,
        badgeGreyText = BadgeGreyTextLight,
        cardBackground = BentoCardWhite,
        cardBorder = Color.Transparent,
        navIndicator = NavIndicatorLight,
        frostedSheet = FrostedGlassSheet,
        liquidGlassSurface = LiquidGlassSurface,
        liquidGlassBorder = LiquidGlassBorder,
        softSkyBg = SoftSkyPillBg,
        softSkyText = SoftSkyPillText,
        softAmberBg = SoftAmberPillBg,
        softAmberText = SoftAmberPillText
    )
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

/**
 * Coordinates the 650ms circular radial feathered reveal animation when switching themes or dark mode.
 */
@Stable
class ThemeTransitionState {
    var snapshotLayer: GraphicsLayer? = null
        internal set

    var isAnimating by mutableStateOf(false)
        internal set

    var origin by mutableStateOf(Offset.Zero)
        internal set

    val progress = Animatable(0f)

    internal var captureRequested by mutableStateOf(false)
    internal var onApplyPendingChange: (() -> Unit)? = null

    suspend fun animateThemeChange(
        tapOrigin: Offset,
        reduceMotion: Boolean,
        applyChange: () -> Unit
    ) {
        if (reduceMotion || snapshotLayer == null || isAnimating) {
            applyChange()
            return
        }
        origin = tapOrigin
        onApplyPendingChange = applyChange
        captureRequested = true
        // Wait briefly so graphicsLayer records current frame before applying new palette
        delay(32L)
        applyChange()
        onApplyPendingChange = null
        isAnimating = true
        progress.snapTo(0f)
        delay(80L)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        isAnimating = false
        captureRequested = false
    }
}

val LocalThemeTransitionState = compositionLocalOf<ThemeTransitionState?> { null }

@Composable
fun rememberThemeTransitionState(): ThemeTransitionState = remember { ThemeTransitionState() }

private fun Color.contrastRatio(other: Color): Float {
    val l1 = luminance()
    val l2 = other.luminance()
    return (max(l1, l2) + 0.05f) / (min(l1, l2) + 0.05f)
}

private fun Color.accessibleContentColor(): Color =
    if (contrastRatio(Color.Black) >= contrastRatio(Color.White)) Color.Black else Color.White

private fun Color.blendToward(target: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = red + (target.red - red) * f,
        green = green + (target.green - green) * f,
        blue = blue + (target.blue - blue) * f,
        alpha = alpha
    )
}

private fun Color.darkenBy(fraction: Float): Color = blendToward(Color.Black, fraction)
private fun Color.lightenBy(fraction: Float): Color = blendToward(Color.White, fraction)

private fun Color.withMinimumContrast(surfaces: List<Color>, minRatio: Float = 4.5f): Color {
    if (surfaces.minOf { contrastRatio(it) } >= minRatio) return this
    val blackMin = surfaces.minOf { Color.Black.contrastRatio(it) }
    val whiteMin = surfaces.minOf { Color.White.contrastRatio(it) }
    val target = if (blackMin >= whiteMin) Color.Black else Color.White
    var lo = 0f
    var hi = 1f
    repeat(12) {
        val mid = (lo + hi) * 0.5f
        val candidate = blendToward(target, mid)
        if (surfaces.minOf { candidate.contrastRatio(it) } >= minRatio) {
            hi = mid
        } else {
            lo = mid
        }
    }
    return blendToward(target, hi)
}

fun resolveAppColorScheme(
    context: Context,
    themePreset: ThemePreset,
    darkTheme: Boolean,
    isAmoled: Boolean = false,
    dynamicColor: Boolean = false,
    customPrimaryArgb: Long? = null,
    customAccentArgb: Long? = null,
    customBackgroundArgb: Long? = null,
    customSurfaceArgb: Long? = null
): ColorScheme {
    val useDynamic = (dynamicColor || themePreset.isDynamic) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        customPrimaryArgb == null

    if (useDynamic) {
        val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return if (darkTheme && isAmoled) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF070709),
                surfaceContainer = Color(0xFF0E0F12),
                surfaceContainerHigh = Color(0xFF16181D),
                surfaceContainerHighest = Color(0xFF1E2128)
            )
        } else {
            base
        }
    }

    if (darkTheme) {
        val rawPrimary = customPrimaryArgb?.let(::Color) ?: themePreset.primaryDark
        val rawSecondary = customAccentArgb?.let(::Color) ?: themePreset.secondaryDark
        val rawTertiary = themePreset.tertiaryDark
        val baseBg = when {
            isAmoled -> Color.Black
            customBackgroundArgb != null -> Color(customBackgroundArgb)
            else -> themePreset.backgroundDark
        }
        val surfaceContainerLowest = if (isAmoled) Color.Black else baseBg.darkenBy(0.22f)
        val surfaceContainerLow = if (isAmoled) Color(0xFF070709) else rawPrimary.copy(alpha = 0.05f).compositeOver(baseBg)
        val surfaceContainer = when {
            isAmoled -> Color(0xFF0E0F12)
            customSurfaceArgb != null -> Color(customSurfaceArgb)
            else -> rawPrimary.copy(alpha = 0.09f).compositeOver(baseBg)
        }
        val surfaceContainerHigh = if (isAmoled) Color(0xFF16181D) else rawPrimary.copy(alpha = 0.13f).compositeOver(baseBg)
        val surfaceContainerHighest = if (isAmoled) Color(0xFF1E2128) else rawPrimary.copy(alpha = 0.18f).compositeOver(baseBg)
        val surfaceVariant = rawPrimary.copy(alpha = if (isAmoled) 0.10f else 0.16f)
            .compositeOver(if (isAmoled) Color(0xFF16161B) else Color(0xFF23252C))
        val surfaces = listOf(baseBg, surfaceContainerLowest, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest)
        val primary = rawPrimary.withMinimumContrast(surfaces)
        val secondary = rawSecondary.withMinimumContrast(surfaces)
        val tertiary = rawTertiary.withMinimumContrast(surfaces)
        val primaryContainer = themePreset.primaryLight.darkenBy(0.2f)
        val secondaryContainer = themePreset.secondaryLight.darkenBy(0.2f)
        val tertiaryContainer = themePreset.tertiaryLight.darkenBy(0.2f)

        return darkColorScheme(
            primary = primary,
            onPrimary = primary.accessibleContentColor(),
            primaryContainer = primaryContainer,
            onPrimaryContainer = primaryContainer.accessibleContentColor(),
            secondary = secondary,
            onSecondary = secondary.accessibleContentColor(),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = secondaryContainer.accessibleContentColor(),
            tertiary = tertiary,
            onTertiary = tertiary.accessibleContentColor(),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tertiaryContainer.accessibleContentColor(),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = baseBg,
            onBackground = Color(0xFFECEAEF),
            surface = baseBg,
            onSurface = Color(0xFFECEAEF),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFCAC4D0),
            surfaceDim = if (isAmoled) Color.Black else baseBg.darkenBy(0.25f),
            surfaceBright = if (isAmoled) Color(0xFF20222A) else baseBg.lightenBy(0.08f),
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = secondary.copy(alpha = 0.45f).compositeOver(Color(0xFF747780)),
            outlineVariant = primary.copy(alpha = 0.22f).compositeOver(if (isAmoled) Color(0xFF22242B) else Color(0xFF33363F)),
            inverseSurface = Color(0xFFF3F3F7),
            inverseOnSurface = Color(0xFF1A1C20),
            inversePrimary = themePreset.primaryLight
        )
    } else {
        val rawPrimary = customPrimaryArgb?.let(::Color) ?: themePreset.primaryLight
        val rawSecondary = customAccentArgb?.let(::Color) ?: themePreset.secondaryLight
        val rawTertiary = themePreset.tertiaryLight
        val baseBg = if (customBackgroundArgb != null && Color(customBackgroundArgb).luminance() > 0.55f) {
            Color(customBackgroundArgb)
        } else {
            themePreset.backgroundLight
        }
        val surfaceContainerLowest = baseBg.lightenBy(0.018f)
        val surfaceContainerLow = rawPrimary.copy(alpha = 0.032f).compositeOver(Color.White)
        val surfaceContainer = customSurfaceArgb?.let(::Color)?.takeIf { it.luminance() > 0.6f }
            ?: rawPrimary.copy(alpha = 0.055f).compositeOver(baseBg)
        val surfaceContainerHigh = rawPrimary.copy(alpha = 0.082f).compositeOver(baseBg)
        val surfaceContainerHighest = rawPrimary.copy(alpha = 0.115f).compositeOver(baseBg)
        val surfaceVariant = rawPrimary.copy(alpha = 0.11f).compositeOver(baseBg.darkenBy(0.035f))
        val surfaces = listOf(baseBg, surfaceContainerLowest, surfaceContainerLow, surfaceContainer, surfaceContainerHigh)
        val primary = rawPrimary.withMinimumContrast(surfaces)
        val secondary = rawSecondary.withMinimumContrast(surfaces)
        val tertiary = rawTertiary.withMinimumContrast(surfaces)
        val primaryContainer = rawPrimary.copy(alpha = 0.22f).compositeOver(surfaceContainerLowest)
        val secondaryContainer = rawSecondary.copy(alpha = 0.20f).compositeOver(surfaceContainerLowest)
        val tertiaryContainer = rawTertiary.copy(alpha = 0.18f).compositeOver(surfaceContainerLowest)

        return lightColorScheme(
            primary = primary,
            onPrimary = primary.accessibleContentColor(),
            primaryContainer = primaryContainer,
            onPrimaryContainer = primaryContainer.accessibleContentColor(),
            secondary = secondary,
            onSecondary = secondary.accessibleContentColor(),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = secondaryContainer.accessibleContentColor(),
            tertiary = tertiary,
            onTertiary = tertiary.accessibleContentColor(),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tertiaryContainer.accessibleContentColor(),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF93000A),
            background = baseBg,
            onBackground = Color(0xFF1A1C1E),
            surface = baseBg,
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF49454F),
            surfaceDim = baseBg.darkenBy(0.075f),
            surfaceBright = baseBg.lightenBy(0.014f),
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = secondary.copy(alpha = 0.55f).compositeOver(Color(0xFF79747E)),
            outlineVariant = primary.copy(alpha = 0.20f).compositeOver(Color(0xFFCAC4D0)),
            inverseSurface = Color(0xFF1E2025),
            inverseOnSurface = Color(0xFFF3F3F7),
            inversePrimary = themePreset.primaryDark
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MpvxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: ThemePreset = ThemePreset.MPVX_CYAN,
    isAmoled: Boolean = false,
    dynamicColor: Boolean = false,
    customPrimaryArgb: Long? = null,
    customAccentArgb: Long? = null,
    customBackgroundArgb: Long? = null,
    customSurfaceArgb: Long? = null,
    useSystemFont: Boolean = AppSettings.useSystemFont.collectAsState().value,
    showCustomThemeBackdrop: Boolean = true,
    transitionState: ThemeTransitionState = rememberThemeTransitionState(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val wallpaperUri by AppSettings.customThemeWallpaperUri.collectAsState()
    val wallpaperIsVideo by AppSettings.customThemeWallpaperIsVideo.collectAsState()
    val wallpaperBlur by AppSettings.customThemeWallpaperBlur.collectAsState()
    val wallpaperBrightness by AppSettings.customThemeWallpaperBrightness.collectAsState()
    val wallpaperSaturation by AppSettings.customThemeWallpaperSaturation.collectAsState()
    val wallpaperScrim by AppSettings.customThemeWallpaperScrimAlpha.collectAsState()
    val customSurfaceOpacity by AppSettings.customThemeSurfaceOpacity.collectAsState()
    val pendingOrigin by AppSettings.themeTransitionOrigin.collectAsState()
    val scope = rememberCoroutineScope()
    val animScale by AppSettings.uiAnimationScale.collectAsState()

    val hasWallpaper = showCustomThemeBackdrop && wallpaperUri.isNotBlank()

    // Trigger circular reveal whenever AppSettings registers a theme tap coordinate
    DisposableEffect(pendingOrigin) {
        if (pendingOrigin != null) {
            val tap = Offset(pendingOrigin!!.first, pendingOrigin!!.second)
            scope.launch {
                transitionState.animateThemeChange(
                    tapOrigin = tap,
                    reduceMotion = animScale <= 0f,
                    applyChange = { AppSettings.setThemeTransitionOrigin(null) }
                )
            }
        }
        onDispose {}
    }

    val baseScheme = remember(
        themePreset, darkTheme, isAmoled, dynamicColor,
        customPrimaryArgb, customAccentArgb, customBackgroundArgb, customSurfaceArgb
    ) {
        resolveAppColorScheme(
            context = context,
            themePreset = themePreset,
            darkTheme = darkTheme,
            isAmoled = isAmoled,
            dynamicColor = dynamicColor,
            customPrimaryArgb = customPrimaryArgb,
            customAccentArgb = customAccentArgb,
            customBackgroundArgb = customBackgroundArgb,
            customSurfaceArgb = customSurfaceArgb
        )
    }

    val colorScheme = if (hasWallpaper) {
        val alphaScale = customSurfaceOpacity.coerceIn(0.25f, 1.0f)
        baseScheme.copy(
            background = baseScheme.background.copy(alpha = (alphaScale * 0.42f).coerceIn(0.12f, 0.72f)),
            surface = baseScheme.surface.copy(alpha = (alphaScale * 0.45f).coerceIn(0.14f, 0.78f)),
            surfaceContainerLowest = baseScheme.surfaceContainerLowest.copy(alpha = alphaScale),
            surfaceContainerLow = baseScheme.surfaceContainerLow.copy(alpha = alphaScale),
            surfaceContainer = baseScheme.surfaceContainer.copy(alpha = (alphaScale * 0.92f).coerceIn(0.25f, 0.96f)),
            surfaceContainerHigh = baseScheme.surfaceContainerHigh.copy(alpha = (alphaScale * 0.95f).coerceIn(0.3f, 0.98f))
        )
    } else {
        baseScheme
    }

    val extendedColors = if (darkTheme) {
        ExtendedColors(
            badgeBlueBg = colorScheme.primaryContainer,
            badgeBlueText = colorScheme.onPrimaryContainer,
            streakOrange = SoftAmberPillText,
            streakOrangeBg = if (isAmoled) Color(0xFF381A05) else SoftAmberPillBgDark,
            badgeGreyBg = colorScheme.surfaceContainerHigh,
            badgeGreyText = colorScheme.onSurfaceVariant,
            cardBackground = colorScheme.surfaceContainer,
            cardBorder = colorScheme.outlineVariant.copy(alpha = 0.45f),
            navIndicator = colorScheme.secondaryContainer,
            frostedSheet = FrostedGlassSheet,
            liquidGlassSurface = colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
            liquidGlassBorder = colorScheme.outlineVariant.copy(alpha = 0.65f),
            softSkyBg = colorScheme.primaryContainer,
            softSkyText = colorScheme.onPrimaryContainer,
            softAmberBg = if (isAmoled) Color(0xFF381A05) else SoftAmberPillBgDark,
            softAmberText = SoftAmberPillTextDark
        )
    } else {
        ExtendedColors(
            badgeBlueBg = colorScheme.primaryContainer,
            badgeBlueText = colorScheme.onPrimaryContainer,
            streakOrange = SoftAmberPillText,
            streakOrangeBg = SoftAmberPillBg,
            badgeGreyBg = colorScheme.surfaceContainerHigh,
            badgeGreyText = colorScheme.onSurfaceVariant,
            cardBackground = colorScheme.surfaceContainerLowest,
            cardBorder = colorScheme.outlineVariant.copy(alpha = 0.45f),
            navIndicator = colorScheme.secondaryContainer,
            frostedSheet = FrostedGlassLight,
            liquidGlassSurface = colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
            liquidGlassBorder = colorScheme.outlineVariant.copy(alpha = 0.65f),
            softSkyBg = colorScheme.primaryContainer,
            softSkyText = colorScheme.onPrimaryContainer,
            softAmberBg = SoftAmberPillBg,
            softAmberText = SoftAmberPillText
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalThemeTransitionState provides transitionState,
        LocalEmphasizedTypography provides AppEmphasizedTypography
    ) {
        val themedBody: @Composable () -> Unit = {
            ThemeTransitionOverlay(state = transitionState) {
                MaterialExpressiveTheme(
                    colorScheme = colorScheme,
                    motionScheme = MotionScheme.expressive(),
                    shapes = AppShapes,
                    typography = mpvxTypography(useSystemFont),
                    content = content
                )
            }
        }

        if (hasWallpaper) {
            CustomThemeBackdrop(
                uriString = wallpaperUri,
                isVideo = wallpaperIsVideo,
                blurDp = wallpaperBlur,
                brightness = wallpaperBrightness,
                saturation = wallpaperSaturation,
                scrimAlpha = wallpaperScrim,
                content = themedBody
            )
        } else {
            themedBody()
        }
    }
}

@Composable
private fun ThemeTransitionOverlay(
    state: ThemeTransitionState,
    content: @Composable () -> Unit
) {
    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember(graphicsContext) { graphicsContext.createGraphicsLayer() }
    DisposableEffect(graphicsContext, graphicsLayer) {
        state.snapshotLayer = graphicsLayer
        onDispose {
            if (state.snapshotLayer === graphicsLayer) {
                state.snapshotLayer = null
            }
            graphicsContext.releaseGraphicsLayer(graphicsLayer)
        }
    }
    state.snapshotLayer = graphicsLayer
    val featherPx = 30.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (state.captureRequested && !state.isAnimating) {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                    }
                    drawContent()
                }
        ) {
            content()
        }

        if (state.isAnimating) {
            val progress = state.progress.value
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithCache {
                        val maxRadius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
                        val feather = featherPx.toPx()
                        onDrawWithContent {
                            drawLayer(graphicsLayer)
                            if (progress > 0f) {
                                val revealRadius = (maxRadius * progress + feather * (1f - progress)).coerceAtLeast(1f)
                                val edgeStart = ((revealRadius - feather) / revealRadius).coerceIn(0f, 0.99f)
                                val brush = Brush.radialGradient(
                                    0f to Color.Transparent,
                                    edgeStart to Color.Transparent,
                                    1f to Color.Black,
                                    center = state.origin,
                                    radius = revealRadius
                                )
                                drawRect(brush = brush, blendMode = BlendMode.DstIn)
                            }
                        }
                    }
            )
        }
    }
}

@Composable
private fun CustomThemeBackdrop(
    uriString: String,
    isVideo: Boolean,
    blurDp: Float,
    brightness: Float,
    saturation: Float,
    scrimAlpha: Float,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorMatrixFilter = remember(brightness, saturation) {
        val matrix = ColorMatrix().apply { setToSaturation(saturation) }
        val vals = matrix.values.copyOf()
        for (idx in intArrayOf(0, 1, 2, 4, 5, 6, 7, 9, 10, 11, 12, 14)) {
            vals[idx] *= brightness
        }
        ColorFilter.colorMatrix(ColorMatrix(vals))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) {
            val exo = remember(uriString) {
                ExoPlayer.Builder(context).build().apply {
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = 0f
                    val parsed = if (uriString.startsWith("/")) Uri.fromFile(File(uriString)) else Uri.parse(uriString)
                    setMediaItem(MediaItem.fromUri(parsed))
                    prepare()
                    playWhenReady = true
                }
            }
            DisposableEffect(exo) {
                onDispose {
                    runCatching { exo.release() }
                }
            }
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).also { tv -> exo.setVideoTextureView(tv) }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurDp > 0f) Modifier.blur(blurDp.dp) else Modifier)
            )
        } else {
            val bitmap by produceState<Bitmap?>(initialValue = null, uriString) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        if (uriString.startsWith("/")) {
                            BitmapFactory.decodeFile(uriString)
                        } else {
                            context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                    }.getOrNull()
                }
            }
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorMatrixFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurDp > 0f) Modifier.blur(blurDp.dp) else Modifier)
                )
            }
        }

        val effectiveScrim = if (isVideo) {
            (scrimAlpha + (1f - brightness).coerceIn(0f, 0.5f) * 0.4f).coerceIn(0f, 0.9f)
        } else {
            scrimAlpha.coerceIn(0f, 0.9f)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = effectiveScrim))
        )
        content()
    }
}

/**
 * Utility for sampling first video frame or image bitmap and saving wallpaper safely to app storage.
 */
object CustomThemeMediaHelper {
    suspend fun copyMediaToAppStorage(context: Context, sourceUri: Uri, isVideo: Boolean): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "themes").apply { mkdirs() }
                val ext = if (isVideo) "mp4" else "jpg"
                val dest = File(dir, "custom_wallpaper_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
                // Remove older custom wallpapers so cache never bloats
                dir.listFiles()?.filter { it.name != dest.name }?.forEach { it.delete() }
                dest.absolutePath
            }.getOrNull()
        }

    suspend fun samplePrimaryColorArgb(path: String, isVideo: Boolean): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val bmp: Bitmap? = if (isVideo) {
                MediaMetadataRetriever().run {
                    setDataSource(path)
                    getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC).also { release() }
                }
            } else {
                BitmapFactory.decodeFile(path)
            }
            bmp?.let {
                val scaled = Bitmap.createScaledBitmap(it, 12, 12, true)
                var rSum = 0L
                var gSum = 0L
                var bSum = 0L
                var count = 0L
                for (x in 0 until scaled.width) {
                    for (y in 0 until scaled.height) {
                        val px = scaled.getPixel(x, y)
                        rSum += (px shr 16) and 0xFF
                        gSum += (px shr 8) and 0xFF
                        bSum += px and 0xFF
                        count++
                    }
                }
                scaled.recycle()
                if (count == 0L) null
                else {
                    val argb = (0xFF shl 24) or
                        (((rSum / count).toInt() and 0xFF) shl 16) or
                        (((gSum / count).toInt() and 0xFF) shl 8) or
                        ((bSum / count).toInt() and 0xFF)
                    argb.toLong() and 0xFFFFFFFFL
                }
            }
        }.getOrNull()
    }
}
