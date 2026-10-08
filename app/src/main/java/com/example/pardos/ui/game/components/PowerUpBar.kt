package com.korkoor.pardos.ui.game.components

import com.korkoor.pardos.ui.design.JellySurface
import com.korkoor.pardos.ui.design.Sage
import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.WorkspacePremium
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.design.PowerGlyph
import com.korkoor.pardos.ui.design.PowerGlyphIcon
import com.korkoor.pardos.ui.game.GameViewModel
import com.korkoor.pardos.ui.game.logic.AdManager

@Composable
fun PowerUpBar(
    onCleanClick: () -> Unit,
    onMergeClick: () -> Unit,
    viewModel: GameViewModel,
    activity: Activity?,
    modifier: Modifier = Modifier,
    labelColor: Color = com.korkoor.pardos.ui.design.Navy
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isVip by remember { com.korkoor.pardos.data.local.EconomyManager(context).isVip }.collectAsState()
    // VIP: los poderes que piden anuncio se activan directo
    fun withAd(action: () -> Unit) {
        if (isVip) action() else activity?.let { act -> AdManager.showRewardedAd(act) { action() } }
    }
    val undos by viewModel.undoCount.collectAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top
    ) {
        // Deshacer (consumible comprable en la tienda): solo si hay una jugada que revertir
        if (undos > 0 || viewModel.canUndo) {
            UndoButton(count = undos, enabled = viewModel.canUndo && undos > 0, labelColor = labelColor) { viewModel.undoLastMove() }
        }
        PowerUpButton(
            label = stringResource(R.string.clean_powerup),
            glyph = PowerGlyph.WAND,
            color = Sage,
            lastUseTime = viewModel.lastCleanTime,
            viewModel = viewModel,
            labelColor = labelColor,
            onClick = onCleanClick
        )
        PowerUpButton(
            label = stringResource(R.string.merge_powerup),
            glyph = PowerGlyph.MERGE,
            color = Color(0xFFE0A93B),
            lastUseTime = viewModel.lastMergeTime,
            viewModel = viewModel,
            labelColor = labelColor,
            onClick = onMergeClick
        )
        PowerUpButton(
            label = stringResource(R.string.powerup_clean_manual),
            glyph = PowerGlyph.BROOM,
            color = Color(0xFFE07A5F),
            lastUseTime = 0L,
            viewModel = viewModel,
            forceAdMode = true,
            vip = isVip,
            labelColor = labelColor,
            onClick = { withAd { viewModel.activateSelectMode("SINGLE_CLEAN") } }
        )
        PowerUpButton(
            label = stringResource(R.string.powerup_merge_manual),
            glyph = PowerGlyph.LINK,
            color = Color(0xFF6C63FF),
            lastUseTime = 0L,
            viewModel = viewModel,
            forceAdMode = true,
            vip = isVip,
            labelColor = labelColor,
            onClick = { withAd { viewModel.activateSelectMode("MANUAL_MERGE") } }
        )
    }
}

/**
 * Botón de poder. Estados:
 *  - listo: orbe luminoso que "respira", con su icono animado
 *  - recarga: orbe apagado con un anillo que se vacía y la cuenta atrás
 *  - anuncio (forceAdMode): insignia de "play" (o corona si eres VIP)
 */
@Composable
private fun PowerUpButton(
    label: String,
    glyph: PowerGlyph,
    color: Color,
    lastUseTime: Long,
    viewModel: GameViewModel,
    forceAdMode: Boolean = false,
    vip: Boolean = false,
    labelColor: Color,
    onClick: () -> Unit
) {
    val currentTime by viewModel.currentTimeProvider.collectAsState()
    val isAvailable = !forceAdMode && viewModel.isPowerUpAvailable(lastUseTime, currentTime)
    val isCooldown = !forceAdMode && !isAvailable
    val remainingText = if (isCooldown) viewModel.getRemainingTime(lastUseTime, currentTime) else ""
    val remainingFraction = if (isCooldown) viewModel.cooldownFraction(lastUseTime, currentTime) else 0f

    PowerOrb(
        label = label, glyph = glyph, color = color, ready = !isCooldown,
        centerText = if (isCooldown) remainingText else null,
        ringFraction = if (isCooldown) remainingFraction else null,
        badge = if (forceAdMode) (if (vip) Badge.CROWN else Badge.PLAY) else null,
        labelColor = labelColor, onClick = onClick
    )
}

