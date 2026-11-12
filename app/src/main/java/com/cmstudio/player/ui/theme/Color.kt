package com.cmstudio.player.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Primary Electric Indigo-Blue & Daylight Theme Colors
val ElectricIndigo = Color(0xFF1B6EF3)
val ElectricIndigoDark = Color(0xFF3B82F6)
val ElectricIndigoLight = Color(0xFF60A5FA)

// Layered Surface Material 3 Tokens
val CanvasCoolAlabaster = Color(0xFFF8F9FA)
val BentoCardWhite = Color(0xFFFFFFFF)
val BentoCardElevated = Color(0xFFF1F5F9)

val CanvasMidnightSlate = Color(0xFF0F172A)
val BentoCardDark = Color(0xFF1E293B)
val BentoCardDarkElevated = Color(0xFF334155)

// True Pitch-Black OLED Mode
val CanvasPitchBlack = Color(0xFF000000)
val BentoCardPitchBlack = Color(0xFF0C0D10)
val BentoCardAmoledElevated = Color(0xFF16181D)
val OutlineAmoled = Color(0xFF22252C)

// High-Contrast Typography
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)
val TextMutedLight = Color(0xFF94A3B8)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Micro-Tag Accents
val SoftSkyPillBg = Color(0xFFE8F0FE)
val SoftSkyPillText = Color(0xFF1967D2)
val SoftSkyPillBgDark = Color(0xFF0C4A6E)
val SoftSkyPillTextDark = Color(0xFF7DD3FC)

val SoftAmberPillBg = Color(0xFFFFF3E0)
val SoftAmberPillText = Color(0xFFFF7A00)
val SoftAmberPillBgDark = Color(0xFF78350F)
val SoftAmberPillTextDark = Color(0xFFFCD34D)

val SoftEmeraldPillBg = Color(0xFFD1FAE5)
val SoftEmeraldPillText = Color(0xFF047857)
val SoftEmeraldPillBgDark = Color(0xFF064E3B)
val SoftEmeraldPillTextDark = Color(0xFF6EE7B7)

// Navigation Active Indicators
val NavIndicatorLight = Color(0xFFDBEAFE)
val NavIndicatorDark = Color(0xFF1E3A8A)

// Outlines & Dividers
val OutlineLight = Color(0xFFE2E8F0)
val OutlineDark = Color(0xFF334155)

// Frosted Liquid Glass Surface
val LiquidGlassSurface = Color(0xC71E293B)
val LiquidGlassSurfaceLight = Color(0xC7FFFFFF)
val LiquidGlassBorder = Color(0x33FFFFFF)
val FrostedGlassSheet = Color(0xC70A0A0F)
val FrostedGlassLight = Color(0xEBFFFFFF)

/**
 * Dual-mode (Light + Dark + AMOLED) color themes with full tonal surface derivation.
 * Preserves legacy `primary`/`background`/`surface`/`accent` properties alongside dedicated
 * Light and Dark roles for high-contrast WCAG accessibility.
 */
