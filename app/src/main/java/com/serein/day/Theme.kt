package com.serein.day

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Serene Forest Mint 设计系统（stitch_serein_day_countdown(1)）的调色板。 */
data class SereinPalette(
    val name: String,
    val surface: Color,
    val surfaceLow: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val onAccentStrong: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val error: Color,
    val isDark: Boolean = false
)

// 跨调色板共享的庆祝色（蜜棕 / 蜜桃，用于生日卡与徽章）
val Honey = Color(0xFF845400)
val OnHoney = Color(0xFFFFD198)
val Peach = Color(0xFFFFDDB6)
val OnPeach = Color(0xFF2A1800)
val SunBadge = Color(0xFFFBBA65)

private val forestMintLight = SereinPalette(
    name = "灰绿",
    surface = Color(0xFFF3FCF4), surfaceLow = Color(0xFFEDF6EE),
    container = Color(0xFFE7F0E9), high = Color(0xFFE1EAE3), highest = Color(0xFFDCE5DD),
    onSurface = Color(0xFF151D19), onSurfaceVariant = Color(0xFF3E4946), outlineVariant = Color(0xFFBEC9C4),
    primary = Color(0xFF006B5B), onPrimary = Color.White,
    accent = Color(0xFF9FF2DE), onAccent = Color(0xFF00201A), onAccentStrong = Color(0xFF005144),
    secondaryContainer = Color(0xFFCCE6D9), onSecondaryContainer = Color(0xFF354B42),
    error = Color(0xFFBA1A1A)
)

private val forestMintDark = SereinPalette(
    name = "灰绿",
    surface = Color(0xFF0F1512), surfaceLow = Color(0xFF131A16),
    container = Color(0xFF18201B), high = Color(0xFF1E2620), highest = Color(0xFF242C26),
    onSurface = Color(0xFFE1E7E2), onSurfaceVariant = Color(0xFFA9B4AC), outlineVariant = Color(0xFF3A423D),
    primary = Color(0xFF83D6C2), onPrimary = Color(0xFF00201A),
    accent = Color(0xFF9FF2DE), onAccent = Color(0xFF00201A), onAccentStrong = Color(0xFF005144),
    secondaryContainer = Color(0xFF2B3A34), onSecondaryContainer = Color(0xFFCBE6D8),
    error = Color(0xFFFFB4AB), isDark = true
)

private val softPurpleLight = SereinPalette(
    name = "柔紫",
    surface = Color(0xFFF8F4FC), surfaceLow = Color(0xFFF4EFF9),
    container = Color(0xFFF0E9F6), high = Color(0xFFEAE2F1), highest = Color(0xFFE4DCEC),
    onSurface = Color(0xFF1B1822), onSurfaceVariant = Color(0xFF474251), outlineVariant = Color(0xFFCDC5D5),
    primary = Color(0xFF6750A4), onPrimary = Color.White,
    accent = Color(0xFFE9DDFF), onAccent = Color(0xFF22005D), onAccentStrong = Color(0xFF4F378A),
    secondaryContainer = Color(0xFFE8DEF9), onSecondaryContainer = Color(0xFF4A4358),
    error = Color(0xFFBA1A1A)
)

private val softPurpleDark = SereinPalette(
    name = "柔紫",
    surface = Color(0xFF131118), surfaceLow = Color(0xFF17151E),
    container = Color(0xFF1D1A25), high = Color(0xFF232030), highest = Color(0xFF292537),
    onSurface = Color(0xFFE4E0EC), onSurfaceVariant = Color(0xFFAFA9C0), outlineVariant = Color(0xFF3B3648),
    primary = Color(0xFFCFBCFF), onPrimary = Color(0xFF22005D),
    accent = Color(0xFFE9DDFF), onAccent = Color(0xFF22005D), onAccentStrong = Color(0xFF4F378A),
    secondaryContainer = Color(0xFF322C4A), onSecondaryContainer = Color(0xFFDFD6F7),
    error = Color(0xFFFFB4AB), isDark = true
)

private val ochreLight = SereinPalette(
    name = "赭红",
    surface = Color(0xFFFCF4F1), surfaceLow = Color(0xFFF8F0EB),
    container = Color(0xFFF4E9E2), high = Color(0xFFEFDFD5), highest = Color(0xFFE9D8CD),
    onSurface = Color(0xFF201A17), onSurfaceVariant = Color(0xFF4D443E), outlineVariant = Color(0xFFD8C7BB),
    primary = Color(0xFF8C4433), onPrimary = Color.White,
    accent = Color(0xFFFFDBD1), onAccent = Color(0xFF3A0B01), onAccentStrong = Color(0xFF5D2A18),
    secondaryContainer = Color(0xFFF2DED6), onSecondaryContainer = Color(0xFF4A3B32),
    error = Color(0xFFBA1A1A)
)

