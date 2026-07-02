package com.hege.dingpay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DingSecondary,
    secondary = DingPrimarySoft,
    tertiary = DingAccentSoft,
    background = DingInk,
    surface = ColorTokens.darkSurface,
    onPrimary = DingInk,
    onSecondary = DingInk,
    onTertiary = DingInk,
    onBackground = ColorTokens.darkOnSurface,
    onSurface = ColorTokens.darkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = DingPrimary,
    secondary = DingSecondary,
    tertiary = DingAccent,
    background = DingBackground,
    surface = DingSurface,
    surfaceVariant = DingPrimarySoft,
    outline = DingLine,
    onPrimary = DingSurface,
    onSecondary = DingInk,
    onTertiary = DingSurface,
    onBackground = DingInk,
    onSurface = DingInk

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private object ColorTokens {
    val darkSurface = androidx.compose.ui.graphics.Color(0xFF17302E)
    val darkOnSurface = androidx.compose.ui.graphics.Color(0xFFEAF3EF)
}

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
)

@Composable
fun DingPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