enum class ThemePreset(
    val displayName: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val tertiaryLight: Color,
    val tertiaryDark: Color,
    val backgroundLight: Color,
    val backgroundDark: Color,
    val isDynamic: Boolean = false
) {
    MPVX_CYAN(
        displayName = "MPVX Cyan (Default)",
        primaryLight = Color(0xFF0E7490),
        primaryDark = Color(0xFF70ECE4),
        secondaryLight = Color(0xFF0F766E),
        secondaryDark = Color(0xFF5EEAD4),
        tertiaryLight = Color(0xFF2563EB),
        tertiaryDark = Color(0xFF93C5FD),
        backgroundLight = Color(0xFFF2F6F6),
        backgroundDark = Color(0xFF111517)
    ),
    MPVX_EXPRESSIVE(
        displayName = "MPVX Expressive",
        primaryLight = Color(0xFF794F81),
        primaryDark = Color(0xFFE8B5EF),
        secondaryLight = Color(0xFF6A596C),
        secondaryDark = Color(0xFFD6C0D6),
        tertiaryLight = Color(0xFF82524D),
        tertiaryDark = Color(0xFFF5B7B0),
        backgroundLight = Color(0xFFF7F5F8),
        backgroundDark = Color(0xFF161217)
    ),
    DYNAMIC(
        displayName = "Dynamic (Material You)",
        primaryLight = Color(0xFF6750A4),
        primaryDark = Color(0xFFD0BCFF),
        secondaryLight = Color(0xFF625B71),
        secondaryDark = Color(0xFFCCC2DC),
        tertiaryLight = Color(0xFF7D5260),
        tertiaryDark = Color(0xFFEFB8C8),
        backgroundLight = Color(0xFFFFFBFF),
        backgroundDark = Color(0xFF1C1B1F),
        isDynamic = true
    ),
    DEFAULT_CLEAN(
        displayName = "Cobalt Indigo",
        primaryLight = Color(0xFF1B6EF3),
        primaryDark = Color(0xFF60A5FA),
        secondaryLight = Color(0xFF0284C7),
        secondaryDark = Color(0xFF38BDF8),
        tertiaryLight = Color(0xFF4F46E5),
        tertiaryDark = Color(0xFFA5B4FC),
        backgroundLight = Color(0xFFF4F7FD),
        backgroundDark = Color(0xFF0F172A)
    ),
    CATPPUCCIN(
        displayName = "Catppuccin Mocha",
        primaryLight = Color(0xFF4C6B9A),
        primaryDark = Color(0xFF89B4FA),
        secondaryLight = Color(0xFFB76B8F),
        secondaryDark = Color(0xFFF5C2E7),
        tertiaryLight = Color(0xFFB8763E),
        tertiaryDark = Color(0xFF94E2D5),
        backgroundLight = Color(0xFFEFF1F5),
        backgroundDark = Color(0xFF1E1E2E)
    ),
    CATPPUCCIN_LATTE(
        displayName = "Catppuccin Latte",
        primaryLight = Color(0xFF1E66F5),
        primaryDark = Color(0xFF7287FD),
        secondaryLight = Color(0xFFEA76CB),
        secondaryDark = Color(0xFFF5C2E7),
        tertiaryLight = Color(0xFF40A02B),
        tertiaryDark = Color(0xFFA6E3A1),
        backgroundLight = Color(0xFFEFF1F5),
        backgroundDark = Color(0xFF1E1E2E)
    ),
    NORD(
        displayName = "Nord",
        primaryLight = Color(0xFF5E81AC),
        primaryDark = Color(0xFF88C0D0),
        secondaryLight = Color(0xFF4C566A),
        secondaryDark = Color(0xFFD8DEE9),
        tertiaryLight = Color(0xFFB48EAD),
        tertiaryDark = Color(0xFFD8A9C4),
        backgroundLight = Color(0xFFECEFF4),
        backgroundDark = Color(0xFF2E3440)
    ),
    TOKYO_NIGHT(
        displayName = "Tokyo Night",
        primaryLight = Color(0xFF3D5A80),
        primaryDark = Color(0xFF7AA2F7),
        secondaryLight = Color(0xFF6B5B95),
        secondaryDark = Color(0xFFBB9AF7),
        tertiaryLight = Color(0xFF4A6B5C),
        tertiaryDark = Color(0xFF73DACA),
        backgroundLight = Color(0xFFF0F1F5),
        backgroundDark = Color(0xFF1A1B26)
    ),
    DRACULA(
        displayName = "Dracula",
        primaryLight = Color(0xFF6272A4),
        primaryDark = Color(0xFFBD93F9),
        secondaryLight = Color(0xFF8B3D6B),
        secondaryDark = Color(0xFFFF79C6),
        tertiaryLight = Color(0xFF2A7A8C),
        tertiaryDark = Color(0xFF8BE9FD),
        backgroundLight = Color(0xFFF8F8F2),
        backgroundDark = Color(0xFF282A36)
    ),
    ROSE_PINE(
        displayName = "Rosé Pine",
        primaryLight = Color(0xFF907AA9),
        primaryDark = Color(0xFFC4A7E7),
        secondaryLight = Color(0xFFB4637A),
        secondaryDark = Color(0xFFEBBCBA),
        tertiaryLight = Color(0xFF286983),
        tertiaryDark = Color(0xFF9CCFD8),
        backgroundLight = Color(0xFFFAF4ED),
        backgroundDark = Color(0xFF191724)
    ),
    GRUVBOX(
        displayName = "Gruvbox",
        primaryLight = Color(0xFFAF3A03),
        primaryDark = Color(0xFFFE8019),
        secondaryLight = Color(0xFF79740E),
        secondaryDark = Color(0xFFB8BB26),
        tertiaryLight = Color(0xFF427B58),
        tertiaryDark = Color(0xFF8EC07C),
        backgroundLight = Color(0xFFFBF1C7),
        backgroundDark = Color(0xFF282828)
    ),
    KANAGAWA(
        displayName = "Kanagawa",
        primaryLight = Color(0xFF5A7785),
        primaryDark = Color(0xFF7E9CD8),
        secondaryLight = Color(0xFF8A7A6E),
        secondaryDark = Color(0xFFDCA561),
        tertiaryLight = Color(0xFF6A8E7F),
        tertiaryDark = Color(0xFF98BB6C),
        backgroundLight = Color(0xFFF2ECBC),
        backgroundDark = Color(0xFF1F1F28)
    ),
    MONOCHROME(
        displayName = "Monochrome",
        primaryLight = Color(0xFF212121),
        primaryDark = Color(0xFFE0E0E0),
        secondaryLight = Color(0xFF424242),
        secondaryDark = Color(0xFFBDBDBD),
        tertiaryLight = Color(0xFF616161),
        tertiaryDark = Color(0xFF9E9E9E),
        backgroundLight = Color(0xFFFFFFFF),
        backgroundDark = Color(0xFF0A0A0A)
    ),
    DOOM(
        displayName = "Doom",
        primaryLight = Color(0xFFBB2929),
        primaryDark = Color(0xFFFF6B6B),
        secondaryLight = Color(0xFF6B5353),
        secondaryDark = Color(0xFFD6BABA),
        tertiaryLight = Color(0xFF8C4A4A),
        tertiaryDark = Color(0xFFFFB4AB),
        backgroundLight = Color(0xFFFFF8F7),
        backgroundDark = Color(0xFF1A1010)
    ),
    GREEN_APPLE(
        displayName = "Green Apple",
        primaryLight = Color(0xFF2E7D32),
        primaryDark = Color(0xFF81C784),
        secondaryLight = Color(0xFF4A6349),
        secondaryDark = Color(0xFFB0CFB1),
        tertiaryLight = Color(0xFF3D7B5F),
        tertiaryDark = Color(0xFF8FD5B7),
        backgroundLight = Color(0xFFF6FFF6),
        backgroundDark = Color(0xFF0F1A0F)
    ),
    MOCHA(
        displayName = "Mocha",
        primaryLight = Color(0xFF795548),
        primaryDark = Color(0xFFBCAAA4),
        secondaryLight = Color(0xFF5D4037),
        secondaryDark = Color(0xFFA1887F),
        tertiaryLight = Color(0xFF6D4C41),
        tertiaryDark = Color(0xFFD7CCC8),
        backgroundLight = Color(0xFFFFF9F5),
        backgroundDark = Color(0xFF1A1512)
    ),
    STRAWBERRY(
        displayName = "Strawberry",
        primaryLight = Color(0xFFD81B60),
        primaryDark = Color(0xFFF48FB1),
        secondaryLight = Color(0xFF6B4958),
        secondaryDark = Color(0xFFD6B0C1),
        tertiaryLight = Color(0xFFC2185B),
        tertiaryDark = Color(0xFFF8BBD9),
        backgroundLight = Color(0xFFFFF5F8),
        backgroundDark = Color(0xFF1A1015)
    ),
    TIDAL(
        displayName = "Tidal",
        primaryLight = Color(0xFF00796B),
        primaryDark = Color(0xFF80CBC4),
        secondaryLight = Color(0xFF4A635E),
        secondaryDark = Color(0xFFB0CFC9),
        tertiaryLight = Color(0xFF00897B),
        tertiaryDark = Color(0xFF4DB6AC),
        backgroundLight = Color(0xFFF2FFFD),
        backgroundDark = Color(0xFF0F1A18)
    ),
    TAKO_GREEN(
        displayName = "Tako Green",
        primaryLight = Color(0xFF2E7D32),
        primaryDark = Color(0xFFA5D6A7),
        secondaryLight = Color(0xFF546E7A),
        secondaryDark = Color(0xFF90A4AE),
        tertiaryLight = Color(0xFF43A047),
        tertiaryDark = Color(0xFF81C784),
        backgroundLight = Color(0xFFF5FFF5),
        backgroundDark = Color(0xFF121A12)
    ),
    YIN_YANG(
        displayName = "Yin & Yang",
        primaryLight = Color(0xFF424242),
        primaryDark = Color(0xFFBDBDBD),
        secondaryLight = Color(0xFF616161),
        secondaryDark = Color(0xFFE0E0E0),
        tertiaryLight = Color(0xFF757575),
        tertiaryDark = Color(0xFFEEEEEE),
        backgroundLight = Color(0xFFFAFAFA),
        backgroundDark = Color(0xFF121212)
    ),
    YOTSUBA(
        displayName = "Yotsuba",
        primaryLight = Color(0xFFD84315),
        primaryDark = Color(0xFFFFAB91),
        secondaryLight = Color(0xFF6D5D5B),
        secondaryDark = Color(0xFFD6C4C2),
        tertiaryLight = Color(0xFFE64A19),
        tertiaryDark = Color(0xFFFFCCBC),
        backgroundLight = Color(0xFFFFF8F5),
        backgroundDark = Color(0xFF1A1412)
    ),
    SAPPHIRE(
        displayName = "Sapphire",
        primaryLight = Color(0xFF1E88E5),
        primaryDark = Color(0xFF64B5F6),
        secondaryLight = Color(0xFF5C6BC0),
        secondaryDark = Color(0xFF9FA8DA),
        tertiaryLight = Color(0xFF0288D1),
        tertiaryDark = Color(0xFF4FC3F7),
        backgroundLight = Color(0xFFF3F8FF),
        backgroundDark = Color(0xFF0D1620)
    ),
    ROSE_GOLD(
        displayName = "Rose Gold",
        primaryLight = Color(0xFFB76E79),
        primaryDark = Color(0xFFE8A9B0),
        secondaryLight = Color(0xFFAD8075),
        secondaryDark = Color(0xFFDDBFB8),
        tertiaryLight = Color(0xFF9C5E63),
        tertiaryDark = Color(0xFFF5D5D5),
        backgroundLight = Color(0xFFFFF5F5),
        backgroundDark = Color(0xFF1A1315)
    ),
    CORAL(
        displayName = "Coral",
        primaryLight = Color(0xFFD32F2F),
        primaryDark = Color(0xFFFF8A80),
        secondaryLight = Color(0xFFD84315),
        secondaryDark = Color(0xFFFFAB91),
        tertiaryLight = Color(0xFFE64A19),
        tertiaryDark = Color(0xFFFFCCBC),
        backgroundLight = Color(0xFFFFF5F5),
        backgroundDark = Color(0xFF1A1010)
    ),
    SLATE(
        displayName = "Slate",
        primaryLight = Color(0xFF455A64),
        primaryDark = Color(0xFF90A4AE),
        secondaryLight = Color(0xFF546E7A),
        secondaryDark = Color(0xFFB0BEC5),
        tertiaryLight = Color(0xFF607D8B),
        tertiaryDark = Color(0xFFCFD8DC),
        backgroundLight = Color(0xFFF5F7F8),
        backgroundDark = Color(0xFF151A1C)
    ),
    AURORA(
        displayName = "Aurora Blue",
        primaryLight = Color(0xFF0B3FA0),
        primaryDark = Color(0xFF00E5FF),
        secondaryLight = Color(0xFF5C6B8C),
        secondaryDark = Color(0xFF5B93FF),
        tertiaryLight = Color(0xFF3648A6),
        tertiaryDark = Color(0xFF97A8FF),
        backgroundLight = Color(0xFFF3F6FF),
        backgroundDark = Color(0xFF05101E)
    ),
    CLOUDFLARE(
        displayName = "Cloudflare Orange",
        primaryLight = Color(0xFFD65D0E),
        primaryDark = Color(0xFFF38020),
        secondaryLight = Color(0xFF6B5E4C),
        secondaryDark = Color(0xFFD6C5AC),
        tertiaryLight = Color(0xFF855316),
        tertiaryDark = Color(0xFFFAAD3F),
        backgroundLight = Color(0xFFFFFBF7),
        backgroundDark = Color(0xFF1A130B)
    ),
    COTTON_CANDY(
        displayName = "Cotton Candy",
        primaryLight = Color(0xFFC2185B),
        primaryDark = Color(0xFFF472B6),
        secondaryLight = Color(0xFF3B7A9E),
        secondaryDark = Color(0xFF38BDF8),
        tertiaryLight = Color(0xFF9C68AC),
        tertiaryDark = Color(0xFFDEB0E9),
        backgroundLight = Color(0xFFFFF8FA),
        backgroundDark = Color(0xFF1F101A)
    ),
    MIDNIGHT_DUSK(
        displayName = "Midnight Dusk",
        primaryLight = Color(0xFF0D47A1),
        primaryDark = Color(0xFF6366F1),
        secondaryLight = Color(0xFF455A64),
        secondaryDark = Color(0xFFA855F7),
        tertiaryLight = Color(0xFF1565C0),
        tertiaryDark = Color(0xFF90CAF9),
        backgroundLight = Color(0xFFF5F9FF),
        backgroundDark = Color(0xFF0B0D19)
    ),
    CYBERPUNK(
        displayName = "Cyberpunk",
        primaryLight = Color(0xFF007799),
        primaryDark = Color(0xFFFCEE0A),
        secondaryLight = Color(0xFF006080),
        secondaryDark = Color(0xFF00F0FF),
        tertiaryLight = Color(0xFFB01060),
        tertiaryDark = Color(0xFFFF38A2),
        backgroundLight = Color(0xFFF7F7FC),
        backgroundDark = Color(0xFF0D0D11)
    ),
    SOLARIZED(
        displayName = "Solarized",
        primaryLight = Color(0xFF268BD2),
        primaryDark = Color(0xFF56B4E9),
        secondaryLight = Color(0xFF2AA198),
        secondaryDark = Color(0xFF2AA198),
        tertiaryLight = Color(0xFFB58900),
        tertiaryDark = Color(0xFFD8A000),
        backgroundLight = Color(0xFFFDF6E3),
        backgroundDark = Color(0xFF002B36)
    ),
    MONOKAI(
        displayName = "Monokai",
        primaryLight = Color(0xFF4E8C00),
        primaryDark = Color(0xFFA6E22E),
        secondaryLight = Color(0xFFB80054),
        secondaryDark = Color(0xFFF92672),
        tertiaryLight = Color(0xFF00798C),
        tertiaryDark = Color(0xFF66D9EF),
        backgroundLight = Color(0xFFF9F9F5),
        backgroundDark = Color(0xFF272822)
    ),
    EMERALD(
        displayName = "Emerald",
        primaryLight = Color(0xFF047857),
        primaryDark = Color(0xFF10B981),
        secondaryLight = Color(0xFF065F46),
        secondaryDark = Color(0xFF34D399),
        tertiaryLight = Color(0xFF0D9488),
        tertiaryDark = Color(0xFF5EEAD4),
        backgroundLight = Color(0xFFF2FBF7),
        backgroundDark = Color(0xFF062017)
    ),
    DEFAULT_AMETHYST(
        displayName = "Amethyst",
        primaryLight = Color(0xFF7E22CE),
        primaryDark = Color(0xFFC084FC),
        secondaryLight = Color(0xFF6B21A8),
        secondaryDark = Color(0xFFA855F7),
        tertiaryLight = Color(0xFF9333EA),
        tertiaryDark = Color(0xFFE9D5FF),
        backgroundLight = Color(0xFFFAF5FF),
        backgroundDark = Color(0xFF0F0C1B)
    ),
    MATRIX(
        displayName = "Matrix Green",
        primaryLight = Color(0xFF007A33),
        primaryDark = Color(0xFF00FF66),
        secondaryLight = Color(0xFF15803D),
        secondaryDark = Color(0xFF22C55E),
        tertiaryLight = Color(0xFF047857),
        tertiaryDark = Color(0xFF4ADE80),
        backgroundLight = Color(0xFFF2FFF7),
        backgroundDark = Color(0xFF021208)
    ),
    SUNSET_ORANGE(
        displayName = "Sunset Orange",
        primaryLight = Color(0xFFD84315),
        primaryDark = Color(0xFFFF7043),
        secondaryLight = Color(0xFFEF6C00),
        secondaryDark = Color(0xFFFFB74D),
        tertiaryLight = Color(0xFFE65100),
        tertiaryDark = Color(0xFFFFCC80),
        backgroundLight = Color(0xFFFFF5F0),
        backgroundDark = Color(0xFF1E0E08)
    ),
    CRIMSON_VELVET(
        displayName = "Crimson Velvet",
        primaryLight = Color(0xFFBE123C),
        primaryDark = Color(0xFFFB7185),
        secondaryLight = Color(0xFF9F1239),
        secondaryDark = Color(0xFFF43F5E),
        tertiaryLight = Color(0xFFE11D48),
        tertiaryDark = Color(0xFFFDA4AF),
        backgroundLight = Color(0xFFFFF3F5),
        backgroundDark = Color(0xFF19060C)
    ),
    FOREST_SAGE(
        displayName = "Forest Sage",
        primaryLight = Color(0xFF3F6212),
        primaryDark = Color(0xFFA3E635),
        secondaryLight = Color(0xFF4D7C0F),
        secondaryDark = Color(0xFF84CC16),
        tertiaryLight = Color(0xFF15803D),
        tertiaryDark = Color(0xFF6EE7B7),
        backgroundLight = Color(0xFFF6FAEE),
        backgroundDark = Color(0xFF0D1606)
    ),
    DEEP_OCEAN(
        displayName = "Deep Ocean",
        primaryLight = Color(0xFF006064),
        primaryDark = Color(0xFF4DD0E1),
        secondaryLight = Color(0xFF0284C7),
        secondaryDark = Color(0xFF38BDF8),
        tertiaryLight = Color(0xFF0097A7),
        tertiaryDark = Color(0xFF26C6DA),
        backgroundLight = Color(0xFFF0FFFF),
        backgroundDark = Color(0xFF031422)
    ),
    LAVENDER_DREAM(
        displayName = "Lavender Dream",
        primaryLight = Color(0xFF6D28D9),
        primaryDark = Color(0xFFA78BFA),
        secondaryLight = Color(0xFF7C3AED),
        secondaryDark = Color(0xFFC4B5FD),
        tertiaryLight = Color(0xFF5B21B6),
        tertiaryDark = Color(0xFFDDD6FE),
        backgroundLight = Color(0xFFFAF7FF),
        backgroundDark = Color(0xFF130E24)
    ),
    CYBER_GOLD(
        displayName = "Amber & Gold",
        primaryLight = Color(0xFFB45309),
        primaryDark = Color(0xFFFACC15),
        secondaryLight = Color(0xFFD97706),
        secondaryDark = Color(0xFFFDE047),
        tertiaryLight = Color(0xFFA16207),
        tertiaryDark = Color(0xFFFEF08A),
        backgroundLight = Color(0xFFFFFBF0),
        backgroundDark = Color(0xFF191404)
    ),
    NEON_MINT(
        displayName = "Neon Mint",
        primaryLight = Color(0xFF0D9488),
        primaryDark = Color(0xFF2DD4BF),
        secondaryLight = Color(0xFF0F766E),
        secondaryDark = Color(0xFF14B8A6),
        tertiaryLight = Color(0xFF059669),
        tertiaryDark = Color(0xFF5EEAD4),
        backgroundLight = Color(0xFFF0FDFA),
        backgroundDark = Color(0xFF041816)
    ),
    SEPIA_WARMTH(
        displayName = "Sepia Warmth",
        primaryLight = Color(0xFF9A3412),
        primaryDark = Color(0xFFF59E0B),
        secondaryLight = Color(0xFFB45309),
        secondaryDark = Color(0xFFD97706),
        tertiaryLight = Color(0xFF78350F),
        tertiaryDark = Color(0xFFFBBF24),
        backgroundLight = Color(0xFFFFF7ED),
        backgroundDark = Color(0xFF1A140F)
    );

    val primary: Color get() = primaryDark
    val background: Color get() = backgroundDark
    val surface: Color get() = backgroundDark
    val accent: Color get() = secondaryDark
}