private val ochreDark = SereinPalette(
    name = "赭红",
    surface = Color(0xFF171210), surfaceLow = Color(0xFF1B1613),
    container = Color(0xFF211B17), high = Color(0xFF27201B), highest = Color(0xFF2D251F),
    onSurface = Color(0xFFEDE3DC), onSurfaceVariant = Color(0xFFB4A69C), outlineVariant = Color(0xFF453830),
    primary = Color(0xFFFFB5A5), onPrimary = Color(0xFF3A0B01),
    accent = Color(0xFFFFDBD1), onAccent = Color(0xFF3A0B01), onAccentStrong = Color(0xFF5D2A18),
    secondaryContainer = Color(0xFF3A2C25), onSecondaryContainer = Color(0xFFF2D9CE),
    error = Color(0xFFFFB4AB), isDark = true
)

private val indigoLight = SereinPalette(
    name = "靛蓝",
    surface = Color(0xFFF2F5FC), surfaceLow = Color(0xFFEDF1FA),
    container = Color(0xFFE8EDF7), high = Color(0xFFE1E7F3), highest = Color(0xFFDBE1EF),
    onSurface = Color(0xFF181C24), onSurfaceVariant = Color(0xFF434956), outlineVariant = Color(0xFFC9CEDC),
    primary = Color(0xFF3B5599), onPrimary = Color.White,
    accent = Color(0xFFD6E2FF), onAccent = Color(0xFF002159), onAccentStrong = Color(0xFF2C4484),
    secondaryContainer = Color(0xFFD9E2F8), onSecondaryContainer = Color(0xFF3D465C),
    error = Color(0xFFBA1A1A)
)

private val indigoDark = SereinPalette(
    name = "靛蓝",
    surface = Color(0xFF11131A), surfaceLow = Color(0xFF151821),
    container = Color(0xFF1A1E29), high = Color(0xFF20242F), highest = Color(0xFF262A37),
    onSurface = Color(0xFFE2E5EE), onSurfaceVariant = Color(0xFFA8AEBF), outlineVariant = Color(0xFF383D4C),
    primary = Color(0xFFB3C5FF), onPrimary = Color(0xFF002159),
    accent = Color(0xFFD6E2FF), onAccent = Color(0xFF002159), onAccentStrong = Color(0xFF2C4484),
    secondaryContainer = Color(0xFF2C3348), onSecondaryContainer = Color(0xFFD6E0F8),
    error = Color(0xFFFFB4AB), isDark = true
)

private val amberLight = SereinPalette(
    name = "蜜橙",
    surface = Color(0xFFFFF8F0), surfaceLow = Color(0xFFFDF3E7),
    container = Color(0xFFF9EDDE), high = Color(0xFFF3E7D7), highest = Color(0xFFEDE1D0),
    onSurface = Color(0xFF201B12), onSurfaceVariant = Color(0xFF4F4639), outlineVariant = Color(0xFFDBD0C0),
    primary = Color(0xFF8B5A00), onPrimary = Color.White,
    accent = Color(0xFFFFDEA8), onAccent = Color(0xFF2B1700), onAccentStrong = Color(0xFF7A4F00),
    secondaryContainer = Color(0xFFF2E1C8), onSecondaryContainer = Color(0xFF4F4232),
    error = Color(0xFFBA1A1A)
)

private val amberDark = SereinPalette(
    name = "蜜橙",
    surface = Color(0xFF17120B), surfaceLow = Color(0xFF1B160F),
    container = Color(0xFF211C13), high = Color(0xFF282218), highest = Color(0xFF2E281E),
    onSurface = Color(0xFFEEE2D4), onSurfaceVariant = Color(0xFFB7AB9A), outlineVariant = Color(0xFF443B2C),
    primary = Color(0xFFFFC766), onPrimary = Color(0xFF2B1700),
    accent = Color(0xFFFFDEA8), onAccent = Color(0xFF2B1700), onAccentStrong = Color(0xFF7A4F00),
    secondaryContainer = Color(0xFF3A3122), onSecondaryContainer = Color(0xFFEFDCC2),
    error = Color(0xFFFFB4AB), isDark = true
)

private val roseLight = SereinPalette(
    name = "玫红",
    surface = Color(0xFFFCF4F8), surfaceLow = Color(0xFFF9EFF4),
    container = Color(0xFFF5E9F0), high = Color(0xFFEFE2EA), highest = Color(0xFFE9DCE4),
    onSurface = Color(0xFF1F1A1D), onSurfaceVariant = Color(0xFF4C4447), outlineVariant = Color(0xFFD5C8CC),
    primary = Color(0xFF984061), onPrimary = Color.White,
    accent = Color(0xFFFFD9E2), onAccent = Color(0xFF3E001D), onAccentStrong = Color(0xFF8E4957),
    secondaryContainer = Color(0xFFF3DAE4), onSecondaryContainer = Color(0xFF4F3D44),
    error = Color(0xFFBA1A1A)
)

