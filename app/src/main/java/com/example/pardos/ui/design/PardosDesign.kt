package com.korkoor.pardos.ui.design

import com.korkoor.pardos.ui.theme.GameTheme
import androidx.compose.foundation.background
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
val Navy = Color(0xFF3D405B)          // texto principal / oscuros
val Sage = Color(0xFF6B9E86)          // acción principal
val SageLight = Color(0xFF7FB69C)
val SageDark = Color(0xFF5A8C74)
val Terracotta = Color(0xFFE07A5F)    // energía: racha, tiempo
val Gold = Color(0xFFE0A93B)          // monedas, estrellas, premios
val Sand = Color(0xFFF2CC8F)
val GemBlue = Color(0xFF4E8FA6)       // gemas, información
val Violet = Color(0xFF6C63FF)        // logros, premium
val Cream = Color(0xFFFFFBF5)         // superficies cálidas
val Paper = Color(0xFFF3EFE6)         // fondo base

/** Texto secundario/terciario derivados de [Navy]. */
val InkSecondary = Navy.copy(alpha = 0.60f)
val InkTertiary = Navy.copy(alpha = 0.40f)

/** Gradiente de la acción principal. */
val PrimaryGradient: Brush get() = Brush.linearGradient(listOf(SageLight, SageDark))

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

/** Botón circular de "atrás" usado en todas las pantallas. */
@Composable
fun PardosBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = modifier.size(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Navy)
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
                Text(eyebrow.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 3.sp)
            }
            Text(title, fontSize = if (eyebrow != null) 20.sp else 20.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1)
        }
        trailing()
    }
}

/** Etiqueta de sección ("GEMAS", "RANKING"...). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = Navy.copy(alpha = 0.5f),
        letterSpacing = 3.sp
    )
}

/** Botón principal con degradado, icono opcional y rebote al pulsar. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    fontSize: TextUnit = 15.sp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "btn")
    val shape = RoundedCornerShape(Radius.Medium)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .shadow(if (enabled && !pressed) 10.dp else 2.dp, shape, spotColor = Sage)
            .clip(shape)
            .background(if (enabled) PrimaryGradient else Brush.linearGradient(listOf(Navy.copy(alpha = 0.12f), Navy.copy(alpha = 0.12f))))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = if (enabled) Color.White else Navy.copy(alpha = 0.4f),
                letterSpacing = 2.sp
            )
        }
    }
}

/** Tarjeta blanca estándar con sombra suave. */
@Composable
fun PardosCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(Radius.Large),
    color: Color = Color.White,
    elevation: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, shadowElevation = elevation, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, shadowElevation = elevation, content = content)
    }
}

/** Icono dentro de un cuadrado tintado (patrón repetido en filas y botones). */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 48.dp, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)) {
    Box(
        modifier = Modifier.size(size).background(color.copy(alpha = 0.14f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}