val PrimaryBlue = ElectricIndigo
val PrimaryBlueVariant = Color(0xFF1D4ED8)
val PrimaryBlueLight = ElectricIndigoLight
val PrimaryBlueDark = ElectricIndigoDark
val BackgroundLight = CanvasCoolAlabaster
val SurfaceLight = BentoCardWhite
val SurfaceLightElevated = BentoCardElevated
val BackgroundDark = CanvasMidnightSlate
val SurfaceDark = BentoCardDark
val SurfaceDarkElevated = BentoCardDarkElevated
val BackgroundAmoled = CanvasPitchBlack
val SurfaceAmoled = BentoCardPitchBlack
val SurfaceAmoledElevated = BentoCardAmoledElevated
val BadgeBlueBgLight = SoftSkyPillBg
val BadgeBlueTextLight = SoftSkyPillText
val BadgeBlueBgDark = SoftSkyPillBgDark
val BadgeBlueTextDark = SoftSkyPillTextDark
val StreakOrange = SoftAmberPillText
val StreakOrangeBgLight = SoftAmberPillBg
val StreakOrangeBgDark = SoftAmberPillBgDark
val BadgeGreyBgLight = Color(0xFFF1F5F9)
val BadgeGreyTextLight = Color(0xFF475569)
val BadgeGreyBgDark = Color(0xFF1E293B)
val BadgeGreyTextDark = Color(0xFF94A3B8)
val FrostedSheetDark = FrostedGlassSheet
val FrostedSheetLight = FrostedGlassLight

@Immutable
data class ExtendedColors(
    val badgeBlueBg: Color,
    val badgeBlueText: Color,
    val streakOrange: Color,
    val streakOrangeBg: Color,
    val badgeGreyBg: Color,
    val badgeGreyText: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val navIndicator: Color,
    val frostedSheet: Color = FrostedGlassSheet,
    val liquidGlassSurface: Color = LiquidGlassSurface,
    val liquidGlassBorder: Color = LiquidGlassBorder,
    val softSkyBg: Color = SoftSkyPillBg,
    val softSkyText: Color = SoftSkyPillText,
    val softAmberBg: Color = SoftAmberPillBg,
    val softAmberText: Color = SoftAmberPillText
)