private val roseDark = SereinPalette(
    name = "玫红",
    surface = Color(0xFF181114), surfaceLow = Color(0xFF1C1518),
    container = Color(0xFF221A1E), high = Color(0xFF281F23), highest = Color(0xFF2E2529),
    onSurface = Color(0xFFEBDFE3), onSurfaceVariant = Color(0xFFB2A6AA), outlineVariant = Color(0xFF44383D),
    primary = Color(0xFFFFB1C8), onPrimary = Color(0xFF3E001D),
    accent = Color(0xFFFFD9E2), onAccent = Color(0xFF3E001D), onAccentStrong = Color(0xFF8E4957),
    secondaryContainer = Color(0xFF37222B), onSecondaryContainer = Color(0xFFEBD5DE),
    error = Color(0xFFFFB4AB), isDark = true
)

private val cyanLight = SereinPalette(
    name = "青碧",
    surface = Color(0xFFF4FAFB), surfaceLow = Color(0xFFEEF5F6),
    container = Color(0xFFE8F0F1), high = Color(0xFFE2EAEB), highest = Color(0xFFDCE4E5),
    onSurface = Color(0xFF161D1D), onSurfaceVariant = Color(0xFF3E4949), outlineVariant = Color(0xFFBCC8C8),
    primary = Color(0xFF006874), onPrimary = Color.White,
    accent = Color(0xFFB4EBFF), onAccent = Color(0xFF001F24), onAccentStrong = Color(0xFF004F59),
    secondaryContainer = Color(0xFFC9E7EA), onSecondaryContainer = Color(0xFF35494C),
    error = Color(0xFFBA1A1A)
)

private val cyanDark = SereinPalette(
    name = "青碧",
    surface = Color(0xFF0F1415), surfaceLow = Color(0xFF131819),
    container = Color(0xFF181E1F), high = Color(0xFF1E2425), highest = Color(0xFF242A2B),
    onSurface = Color(0xFFDFE7E8), onSurfaceVariant = Color(0xFFA6B2B3), outlineVariant = Color(0xFF384345),
    primary = Color(0xFF4FD8EB), onPrimary = Color(0xFF001F24),
    accent = Color(0xFFB4EBFF), onAccent = Color(0xFF001F24), onAccentStrong = Color(0xFF004F59),
    secondaryContainer = Color(0xFF273437), onSecondaryContainer = Color(0xFFC4E1E4),
    error = Color(0xFFFFB4AB), isDark = true
)

private val graphiteLight = SereinPalette(
    name = "石墨",
    surface = Color(0xFFF4F5F7), surfaceLow = Color(0xFFEFF0F3),
    container = Color(0xFFE9EAEE), high = Color(0xFFE3E4E9), highest = Color(0xFFDDDFE4),
    onSurface = Color(0xFF191B1F), onSurfaceVariant = Color(0xFF45474D), outlineVariant = Color(0xFFC9CBD1),
    primary = Color(0xFF3F4754), onPrimary = Color.White,
    accent = Color(0xFFDDE2EA), onAccent = Color(0xFF191C21), onAccentStrong = Color(0xFF3F4754),
    secondaryContainer = Color(0xFFDEE2EB), onSecondaryContainer = Color(0xFF41474E),
    error = Color(0xFFBA1A1A)
)

private val graphiteDark = SereinPalette(
    name = "石墨",
    surface = Color(0xFF111214), surfaceLow = Color(0xFF151619),
    container = Color(0xFF1A1C1F), high = Color(0xFF202226), highest = Color(0xFF26282C),
    onSurface = Color(0xFFE1E2E6), onSurfaceVariant = Color(0xFFA6A8AE), outlineVariant = Color(0xFF383A40),
    primary = Color(0xFFB6C2D6), onPrimary = Color(0xFF16222F),
    accent = Color(0xFFDDE2EA), onAccent = Color(0xFF191C21), onAccentStrong = Color(0xFF3F4754),
    secondaryContainer = Color(0xFF2C313A), onSecondaryContainer = Color(0xFFD5DCE8),
    error = Color(0xFFFFB4AB), isDark = true
)

/** 配色方案：8 套预设 + 自定义。顺序与设置页圆点一致。 */
val PaletteNames = listOf("柔紫", "赭红", "靛蓝", "灰绿", "蜜橙", "玫红", "青碧", "石墨", "自定义")
private val PalettesLight = listOf(
    softPurpleLight, ochreLight, indigoLight, forestMintLight,
    amberLight, roseLight, cyanLight, graphiteLight
)
private val PalettesDark = listOf(
    softPurpleDark, ochreDark, indigoDark, forestMintDark,
    amberDark, roseDark, cyanDark, graphiteDark
)
const val CUSTOM_PALETTE_INDEX = 8

