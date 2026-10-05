package com.angeluzt.miprimerempleo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angeluzt.miprimerempleo.R

/*
 * Identidad visual "Evidencia".
 *
 * Índigo como color de marca: serio sin ser el azul de banco que usan todas las bolsas de
 * trabajo. Naranja quemado para lo que da calor y empuja (el CV, los logros). Verde solo para
 * lo terminado. Neutros con un tinte violeta muy leve para que la marca se sienta en toda la app.
 *
 * Cada par de texto y fondo se revisó contra WCAG AA (4.5:1 o más). La excepción conocida es
 * el texto secundario sobre el degradado de marca, que por eso va al 90 % de opacidad y no menos.
 */

// Marca
val Indigo = Color(0xFF4338CA)
val IndigoClaro = Color(0xFFE0E7FF)
val IndigoProfundo = Color(0xFF1E1B4B)
val Violeta = Color(0xFF6D28D9)
val Naranja = Color(0xFFC2410C)
val NaranjaClaro = Color(0xFFFFEDD5)
val NaranjaProfundo = Color(0xFF7C2D12)
val Esmeralda = Color(0xFF047857)
val EsmeraldaClara = Color(0xFFD1FAE5)
val EsmeraldaProfunda = Color(0xFF064E3B)
val Rojo = Color(0xFFB42318)
val RojoClaro = Color(0xFFFEE4E2)

// Neutros
val Tinta = Color(0xFF17151F)
val TintaSuave = Color(0xFF5B5770)
val Fondo = Color(0xFFF7F7FA)
val Borde = Color(0xFFE4E2EE)

/** El degradado de marca: tarjeta de nivel, encabezados que celebran algo. */
val DegradadoMarca = Brush.linearGradient(listOf(Indigo, Violeta))

/** Degradado cálido para el CV: es la herramienta, no la lectura. */
val DegradadoCalido = Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFFC2410C)))

/** Texto secundario sobre un degradado de marca: 90 % para no bajar de 4.5:1. */
val SobreMarcaSuave = Color.White.copy(alpha = 0.9f)

private val colorClaro = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoClaro,
    onPrimaryContainer = IndigoProfundo,
    secondary = Naranja,
    onSecondary = Color.White,
    secondaryContainer = NaranjaClaro,
    onSecondaryContainer = NaranjaProfundo,
    tertiary = Esmeralda,
    onTertiary = Color.White,
    tertiaryContainer = EsmeraldaClara,
    onTertiaryContainer = EsmeraldaProfunda,
    error = Rojo,
    onError = Color.White,
    errorContainer = RojoClaro,
    onErrorContainer = Color(0xFF55160C),
    background = Fondo,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Color(0xFFF0EFF6),
    onSurfaceVariant = TintaSuave,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBFD),
    surfaceContainer = Color(0xFFF3F2F8),
    surfaceContainerHigh = Color(0xFFEDECF4),
    surfaceContainerHighest = Color(0xFFE7E5EF),
    outline = Borde,
    outlineVariant = Color(0xFFCFCCDD),
    inverseSurface = Color(0xFF2B2838),
    inverseOnSurface = Color(0xFFF2F0F8),
    inversePrimary = Color(0xFFB4BCFF),
)

private val colorOscuro = darkColorScheme(
    primary = Color(0xFFB4BCFF),
    onPrimary = Color(0xFF1A1660),
    primaryContainer = Color(0xFF2E2A7A),
    onPrimaryContainer = Color(0xFFE3E5FF),
    secondary = Color(0xFFFFB088),
    onSecondary = Color(0xFF4A1A06),
    secondaryContainer = Color(0xFF5A240C),
    onSecondaryContainer = Color(0xFFFFDCCB),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF003826),
    tertiaryContainer = Color(0xFF0B4A37),
    onTertiaryContainer = Color(0xFFC6F6E1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF55160C),
    errorContainer = Color(0xFF5C1A14),
    onErrorContainer = Color(0xFFFFDAD5),
    background = Color(0xFF0F0E17),
    onBackground = Color(0xFFECEAF5),
    surface = Color(0xFF18162A),
    onSurface = Color(0xFFECEAF5),
    surfaceVariant = Color(0xFF232038),
    onSurfaceVariant = Color(0xFFABA6C4),
    surfaceContainerLowest = Color(0xFF0B0A12),
    surfaceContainerLow = Color(0xFF151324),
    surfaceContainer = Color(0xFF1C1A2E),
    surfaceContainerHigh = Color(0xFF242138),
    surfaceContainerHighest = Color(0xFF2D2A43),
    outline = Color(0xFF34304D),
    outlineVariant = Color(0xFF4A4566),
    inverseSurface = Color(0xFFECEAF5),
    inverseOnSurface = Color(0xFF2B2838),
    inversePrimary = Indigo,
)

/** Plus Jakarta Sans, en cinco pesos estáticos: los variables no respetan el peso en Android 7. */
val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

private fun estilo(tamano: Int, alto: Int, peso: FontWeight, espaciado: Float = 0f) = TextStyle(
    fontFamily = Jakarta,
    fontSize = tamano.sp,
    lineHeight = alto.sp,
    fontWeight = peso,
    letterSpacing = espaciado.sp,
)

private val tipografia = Typography(
    displayLarge = estilo(40, 46, FontWeight.ExtraBold, -1f),
    displayMedium = estilo(34, 40, FontWeight.ExtraBold, -0.8f),
    displaySmall = estilo(30, 36, FontWeight.ExtraBold, -0.6f),
    headlineLarge = estilo(28, 34, FontWeight.ExtraBold, -0.4f),
    headlineMedium = estilo(25, 31, FontWeight.ExtraBold, -0.3f),
    headlineSmall = estilo(21, 27, FontWeight.Bold, -0.2f),
    titleLarge = estilo(19, 25, FontWeight.Bold, -0.1f),
    titleMedium = estilo(16, 22, FontWeight.Bold),
    titleSmall = estilo(14, 20, FontWeight.SemiBold),
    bodyLarge = estilo(17, 27, FontWeight.Normal),
    bodyMedium = estilo(15, 22, FontWeight.Normal),
    bodySmall = estilo(13, 18, FontWeight.Normal),
    labelLarge = estilo(14, 20, FontWeight.SemiBold),
    labelMedium = estilo(12, 16, FontWeight.SemiBold, 0.1f),
    labelSmall = estilo(11, 15, FontWeight.Bold, 0.4f),
)

private val formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MiPrimerEmpleoTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) colorOscuro else colorClaro,
        typography = tipografia,
        shapes = formas,
        content = contenido,
    )
}
