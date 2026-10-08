package com.korkoor.pardos.ui.design

import com.korkoor.pardos.ui.theme.GameTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================================
//  PARDOS DESIGN SYSTEM
//  Una sola fuente de verdad para color, forma y componentes base. Las pantallas
//  nuevas deben usar esto en lugar de declarar sus propios colores privados.
// =====================================================================================

// ---- Paleta de identidad (los colores del juego) ----
// En Noche de brujas (ver `Season`) la paleta se viste de Halloween: tinta de berenjena, acción en naranja calabaza,
// la "energía" en morado y los fondos con un tinte lila. El resto del año son los colores de siempre.
val Navy: Color get() = if (Season.halloween) Color(0xFF33204D) else Color(0xFF3D405B)   // texto principal / oscuros
val Sage: Color get() = if (Season.halloween) Color(0xFFE8772E) else Color(0xFF6B9E86)   // acción principal
val SageLight: Color get() = if (Season.halloween) Color(0xFFF59A57) else Color(0xFF7FB69C)
val SageDark: Color get() = if (Season.halloween) Color(0xFFC85F1B) else Color(0xFF5A8C74)
val Terracotta: Color get() = if (Season.halloween) Color(0xFF9B4FC9) else Color(0xFFE07A5F)  // energía: racha, tiempo
val Gold = Color(0xFFE0A93B)          // monedas, estrellas, premios
val Sand = Color(0xFFF2CC8F)
val GemBlue = Color(0xFF4E8FA6)       // gemas, información
val Violet = Color(0xFF6C63FF)        // logros, premium
val Cream: Color get() = if (Season.halloween) Color(0xFFFFF7EE) else Color(0xFFFFFBF5)   // superficies cálidas
val Paper: Color get() = if (Season.halloween) Color(0xFFEFE6F6) else Color(0xFFF3EFE6)   // fondo base

/** Texto secundario/terciario derivados de [Navy]. */
val InkSecondary: Color get() = Navy.copy(alpha = 0.60f)
val InkTertiary: Color get() = Navy.copy(alpha = 0.40f)

/** Gradiente de la acción principal. */
val PrimaryGradient: Brush get() = Brush.linearGradient(listOf(SageLight, SageDark))

/**
 * Versión "de botón" de un pastel: mismo tono, más saturada y algo más oscura (en lugar de apagarlo con negro, que lo dejaba lodoso).
 * Con ella un pastel rosa da un rosa franco y uno verde un verde profundo, siempre legibles con texto blanco.
 */
fun Color.actionTone(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.argb(255, (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()), hsv)
    hsv[1] = (hsv[1] * 1.7f + 0.10f).coerceIn(0.38f, 0.85f)
    hsv[2] = (hsv[2] * 0.80f).coerceIn(0.52f, 0.78f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Un tono más oscuro de [this] (para degradados de tarjetas). */
fun Color.darker(factor: Float = 0.8f) = copy(red = red * factor, green = green * factor, blue = blue * factor)

fun Color.gradient(): Brush = Brush.linearGradient(listOf(this, this.darker()))

/**
 * Color de ACCIÓN del tema: botones, selección y énfasis. El `accentColor` de cada tema es un
 * pastel pensado para fondos y no da contraste con texto blanco; este es su versión legible.
 */
val GameTheme.actionColor: Color
    get() = when (this) {
        GameTheme.Zen -> Sage
        GameTheme.Forest -> Color(0xFFC76B76)   // Sakura
        GameTheme.Sunset -> GemBlue             // Cloud
        GameTheme.Cyber -> Color(0xFF7A74E0)    // Lavanda
        GameTheme.Midnight -> Color(0xFF4F9A63) // Matcha
        is GameTheme.Skinned -> accentColor.actionTone() // legible con texto blanco y con color vivo
    }

// ---- Formas ----
object Radius {
    val Small = 14.dp
    val Medium = 20.dp
    val Large = 26.dp
    val XLarge = 32.dp
}

// ---- Fondo base de pantalla ----
val ScreenBackground: Brush get() = Brush.verticalGradient(listOf(Paper, Cream))

// =====================================================================================
//  COMPONENTES BASE
// =====================================================================================

/** Botón circular de "atrás" usado en todas las pantallas: pieza de juguete que se hunde al pulsar. */
@Composable
fun PardosBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    JellyCard(
        onClick = onClick,
        shape = CircleShape,
        lipHeight = 4.dp,
        modifier = modifier.size(50.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(bottom = 0.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Navy, modifier = Modifier.size(24.dp))
        }
    }
}

/** Barra superior estándar: atrás + etiqueta pequeña + título + contenido a la derecha. */
@Composable
fun PardosTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    horizontalPadding: Dp = 20.dp,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PardosBackButton(onBack)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).rotate(45f).background(Terracotta, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(7.dp))
                    Text(eyebrow.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Terracotta.copy(alpha = 0.85f), letterSpacing = 2.6.sp)
                }
            }
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1, letterSpacing = (-0.3).sp, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** Etiqueta de sección: un pequeño sello girado + el título + tres puntitos de cola. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).rotate(45f).background(Brush.linearGradient(listOf(Terracotta, Gold)), RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(9.dp))
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Navy.copy(alpha = 0.62f),
            letterSpacing = 2.6.sp
        )
        Spacer(Modifier.width(8.dp))
        repeat(3) { k ->
            Box(Modifier.padding(horizontal = 1.5.dp).size((4 - k).dp).background(Navy.copy(alpha = 0.18f - k * 0.04f), CircleShape))
        }
    }
}

/**
 * Botón principal "de juguete": degradado, brillo en la parte alta, labio oscuro debajo y se hunde al pulsar.
 * [color] permite usarlo con otras familias de color (terracota, dorado...).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    fontSize: TextUnit = 15.sp,
    color: Color = Sage
) {
    val base = if (enabled) color else Navy.copy(alpha = 0.12f).compositeOver(Color.White)
    val shape = RoundedCornerShape(Radius.Medium)
    JellyCard(
        modifier = modifier.fillMaxWidth().height(height),
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        fill = base,
        lip = if (enabled) base.deepen(0.38f) else Navy.copy(alpha = 0.10f),
        lipHeight = 5.dp,
        brush = if (enabled) base.toyGradient() else null
    ) {
        if (enabled) {
            // brillo de plástico en la parte alta
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 4.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth().height(height * 0.17f)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            )
        }
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = if (enabled) Color.White else Navy.copy(alpha = 0.4f),
                letterSpacing = 1.6.sp
            )
        }
    }
}

/** Tarjeta estándar: pieza de juguete con labio (ver [JellyCard]). [elevation] se conserva por compatibilidad. */
@Composable
fun PardosCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(Radius.Large),
    color: Color = Color.White,
    @Suppress("UNUSED_PARAMETER") elevation: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    JellyCard(modifier = modifier, onClick = onClick, shape = shape, fill = color) { content() }
}

/** Icono dentro de una baldosa tintada, con degradado, borde y brillo (patrón de filas y botones). */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 48.dp, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0.11f))))
            .border(1.dp, color.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = size * 0.07f).fillMaxWidth(0.55f).height(size * 0.08f).background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(50)))
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}