/** 由任意主色程序化生成同结构的调色板（用于「自定义」配色）。 */
fun paletteFromPrimary(primary: Color, dark: Boolean): SereinPalette {
    return if (!dark) {
        SereinPalette(
            name = "自定义",
            surface = lerp(Color.White, primary, 0.03f),
            surfaceLow = lerp(Color.White, primary, 0.055f),
            container = lerp(Color.White, primary, 0.09f),
            high = lerp(Color.White, primary, 0.12f),
            highest = lerp(Color.White, primary, 0.16f),
            onSurface = lerp(Color(0xFF15181C), primary, 0.06f),
            onSurfaceVariant = lerp(Color(0xFF3C4249), primary, 0.25f),
            outlineVariant = lerp(Color.White, primary, 0.38f),
            primary = primary,
            onPrimary = Color.White,
            accent = lerp(Color.White, primary, 0.28f),
            onAccent = lerp(Color.Black, primary, 0.85f),
            onAccentStrong = lerp(primary, Color.Black, 0.28f),
            secondaryContainer = lerp(Color.White, primary, 0.22f),
            onSecondaryContainer = lerp(primary, Color.Black, 0.35f),
            error = Color(0xFFBA1A1A)
        )
    } else {
        val lightened = lerp(primary, Color.White, 0.5f)
        val base = Color(0xFF0E1013)
        SereinPalette(
            name = "自定义",
            surface = lerp(base, primary, 0.05f),
            surfaceLow = lerp(base, primary, 0.09f),
            container = lerp(base, primary, 0.13f),
            high = lerp(base, primary, 0.17f),
            highest = lerp(base, primary, 0.21f),
            onSurface = lerp(Color(0xFFE2E4E8), primary, 0.08f),
            onSurfaceVariant = lerp(Color(0xFFA9ADB5), primary, 0.3f),
            outlineVariant = lerp(base, primary, 0.42f),
            primary = lightened,
            onPrimary = lerp(Color.Black, primary, 0.85f),
            accent = lerp(primary, Color.White, 0.68f),
            onAccent = lerp(Color.Black, primary, 0.85f),
            onAccentStrong = lerp(primary, Color.Black, 0.3f),
            secondaryContainer = lerp(base, primary, 0.3f),
            onSecondaryContainer = lerp(Color.White, primary, 0.45f),
            error = Color(0xFFFFB4AB),
            isDark = true
        )
    }
}

fun paletteFor(index: Int, dark: Boolean, customPrimary: Int? = null): SereinPalette {
    if (index == CUSTOM_PALETTE_INDEX && customPrimary != null) {
        return paletteFromPrimary(Color(customPrimary), dark)
    }
    val i = index.coerceIn(0, PalettesLight.lastIndex)
    return if (dark) PalettesDark[i] else PalettesLight[i]
}

/** 预设圆点展示色（始终用浅色主色）。 */
fun paletteDotColor(index: Int): Color {
    val i = index.coerceIn(0, PalettesLight.lastIndex)
    return PalettesLight[i].primary
}

val LocalSerein = staticCompositionLocalOf { forestMintLight }

@Composable
fun SereinTheme(palette: SereinPalette, dark: Boolean, content: @Composable () -> Unit) {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primary,
        onPrimaryContainer = palette.onPrimary,
        secondary = palette.primary,
        onSecondary = palette.onPrimary,
        secondaryContainer = palette.secondaryContainer,
        onSecondaryContainer = palette.onSecondaryContainer,
        tertiary = Honey,
        onTertiary = Color.White,
        tertiaryContainer = Honey,
        onTertiaryContainer = OnHoney,
        background = palette.surface,
        onBackground = palette.onSurface,
        surface = palette.surface,
        onSurface = palette.onSurface,
        surfaceVariant = palette.highest,
        onSurfaceVariant = palette.onSurfaceVariant,
        outline = palette.outlineVariant,
        outlineVariant = palette.outlineVariant,
        surfaceContainerLowest = if (dark) palette.container else Color.White,
        surfaceContainerLow = palette.surfaceLow,
        surfaceContainer = palette.container,
        surfaceContainerHigh = palette.high,
        surfaceContainerHighest = palette.highest,
        error = palette.error,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A)
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = palette.surface.toArgb()
            window.navigationBarColor = palette.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalSerein provides palette, content = content)
    }
}

fun paletteFor(index: Int, dark: Boolean): SereinPalette {
    val i = index.coerceIn(0, PalettesLight.lastIndex)
    return if (dark) PalettesDark[i] else PalettesLight[i]
}