@Composable
private fun UndoButton(count: Int, enabled: Boolean, labelColor: Color, onClick: () -> Unit) {
    PowerOrb(
        label = "DESHACER", glyph = PowerGlyph.UNDO, color = Color(0xFFE07A5F), ready = enabled,
        countBadge = count, labelColor = labelColor, onClick = onClick, enabled = enabled, labelSize = 8
    )
}

private enum class Badge { PLAY, CROWN }

@Composable
private fun PowerOrb(
    label: String,
    glyph: PowerGlyph,
    color: Color,
    ready: Boolean,
    labelColor: Color,
    onClick: () -> Unit,
    centerText: String? = null,
    ringFraction: Float? = null,
    badge: Badge? = null,
    countBadge: Int? = null,
    enabled: Boolean = true,
    labelSize: Int = 10
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "PowerOrbPress"
    )
    // Respiración: solo cuando el poder está listo
    val breathe by rememberInfiniteTransition(label = "orb").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orbBreath"
    )
    val glow = if (ready && enabled) breathe else 0f
    val shape = RoundedCornerShape(22.dp)
    val tint = if (ready && enabled) color else Color(0xFF9A9AA8)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp).scale(pressScale)) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                color = Color.Transparent,
                shape = shape,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = if (ready && enabled) (6f + 8f * glow).dp else 1.dp, shape = shape,
                        spotColor = tint, ambientColor = tint
                    )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White, tint.copy(alpha = if (ready && enabled) 0.20f else 0.10f).compositeOverWhite()),
                                start = Offset(0f, 0f), end = Offset(160f, 220f)
                            )
                        )
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), endY = 60f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Borde luminoso en degradado
                    Canvas(Modifier.matchParentSize()) {
                        val sw = 2.2.dp.toPx()
                        drawRoundRect(
                            Brush.linearGradient(listOf(tint.copy(alpha = 0.95f), tint.copy(alpha = 0.25f), tint.copy(alpha = 0.7f))),
                            topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()), style = Stroke(sw)
                        )
                        // Anillo de recarga: se vacía hasta que el poder vuelve
                        if (ringFraction != null) {
                            val inset = 5.dp.toPx()
                            drawArc(
                                color = color.copy(alpha = 0.75f), startAngle = -90f, sweepAngle = 360f * ringFraction.coerceIn(0f, 1f),
                                useCenter = false, topLeft = Offset(inset, inset),
                                size = Size(size.width - inset * 2, size.height - inset * 2),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    PowerGlyphIcon(
                        glyph = glyph, color = color, enabled = ready && enabled,
                        modifier = Modifier.size(if (centerText != null) 34.dp else 46.dp).then(if (centerText != null) Modifier.padding(bottom = 6.dp) else Modifier),
                        animate = ready && enabled
                    )
                    if (centerText != null) {
                        Text(
                            text = centerText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                        )
                    }
                }
            }
            // Insignias en la esquina
            when {
                badge == Badge.PLAY -> CornerBadge(color) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.ad_label), tint = Color.White, modifier = Modifier.size(13.dp))
                }
                badge == Badge.CROWN -> CornerBadge(Color(0xFFE0A93B)) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
                countBadge != null -> CornerBadge(color) {
                    Text("$countBadge", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
        Text(
            text = label,
            fontSize = labelSize.sp,
            fontWeight = FontWeight.Black,
            color = labelColor.copy(alpha = if (ready && enabled) 0.88f else 0.5f),
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun BoxScope.CornerBadge(color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-4).dp)
            .size(20.dp)
            .shadow(3.dp, CircleShape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.9f), color)), CircleShape)
            .then(Modifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Mezcla un color translúcido sobre blanco (para degradados limpios sin transparencias raras). */
private fun Color.compositeOverWhite(): Color {
    val a = alpha
    return Color(red * a + (1f - a), green * a + (1f - a), blue * a + (1f - a), 1f)
}
