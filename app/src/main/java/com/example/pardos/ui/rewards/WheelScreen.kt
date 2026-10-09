package com.korkoor.pardos.ui.rewards

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.retention.DailyWheel
import com.korkoor.pardos.domain.retention.WheelKind
import com.korkoor.pardos.domain.retention.WheelSlice
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.logic.AdManager
import com.korkoor.pardos.ui.menu.CurrencyPill
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private fun sliceColor(i: Int, s: WheelSlice): Color = when (s.kind) {
    WheelKind.COINS -> if (i % 2 == 0) Gold else Gold.darker(0.88f)
    WheelKind.GEMS -> GemBlue
    WheelKind.CHEST -> if (s.chest == ChestType.RARE) Violet else Terracotta
    WheelKind.SEASON_POINTS -> Sage
}

private fun sliceKind(s: WheelSlice): CozyKind = when (s.kind) {
    WheelKind.COINS -> CozyKind.COIN
    WheelKind.GEMS -> CozyKind.GEM
    WheelKind.CHEST -> CozyKind.CHEST
    WheelKind.SEASON_POINTS -> CozyKind.CROWN
}

private fun sliceIcon(s: WheelSlice): ImageVector = when (s.kind) {
    WheelKind.COINS -> Icons.Rounded.MonetizationOn
    WheelKind.GEMS -> Icons.Rounded.Diamond
    WheelKind.CHEST -> Icons.Rounded.Inventory2
    WheelKind.SEASON_POINTS -> Icons.Rounded.WorkspacePremium
}

