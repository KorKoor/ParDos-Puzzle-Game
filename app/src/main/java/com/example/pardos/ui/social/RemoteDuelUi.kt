package com.korkoor.pardos.ui.social

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.logic.RemoteChallenge
import com.korkoor.pardos.domain.logic.RemoteDuel
import com.korkoor.pardos.domain.logic.RemoteOutcome
import com.korkoor.pardos.domain.social.ShareText
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.RemoteRole
import kotlinx.coroutines.delay

/** Abre el selector del sistema con el reto listo para pegar en WhatsApp, Telegram, etc. */
fun shareChallenge(context: Context, challenge: RemoteChallenge) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, RemoteDuel.shareText(challenge, ShareText.STORE_URL))
    }
    context.startActivity(Intent.createChooser(send, "Lanzar reto"))
}

/**
 * Pantalla al terminar un duelo a distancia.
 *  - CREATOR: tu reto está listo → comparte el código.
 *  - CHALLENGED: comparación contigo vs quien te retó, premios y revancha.
 */
@Composable
fun RemoteDuelOverlay(
    role: RemoteRole,
    myScore: Int,
    challenge: RemoteChallenge?,
    outcome: RemoteOutcome?,
    reward: RemoteDuel.Reward?,
    firstTime: Boolean,
    onShare: () -> Unit,
    onRetryBoard: () -> Unit,
    onChallengeBack: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }

    val creator = role == RemoteRole.CREATOR
    val accent = when {
        creator -> Terracotta
        outcome == RemoteOutcome.WIN -> Sage
        outcome == RemoteOutcome.TIE -> Gold
        else -> GemBlue
    }
    val title = when {
        creator -> "¡Tu reto está listo!"
        outcome == RemoteOutcome.WIN -> "¡Ganaste!"
        outcome == RemoteOutcome.TIE -> "¡Empate!"
        else -> "Casi…"
    }
    val opponent = challenge?.let { RemoteDuel.displayName(it.name) } ?: ""

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).padding(12.dp),
            color = Cream, shape = RoundedCornerShape(32.dp), shadowElevation = 16.dp
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(64.dp).background(Brush.linearGradient(listOf(accent, accent.darker(0.8f))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (creator) Icons.Rounded.Send else if (outcome == RemoteOutcome.WIN) Icons.Rounded.EmojiEvents else Icons.Rounded.SportsMma,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)

                if (creator) {
                    Text("Hiciste $myScore puntos en 60 s", fontSize = 13.sp, color = InkSecondary)
                    Spacer(Modifier.height(16.dp))
                    challenge?.let { ch ->
                        val code = RemoteDuel.encode(ch)
                        // el código, tocable para copiar
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Navy.copy(alpha = 0.06f))
                                .clickable { clipboard.setText(AnnotatedString(code)); copied = true }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("CÓDIGO DEL RETO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(code, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(4.dp))
                            Text(if (copied) "¡Copiado!" else "Toca para copiar", fontSize = 10.sp, color = if (copied) Sage else InkTertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                    reward?.takeIf { it.coins > 0 }?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("+${it.coins} monedas por retar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold)
                    }
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton("Compartir reto", onClick = onShare, icon = Icons.Rounded.Share)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Intentarlo de nuevo", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 1.sp,
                        modifier = Modifier.clickable(onClick = onRetryBoard).padding(10.dp)
                    )
                } else {
                    val margin = kotlin.math.abs(myScore - (challenge?.score ?: 0))
                    if (outcome != RemoteOutcome.TIE) Text("por $margin puntos", fontSize = 13.sp, color = InkSecondary)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScoreColumn("TÚ", myScore, outcome == RemoteOutcome.WIN, Modifier.weight(1f))
                        ScoreColumn(opponent.uppercase(), challenge?.score ?: 0, outcome == RemoteOutcome.LOSE, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    when {
                        !firstTime -> Text("Ya cobraste este reto", fontSize = 12.sp, color = InkTertiary)
                        reward != null && (reward.coins > 0 || reward.gems > 0) -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (reward.coins > 0) RewardPill(Icons.Rounded.MonetizationOn, "+${reward.coins}", Gold)
                            if (reward.gems > 0) RewardPill(Icons.Rounded.Diamond, "+${reward.gems}", GemBlue)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton("Retar de vuelta", onClick = onChallengeBack, icon = Icons.Rounded.Bolt)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Intentarlo otra vez", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 1.sp,
                        modifier = Modifier.clickable(onClick = onRetryBoard).padding(8.dp)
                    )
                }
                Text(
                    "SALIR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.45f), letterSpacing = 2.sp,
                    modifier = Modifier.clickable(onClick = onExit).padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreColumn(label: String, score: Int, highlight: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp))
            .background(if (highlight) Gold.copy(alpha = 0.2f) else Navy.copy(alpha = 0.05f)).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 1.sp, maxLines = 1)
        Text("$score", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}

@Composable
internal fun RewardPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}
