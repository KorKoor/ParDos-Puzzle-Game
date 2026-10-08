package com.korkoor.pardos.ui.rewards

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val slices = DailyWheel.slices
    val sliceAngle = 360f / slices.size

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
            prize = slices[idx]
            spinning = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = "Cada día", title = "Ruleta", onBack = onBack) {
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
                    // Marco
                    Box(Modifier.size(wheelSize + 20.dp).shadow(14.dp, CircleShape, spotColor = Gold).background(Navy, CircleShape))

                    Box(
                        modifier = Modifier.size(wheelSize).graphicsLayer { rotationZ = rotation.value },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val d = size.minDimension
                            slices.forEachIndexed { i, s ->
                                drawArc(
                                    color = sliceColor(i, s),
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
                                Icon(sliceIcon(s), null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Text(s.label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    // Centro
                    Box(Modifier.size(54.dp).shadow(6.dp, CircleShape).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(28.dp))
                    }
                    // Puntero fijo
                    Canvas(Modifier.size(wheelSize + 24.dp)) {
                        val cx = size.width / 2
                        val path = Path().apply {
                            moveTo(cx - 16f, 0f); lineTo(cx + 16f, 0f); lineTo(cx, 44f); close()
                        }
                        drawPath(path, Terracotta)
                        drawPath(path, Color.White, style = Stroke(4f))
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ---- Resultado ----
                prize?.let { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Gold.copy(alpha = 0.16f)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(sliceIcon(p), null, tint = Gold, modifier = Modifier.size(28.dp))
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
                Spacer(Modifier.height(10.dp))
                Text(
                    "Premios: monedas, gemas, cofres y puntos del pase de temporada.",
                    fontSize = 11.sp, color = InkTertiary, textAlign = TextAlign.Center
                )
            }
        }
    }
}
