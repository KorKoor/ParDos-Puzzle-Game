package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.flow.FlowMeter
import com.korkoor.pardos.domain.flow.FlowTier
import com.korkoor.pardos.domain.flow.NextLevelTeaser
import com.korkoor.pardos.domain.flow.WinStreak
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.Sage
import com.korkoor.pardos.ui.design.Terracotta
import com.korkoor.pardos.ui.design.Violet
import com.korkoor.pardos.ui.game.GameViewModel

/**
 * Aura del tablero: un halo que crece con las jugadas seguidas que fusionan (dorado → naranja → arcoíris en FLOW) y un
 * letrero que aparece al subir de escalón. Se pone encima del tablero y no recibe toques.
 */
@Composable
fun FlowAura(streak: Int, callout: GameViewModel.FlowCallout?, modifier: Modifier = Modifier) {
    val aura by animateFloatAsState(FlowMeter.aura(streak), tween(450), label = "flowAura")
    val tier = FlowMeter.tierOf(streak)
    val infinite = rememberInfiniteTransition(label = "flowPulse")
    val pulse by infinite.animateFloat(0.55f, 1f, infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val hue by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "hue")

    Box(modifier.fillMaxSize()) {
        if (aura > 0.02f && tier != FlowTier.CALM) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 2.dp.toPx() + 6.dp.toPx() * aura
                val color = when (tier) {
                    FlowTier.FLOW -> Color.hsv(hue, 0.65f, 1f)
                    FlowTier.HOT -> Terracotta
                    else -> Gold
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(w),
                    alpha = ((0.22f + 0.6f * aura) * pulse).coerceIn(0f, 1f)
                )
            }
        }

        // El último letrero se queda guardado para poder desvanecerse al irse
        var shown by remember { mutableStateOf<GameViewModel.FlowCallout?>(null) }
        if (callout != null) shown = callout
        AnimatedVisibility(
            visible = callout != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f), initialScale = 0.4f) + fadeIn(tween(120)),
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 2 }
        ) {
            val t = shown?.tier ?: FlowTier.WARM
            val c = when (t) {
                FlowTier.FLOW -> Color.hsv(hue, 0.6f, 1f)
                FlowTier.HOT -> Terracotta
                else -> Gold
            }
            Text(
                text = t.callout.orEmpty(),
                fontSize = if (t == FlowTier.FLOW) 34.sp else 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(c, c.copy(alpha = 0.78f))))
                    .padding(horizontal = 22.dp, vertical = 6.dp)
            )
        }
    }
}

/** Llama de la racha de niveles ganados: pequeña, para tenerla siempre a la vista. */
@Composable
fun FlamePill(streak: Int, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "flame")
    val s by infinite.animateFloat(0.94f, 1.08f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flameScale")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Terracotta, Gold)))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(15.dp).scale(s))
        Spacer(Modifier.width(3.dp))
        Text("$streak", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(" seguidos", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
    }
}

/** Aviso amable de la ayuda adaptativa ("Te echamos una mano…"): siempre a la vista, nunca escondida. */
@Composable
fun AssistBanner(message: String?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf("") }
    if (message != null) last = message
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(300))
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.94f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Favorite, null, tint = Terracotta, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text(last, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
        }
    }
}

/**
 * Lo que empuja a seguir en el resumen de victoria: la racha y su bono, el hito si lo hubo, el "siguiente" (con aviso de
 * jefe o regla nueva y la distancia al cofre) y la barra de la cuenta atrás del siguiente nivel automático.
 */
@Composable
fun FlowSummaryExtras(
    streak: Int,
    streakBonusPct: Int,
    flowBonusPct: Int,
    milestone: WinStreak.Milestone?,
    teaser: NextLevelTeaser.Teaser?,
    nextKindSeen: Boolean,
    autoProgress: Float,
    accent: Color
) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (streak >= 2) {
            val total = streakBonusPct + flowBonusPct
            Row(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Terracotta.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Terracotta, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Racha de $streak", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
                if (total > 0) {
                    Text("  ·  +$total% monedas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Terracotta)
                }
            }
            if (flowBonusPct > 0) {
                Text("Estado flow: +$flowBonusPct% por tus fusiones seguidas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.55f), textAlign = TextAlign.Center)
            }
        }
        milestone?.let { m ->
            Text(
                text = "¡Hito de ${m.streak} seguidos! +${m.gems} gemas" + if (m.undos > 0) " y +${m.undos} deshacer" else "",
                fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Gold).padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
        teaser?.let { t ->
            Row(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KindBadge(t.kind, 30.dp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Nivel ${t.nextLevel} · ${t.title}",
                        fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1
                    )
                    val tags = buildList {
                        if (t.isBoss) add("¡JEFE!")
                        else if (!nextKindSeen) add("¡NUEVA REGLA!")
                        if (t.levelsToChest == 0) add("Cofre del capítulo al terminar")
                        else if (t.levelsToChest <= 3) add("Cofre en ${t.levelsToChest + 1} niveles")
                    }
                    if (tags.isNotEmpty()) Text(tags.joinToString("  ·  "), fontSize = 10.sp, fontWeight = FontWeight.Black, color = accent)
                }
            }
        }
        if (autoProgress > 0f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(160.dp).height(4.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
                    Box(Modifier.fillMaxWidth(autoProgress.coerceIn(0f, 1f)).height(4.dp).background(accent, CircleShape))
                }
                Spacer(Modifier.height(3.dp))
                Text("Siguiente nivel automático · toca para pausar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.45f))
            }
        }
    }
}