/** Ruleta diaria: un giro gratis cada día y giros extra viendo un anuncio. */
@Composable
fun WheelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val retention = remember { RetentionManager(context) }
    val economy = remember { EconomyManager(context) }
    val coins by economy.coins.collectAsState()
    val gems by economy.gems.collectAsState()
    val isVip by economy.isVip.collectAsState()
    val tick by retention.tick.collectAsState()
    val allowance = remember(tick) { retention.wheelAllowance() }

    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var prize by remember { mutableStateOf<WheelSlice?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var prizeCount by remember { mutableIntStateOf(0) }
    val slices = DailyWheel.slices
    val sliceAngle = 360f / slices.size

    // Un "clic" de vibración cada vez que el puntero cruza una casilla
    LaunchedEffect(spinning) {
        if (spinning) {
            var last = (rotation.value / (360f / slices.size)).toInt()
            snapshotFlow { (rotation.value / (360f / slices.size)).toInt() }.collect { n ->
                if (n != last) {
                    last = n
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.WHEEL_TICK, 1f, 0.92f + 0.16f * ((n % 5) / 4f))
                }
            }
        }
    }

    val blink by androidx.compose.animation.core.rememberInfiniteTransition(label = "bulbs").animateFloat(
        0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(1600, easing = androidx.compose.animation.core.LinearEasing)), label = "blink"
    )

    fun startSpin(viaAd: Boolean) {
        if (spinning) return
        val idx = retention.spinWheel(viaAd) ?: return
        spinning = true
        prize = null
        scope.launch {
            // La casilla [idx] debe quedar bajo el puntero (arriba). Centro de la casilla, medido en sentido horario desde arriba:
            val center = idx * sliceAngle + sliceAngle / 2f + (Random.nextFloat() - 0.5f) * sliceAngle * 0.6f
            val need = (((-center) % 360f) + 360f) % 360f
            val now = rotation.value
            var target = now - (now % 360f) + need
            while (target < now + 360f * 5) target += 360f
            rotation.animateTo(target, tween(4800, easing = CubicBezierEasing(0.12f, 0.62f, 0.08f, 1f)))
            retention.applyWheelPrize(slices[idx])
            com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.WHEEL_STOP)
            when (slices[idx].kind) {
                com.korkoor.pardos.domain.retention.WheelKind.CHEST, com.korkoor.pardos.domain.retention.WheelKind.GEMS -> com.korkoor.pardos.audio.GameAudio.playLater(com.korkoor.pardos.audio.Sfx.WHEEL_WIN, 220)
                else -> com.korkoor.pardos.audio.GameAudio.playLater(com.korkoor.pardos.audio.Sfx.COINS, 220)
            }
            prize = slices[idx]
            prizeCount++
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            spinning = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().pardosBackdrop()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = com.korkoor.pardos.ui.design.Season.text("Cada día", "¿Truco o trato?"), title = "Ruleta", onBack = onBack) {
                CurrencyPill(Icons.Rounded.MonetizationOn, coins, Gold)
                Spacer(Modifier.width(8.dp))
                CurrencyPill(Icons.Rounded.Diamond, gems, GemBlue)
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))

                // ---- Rueda ----
                val wheelSize = 300.dp
                Box(modifier = Modifier.size(wheelSize + 24.dp), contentAlignment = Alignment.Center) {
                    // Marco: aro oscuro con borde dorado y bombillas que corren mientras gira
                    Box(
                        Modifier.size(wheelSize + 22.dp).shadow(14.dp, CircleShape, spotColor = Gold)
                            .background(Brush.verticalGradient(listOf(Color(0xFF55597A), Navy, Color(0xFF2B2D45))), CircleShape)
                    )
                    Canvas(Modifier.size(wheelSize + 22.dp)) {
                        val c = Offset(size.width / 2, size.height / 2)
                        val rr = size.minDimension / 2 - 8.dp.toPx()
                        val n = 20
                        repeat(n) { i ->
                            val a = Math.toRadians((i * 360.0 / n) - 90.0)
                            val pos = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
                            val speed = if (spinning) 3 else 1
                            val on = ((blink * n * speed).toInt() + i) % 2 == 0
                            drawCircle(Gold.copy(alpha = if (on) 0.35f else 0.10f), 7.dp.toPx(), pos)
                            drawCircle(if (on) Color(0xFFFFE08A) else Color(0xFF8A7A52), 3.6.dp.toPx(), pos)
                        }
                    }

                    Box(
                        modifier = Modifier.size(wheelSize).graphicsLayer { rotationZ = rotation.value },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val d = size.minDimension
                            slices.forEachIndexed { i, s ->
                                val base = sliceColor(i, s)
                                drawArc(
                                    brush = Brush.radialGradient(listOf(base.deepen(0.10f), base, base.lighten(0.16f)), Offset(d / 2, d / 2), d / 2),
                                    startAngle = -90f + i * sliceAngle, sweepAngle = sliceAngle, useCenter = true,
                                    topLeft = Offset.Zero, size = Size(d, d)
                                )
                            }
                            // Separadores
                            slices.indices.forEach { i ->
                                val a = Math.toRadians((-90f + i * sliceAngle).toDouble())
                                drawLine(
                                    Color.White.copy(alpha = 0.5f), Offset(d / 2, d / 2),
                                    Offset(d / 2 + (d / 2 * cos(a)).toFloat(), d / 2 + (d / 2 * sin(a)).toFloat()), strokeWidth = 3f
                                )
                            }
                            drawCircle(Color.White.copy(alpha = 0.35f), radius = d / 2 - 3f, style = Stroke(6f))
                        }
                        slices.forEachIndexed { i, s ->
                            val theta = i * sliceAngle + sliceAngle / 2f
                            val rad = Math.toRadians(theta.toDouble())
                            val r = wheelSize.value * 0.34f
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = (r * sin(rad)).dp, y = (-r * cos(rad)).dp)
                                    .graphicsLayer { rotationZ = theta },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CozyIcon(sliceKind(s), Modifier.size(36.dp))
                                Text(
                                    s.label, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center,
                                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 4f))
                                )
                            }
                        }
                    }
                    // Centro
                    JellyCard(Modifier.size(64.dp), shape = CircleShape, lipHeight = 5.dp) {
                        CozyIcon(CozyKind.STAR, Modifier.align(Alignment.Center).size(38.dp).breathing(0.07f, 1100))
                    }
                    com.korkoor.pardos.ui.game.components.MergeBurst(
                        com.korkoor.pardos.domain.shop.MergeFx.FIREWORKS, prizeCount, 4096, wheelSize, Modifier.zIndex(5f)
                    )
                    // Puntero fijo
                    Canvas(Modifier.size(wheelSize + 24.dp)) {
                        val cx = size.width / 2
                        val path = Path().apply {
                            moveTo(cx - 22f, 0f); lineTo(cx + 22f, 0f); lineTo(cx, 62f); close()
                        }
                        drawPath(path.also { }, Color.Black.copy(alpha = 0.25f), style = Stroke(10f))
                        drawPath(path, Brush.verticalGradient(listOf(Color(0xFFF08A70), Color(0xFFC95C42))))
                        drawPath(path, Color.White, style = Stroke(4f))
                        drawCircle(Color.White.copy(alpha = 0.7f), 4f, Offset(cx, 10f))
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ---- Resultado ----
                prize?.let { p ->
                    JellyRow(
                        modifier = Modifier.fillMaxWidth().popIn(),
                        shape = RoundedCornerShape(20.dp), fill = Gold.lighten(0.84f), lip = Gold.copy(alpha = 0.5f),
                        padding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CozyIcon(sliceKind(p), Modifier.size(34.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "¡Ganaste " + when (p.kind) {
                                WheelKind.COINS -> "${p.amount} monedas"
                                WheelKind.GEMS -> "${p.amount} gemas"
                                WheelKind.CHEST -> if (p.chest == ChestType.RARE) "un cofre raro" else "un cofre"
                                WheelKind.SEASON_POINTS -> "${p.amount} puntos de pase"
                            } + "!",
                            fontSize = 16.sp, fontWeight = FontWeight.Black, color = Navy
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // ---- Botones ----
                when {
                    allowance.freeLeft > 0 -> PrimaryButton("Girar gratis", onClick = { startSpin(false) }, icon = Icons.Rounded.Casino, enabled = !spinning)
                    allowance.adLeft > 0 -> PrimaryButton(
                        if (isVip) "Girar de nuevo (VIP)" else "Girar con anuncio (${allowance.adLeft})",
                        onClick = {
                            if (isVip) startSpin(true)
                            else activity?.let { act -> AdManager.showRewardedAd(act) { startSpin(true) } }
                        },
                        icon = Icons.Rounded.PlayCircle, enabled = !spinning
                    )
                    else -> Text(
                        "Ya usaste todos tus giros de hoy. ¡Mañana tienes otro gratis!",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkSecondary, textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(18.dp))
                JellyColumn(modifier = Modifier.fillMaxWidth().staggerIn(1), padding = PaddingValues(16.dp)) {
                    SectionLabel("Qué puedes ganar")
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(CozyKind.COIN to "Monedas", CozyKind.GEM to "Gemas", CozyKind.CHEST to "Cofres", CozyKind.CROWN to "Pase").forEach { (k, label) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CozyIcon(k, Modifier.size(42.dp))
                                Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 1.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Giros de hoy: ${allowance.freeLeft} gratis · ${allowance.adLeft} con anuncio",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
