package com.mp34dl.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mode tema aplikasi yang dapat diganti dari Settings.
 */
enum class AppThemeMode {
    MATERIAL,
    LIQUID_GLASS
}

/**
 * Token visual tambahan untuk mode LIQUID_GLASS.
 */
@Immutable
data class LiquidGlassTokens(
    val surfaceAlpha: Float = 0.18f,
    val borderAlpha: Float = 0.35f,
    val borderWidth: Dp = 1.dp,
    val cornerRadius: Dp = 24.dp,
    val blurRadius: Dp = 20.dp,
    val springSpec: androidx.compose.animation.core.SpringSpec<Float> =
        spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
)

val LocalLiquidGlassTokens = staticCompositionLocalOf { LiquidGlassTokens() }

/**
 * Palet warna dasar untuk mode MATERIAL (Material 3).
 */
private val MaterialLightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    secondary = Color(0xFF43A047),
    onSecondary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E)
)

private val MaterialDarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE3E3E3)
)

/**
 * Palet warna untuk mode LIQUID_GLASS (translucent glass + living flow).
 */
private val LiquidGlassLightColors = lightColorScheme(
    primary = Color(0xFF5B8DEF),
    onPrimary = Color.White,
    secondary = Color(0xFF7BD389),
    onSecondary = Color.White,
    background = Color(0xFFEAF1FF),
    onBackground = Color(0xFF10131A),
    surface = Color(0x2EFFFFFF),
    onSurface = Color(0xFF10131A)
)

private val LiquidGlassDarkColors = darkColorScheme(
    primary = Color(0xFF7AA8FF),
    onPrimary = Color(0xFF0B1A3A),
    secondary = Color(0xFF8BE39A),
    onSecondary = Color(0xFF0F2A14),
    background = Color(0xFF0A1020),
    onBackground = Color(0xFFE6ECFF),
    surface = Color(0x26FFFFFF),
    onSurface = Color(0xFFE6ECFF)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

private val AppTypography = Typography()

/**
 * Root theme composable. Bungkus seluruh UI dengan ini.
 *
 * @param mode Mode tema yang dipilih dari Settings.
 * @param darkTheme Apakah sistem sedang dark mode.
 * @param content Konten aplikasi.
 */
@Composable
fun AppTheme(
    mode: AppThemeMode = AppThemeMode.MATERIAL,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (mode) {
        AppThemeMode.MATERIAL -> if (darkTheme) MaterialDarkColors else MaterialLightColors
        AppThemeMode.LIQUID_GLASS -> if (darkTheme) LiquidGlassDarkColors else LiquidGlassLightColors
    }

    val glassTokens = if (mode == AppThemeMode.LIQUID_GLASS) {
        LiquidGlassTokens()
    } else {
        LiquidGlassTokens(surfaceAlpha = 1f, borderAlpha = 0f, borderWidth = 0.dp, blurRadius = 0.dp)
    }

    CompositionLocalProvider(LocalLiquidGlassTokens provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * Surface bergaya glass untuk mode LIQUID_GLASS.
 * Pada mode MATERIAL berperilaku seperti Surface biasa.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val tokens = LocalLiquidGlassTokens.current
    val isGlass = tokens.surfaceAlpha < 1f

    if (isGlass) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = tokens.surfaceAlpha),
            border = BorderStroke(
                width = tokens.borderWidth,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = tokens.borderAlpha)
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = content
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            content = content
        )
    }
}

/**
 * Brush gradien halus untuk latar "living flow" pada mode glass.
 * Gunakan dengan Modifier.background(...) pada container root.
 */
@Composable
fun livingFlowBrush(): Brush {
    val tokens = LocalLiquidGlassTokens.current
    val isGlass = tokens.surfaceAlpha < 1f
    return if (isGlass) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    } else {
        Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
    }
}
