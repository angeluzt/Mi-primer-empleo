package com.angeluzt.miprimerempleo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Azul = Color(0xFF1D4ED8)
val AzulClaro = Color(0xFFEFF4FF)
val AzulOscuro = Color(0xFF15307A)
val Ambar = Color(0xFFD97706)
val AmbarClaro = Color(0xFFFEF3C7)
val Verde = Color(0xFF047857)
val VerdeClaro = Color(0xFFDCFCE7)
val Rojo = Color(0xFFDC2626)
val RojoClaro = Color(0xFFFEE2E2)
val Tinta = Color(0xFF0F172A)
val Gris = Color(0xFF64748B)
val GrisClaro = Color(0xFFE2E8F0)
val Fondo = Color(0xFFF8FAFC)

private val colorClaro = lightColorScheme(
    primary = Azul,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = AzulOscuro,
    secondary = Ambar,
    onSecondary = Color.White,
    secondaryContainer = AmbarClaro,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Verde,
    tertiaryContainer = VerdeClaro,
    error = Rojo,
    errorContainer = RojoClaro,
    background = Fondo,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Gris,
    outline = GrisClaro,
)

private val colorOscuro = darkColorScheme(
    primary = Color(0xFF93B4FF),
    onPrimary = Color(0xFF0A1F52),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBE6FF),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3B2600),
    secondaryContainer = Color(0xFF6B4A05),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF6EE7B7),
    tertiaryContainer = Color(0xFF064E3B),
    error = Color(0xFFFCA5A5),
    errorContainer = Color(0xFF7F1D1D),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF131C31),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1C2742),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
)

private val tipografia = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MiPrimerEmpleoTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) colorOscuro else colorClaro,
        typography = tipografia,
        content = contenido,
    )
}
