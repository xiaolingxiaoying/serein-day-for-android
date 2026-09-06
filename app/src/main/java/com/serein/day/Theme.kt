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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Acid Lime 设计系统（标注详见 docs/DESIGN.md）：
 * 高对比 Web3 / FinTech × 轻 Neo-Brutalism。
 * 浅色：Off-white 底 + 纯白卡片 + 近黑墨色；深色：纯黑底 + 炭灰卡片。
 * 唯一的高饱和变量是「荧光青柠」accent，承担 CTA、大号天数、进度等全部能量语义；
 * primary 为墨色（深色下翻转银白），只做静态强调：标题、选中态、列表大数字。
 */
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

/** 黑色大卡（Hero / 详情里程碑卡）在两套主题下共用的墨色阶梯。 */
val Ink = Color(0xFF0E1116)
val InkElevated = Color(0xFF171B21)
val OnInk = Color(0xFFF4F5F7)
val OnInkMuted = Color(0xFF9AA1AB)

/** 类别彩色底衬（图标圆 / 徽标）：低饱和马卡龙底 + 近黑图标，呼应参考稿。 */
val PastelLavender = Color(0xFFE6DFFF)
val PastelButter = Color(0xFFFFE7AD)
val PastelMint = Color(0xFFCDEFD2)
val PastelSky = Color(0xFFD3E6FF)
val PastelBlush = Color(0xFFFFDCD4)

/** 酸性青柠：全系统默认点缀色。 */
val AcidLime = Color(0xFFC8F531)
val OnAcid = Color(0xFF101405)

/** 浅色共用中性底：Off-white 阶梯。accent 需自带 onAccent / onAccentStrong。 */
private fun neonLight(name: String, accent: Color, onAccent: Color, accentStrong: Color) = SereinPalette(
    name = name,
    surface = Color(0xFFF1F2F4), surfaceLow = Color(0xFFEBEDEF),
    container = Color(0xFFFFFFFF), high = Color(0xFFF3F4F6), highest = Color(0xFFE8EAED),
    onSurface = Color(0xFF0D0F12), onSurfaceVariant = Color(0xFF767B84), outlineVariant = Color(0xFFE2E4E8),
    primary = Color(0xFF0D0F12), onPrimary = Color.White,
    accent = accent, onAccent = onAccent, onAccentStrong = accentStrong,
    secondaryContainer = Color(0xFFF0F1F3), onSecondaryContainer = Color(0xFF3A3E45),
    error = Color(0xFFE5484D)
)

/** 深色共用中性底：纯黑阶梯，primary 翻转为银白。 */
private fun neonDark(name: String, accent: Color, onAccent: Color = OnAcid) = SereinPalette(
    name = name,
    surface = Color(0xFF050506), surfaceLow = Color(0xFF0C0D0F),
    container = Color(0xFF15171B), high = Color(0xFF1C1F24), highest = Color(0xFF24272D),
    onSurface = Color(0xFFF1F2F4), onSurfaceVariant = Color(0xFF9AA0A8), outlineVariant = Color(0xFF2B2F35),
    primary = Color(0xFFF1F2F4), onPrimary = Color(0xFF0D0F12),
    accent = accent, onAccent = onAccent, onAccentStrong = accent,
    secondaryContainer = Color(0xFF22252B), onSecondaryContainer = Color(0xFFE4E6EA),
    error = Color(0xFFFF6B6E), isDark = true
)

private val limeLight = neonLight("青柠", AcidLime, OnAcid, Color(0xFF7FA30A))
private val limeDark = neonDark("青柠", Color(0xFFCDF64A))

private val greenLight = neonLight("荧光绿", Color(0xFF00E013), Color(0xFF002B05), Color(0xFF00A50E))
private val greenDark = neonDark("荧光绿", Color(0xFF2AEF3C))

private val violetLight = neonLight("电光紫", Color(0xFF8B5CF6), Color.White, Color(0xFF6D3FD8))
private val violetDark = neonDark("电光紫", Color(0xFFA78BFA), Color(0xFF1B1040))

private val blueLight = neonLight("电光蓝", Color(0xFF3D6BFF), Color.White, Color(0xFF2C4FD1))
private val blueDark = neonDark("电光蓝", Color(0xFF7C93FF), Color(0xFF0B1740))

private val orangeLight = neonLight("橙焰", Color(0xFFFF8A00), Color(0xFF2B1500), Color(0xFFD06F00))
private val orangeDark = neonDark("橙焰", Color(0xFFFFA133), Color(0xFF2B1500))

private val magentaLight = neonLight("品红", Color(0xFFF0509A), Color.White, Color(0xFFC93278))
private val magentaDark = neonDark("品红", Color(0xFFFF7CB4), Color(0xFF3D0A22))

private val tealLight = neonLight("青碧", Color(0xFF12B8A5), Color(0xFF00201C), Color(0xFF0C9080))
private val tealDark = neonDark("青碧", Color(0xFF3BDCC9))

private val redLight = neonLight("绯红", Color(0xFFFF4D45), Color.White, Color(0xFFD32820))
private val redDark = neonDark("绯红", Color(0xFFFF7B74), Color(0xFF3D0603))

/** 配色方案：8 套霓虹预设（青柠为默认）+ 自定义。顺序与设置页圆点一致。 */
val PaletteNames = listOf("青柠", "荧光绿", "电光紫", "电光蓝", "橙焰", "品红", "青碧", "绯红", "自定义")
private val PalettesLight = listOf(
    limeLight, greenLight, violetLight, blueLight,
    orangeLight, magentaLight, tealLight, redLight
)
private val PalettesDark = listOf(
    limeDark, greenDark, violetDark, blueDark,
    orangeDark, magentaDark, tealDark, redDark
)
const val CUSTOM_PALETTE_INDEX = 8

/** 由用户挑选拾的点缀色生成同结构调色板：中性底不变，仅替换点缀色。 */
fun paletteFromPrimary(accent: Color, dark: Boolean): SereinPalette {
    return if (!dark) {
        neonLight(
            "自定义",
            accent = accent,
            onAccent = if (accent.luminance() > 0.55f) OnAcid else Color.White,
            accentStrong = lerp(accent, Color.Black, 0.3f)
        )
    } else {
        neonDark(
            "自定义",
            accent = lerp(accent, Color.White, 0.15f),
            onAccent = if (lerp(accent, Color.White, 0.15f).luminance() > 0.55f) OnAcid else Color.Black
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

/** 预设圆点展示色（设置页色板圆点展示各套的点缀色）。 */
fun paletteDotColor(index: Int): Color {
    val i = index.coerceIn(0, PalettesLight.lastIndex)
    return PalettesLight[i].accent
}

val LocalSerein = staticCompositionLocalOf { limeLight }

/** 全局触感反馈开关（设置页控制），由 MainActivity 提供。 */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

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
        tertiary = palette.accent,
        onTertiary = palette.onAccent,
        tertiaryContainer = palette.accent,
        onTertiaryContainer = palette.onAccent,
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
