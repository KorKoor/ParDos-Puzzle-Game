package com.korkoor.pardos.ui.game

import com.korkoor.pardos.ui.design.JellySurface
import FloatingScore
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pardos.ui.game.components.SakuraBackgroundAnimation
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.achievements.AchievementPopUp
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.ui.game.components.*
import com.korkoor.pardos.ui.game.logic.AdManager
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.profile.ProfileSetupDialog
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.ui.theme.ThemeSelector
import com.korkoor.pardos.ui.theme.ThemeViewModel
import com.korkoor.pardos.data.local.ProfileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BouncingText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    fontWeight: FontWeight
) {
    var previousText by remember { mutableStateOf(text) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(text) {
        if (text != previousText) {
            previousText = text
            scale.animateTo(1.2f, animationSpec = tween(100))
            scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f))
        }
    }

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = Modifier.scale(scale.value)
    )
}

@Composable
fun PowerUpSection(
    viewModel: GameViewModel,
    haptic: HapticFeedback,
    activity: Activity?,
    modifier: Modifier = Modifier,
    labelColor: Color = com.korkoor.pardos.ui.design.Navy
) {
    val currentTime by viewModel.currentTimeProvider.collectAsState()
    val vipContext = androidx.compose.ui.platform.LocalContext.current
    val isVip by remember { com.korkoor.pardos.data.local.EconomyManager(vipContext).isVip }.collectAsState()

    PowerUpBar(
        viewModel = viewModel,
        modifier = modifier,
        labelColor = labelColor,
        activity = activity,
        onCleanClick = {
            if (viewModel.isPowerUpAvailable(viewModel.lastCleanTime, currentTime)) {
                viewModel.useCleanPowerUp()
            } else if (isVip) {
                viewModel.grantAdReward("CLEAN")
            } else {
                activity?.let { act ->
                    AdManager.showRewardedAd(act) {
                        viewModel.grantAdReward("CLEAN")
                    }
                }
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onMergeClick = {
            if (viewModel.isPowerUpAvailable(viewModel.lastMergeTime, currentTime)) {
                viewModel.useMergePowerUp()
            } else if (isVip) {
                viewModel.grantAdReward("MERGE")
            } else {
                activity?.let { act ->
                    AdManager.showRewardedAd(act) {
                        viewModel.grantAdReward("MERGE")
                    }
                }
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )
}

@Composable
fun AdLoadingOverlay(currentTheme: GameTheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = currentTheme.accentColor)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.preparing_revive),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GameOverOverlay(
    onRestart: () -> Unit,
    currentTheme: GameTheme,
    isRace: Boolean = false,
    stagesCleared: Int = 0,
    coinsEarned: Int = 0,
    reason: GameOverReason = GameOverReason.BOARD_FULL,
    /** "¡Te faltó una fusión!": lo cerca que estuviste (null = nada que decir). */
    nearMiss: String? = null,
    /** Línea extra bajo el "casi" (por ejemplo los corazones que quedan en la torre). */
    footnote: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        JellySurface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .padding(16.dp),
            color = com.korkoor.pardos.ui.design.Cream,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRace) stringResource(R.string.race_over_title)
                    else if (reason == GameOverReason.BOARD_FULL && !com.korkoor.pardos.ui.design.Season.halloween) stringResource(R.string.board_full) else reason.headline,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = com.korkoor.pardos.ui.design.Navy,
                    textAlign = TextAlign.Center
                )

                if (isRace) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "$stagesCleared",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = com.korkoor.pardos.ui.design.Gold
                    )
                    Text(
                        text = stringResource(R.string.race_stages_cleared).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.5f),
                        letterSpacing = 3.sp
                    )
                    if (coinsEarned > 0) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(com.korkoor.pardos.ui.design.Gold.copy(alpha = 0.16f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = com.korkoor.pardos.ui.design.Gold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("+$coinsEarned", fontSize = 16.sp, fontWeight = FontWeight.Black, color = com.korkoor.pardos.ui.design.Navy)
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (reason == GameOverReason.BOARD_FULL) stringResource(R.string.no_moves) else reason.body,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.6f)
                    )
                }

                if (nearMiss != null && !isRace) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = nearMiss,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = com.korkoor.pardos.ui.design.Terracotta,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(com.korkoor.pardos.ui.design.Terracotta.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                if (footnote != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(footnote, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(24.dp))
                com.korkoor.pardos.ui.design.PrimaryButton(
                    text = stringResource(R.string.retry),
                    onClick = onRestart,
                    icon = androidx.compose.material.icons.Icons.Default.Refresh
                )
            }
        }
    }
}

/** Cómo se ve un combo según su tamaño: más alto = más grande, más colorido y con fuegos artificiales. */
private data class ComboLook(val label: String, val colors: List<Color>, val size: Int, val fireworks: Boolean)

private fun comboLook(count: Int, accent: Color): ComboLook = when {
    count >= 8 -> ComboLook("¡LEYENDA!", listOf(Color(0xFFFFF0A8), Color(0xFFFF9A5B), Color(0xFFC04CFF)), 76, true)
    count >= 6 -> ComboLook("¡ÉPICO!", listOf(Color(0xFFD7B8FF), Color(0xFF6C63FF)), 68, true)
    count >= 4 -> ComboLook("¡INCREÍBLE!", listOf(Color(0xFFFFC08F), Color(0xFFE07A5F)), 62, false)
    count >= 3 -> ComboLook("¡GENIAL!", listOf(Color(0xFFFFE9A0), Color(0xFFE0A93B)), 58, false)
    else -> ComboLook("COMBO", listOf(accent, accent), 52, false)
}

@Composable
fun ComboIndicator(count: Int, accentColor: Color) {
    // Durante la animación de salida `count` ya vale 0: mostramos el último combo real para que no aparezca "×0"
    var shown by remember { mutableIntStateOf(2) }
    if (count > 1) shown = count
    val look = comboLook(shown, accentColor)
    // Cada subida del combo "golpea": el número aparece grande y se asienta con rebote
    val punch = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(count) {
        if (count > 1) {
            punch.snapTo(1.45f)
            punch.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f))
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = count > 1,
        enter = scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn() + expandIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Estallido de estrellitas en cada subida (fuegos artificiales a partir de x6)
            com.korkoor.pardos.ui.game.components.MergeBurst(
                if (look.fireworks) com.korkoor.pardos.domain.shop.MergeFx.FIREWORKS else com.korkoor.pardos.domain.shop.MergeFx.STARS,
                count, 128, 230.dp
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    rotationZ = -5f
                    scaleX = punch.value
                    scaleY = punch.value
                }
            ) {
                val shadow = Shadow(Color.Black.copy(alpha = 0.5f), offset = Offset(4f, 6.dp.value), blurRadius = 12f)
                Text(
                    text = look.label,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp,
                        brush = Brush.horizontalGradient(look.colors), shadow = shadow
                    )
                )
                Text(
                    text = stringResource(R.string.combo_multiplier, shown),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = look.size.sp, fontWeight = FontWeight.Black,
                        brush = Brush.verticalGradient(look.colors), shadow = shadow
                    )
                )
            }
        }
    }
}

@Composable
internal fun AchievementManagerPopup(viewModel: GameViewModel) {
    androidx.compose.animation.AnimatedVisibility(
        visible = viewModel.activeAchievementPopup != null,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp),
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        viewModel.activeAchievementPopup?.let { achievement ->
            AchievementPopUp(achievement = achievement)
        }
    }
}


/** Entrega del teléfono (tras el jugador 1) y resultado final del duelo local. */
@Composable
fun DuelOverlay(
    phase: DuelPhase,
    scores: List<Int>,
    onNext: () -> Unit,
    onRematch: () -> Unit,
    onExit: () -> Unit
) {
    val navy = com.korkoor.pardos.ui.design.Navy
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        JellySurface(
            modifier = Modifier.fillMaxWidth(0.86f).padding(16.dp),
            color = com.korkoor.pardos.ui.design.Cream,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (phase == DuelPhase.HANDOVER) {
                    Text(stringResource(R.string.duel_pass_title), fontSize = 24.sp, fontWeight = FontWeight.Black, color = navy)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.duel_score_of, 1, scores[0]),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.korkoor.pardos.ui.design.Sage
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.duel_pass_desc, 2),
                        fontSize = 14.sp, textAlign = TextAlign.Center, color = navy.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(24.dp))
                    com.korkoor.pardos.ui.design.PrimaryButton(
                        text = stringResource(R.string.duel_ready),
                        onClick = onNext,
                        icon = androidx.compose.material.icons.Icons.Default.PlayArrow
                    )
                } else {
                    val winner = com.korkoor.pardos.domain.logic.DuelRules.winner(scores[0], scores[1])
                    val margin = com.korkoor.pardos.domain.logic.DuelRules.margin(scores[0], scores[1])
                    Text(
                        text = when (winner) {
                            com.korkoor.pardos.domain.logic.DuelWinner.PLAYER_1 -> stringResource(R.string.duel_winner, 1)
                            com.korkoor.pardos.domain.logic.DuelWinner.PLAYER_2 -> stringResource(R.string.duel_winner, 2)
                            com.korkoor.pardos.domain.logic.DuelWinner.TIE -> stringResource(R.string.duel_tie)
                        },
                        fontSize = 24.sp, fontWeight = FontWeight.Black, color = navy, textAlign = TextAlign.Center
                    )
                    if (winner != com.korkoor.pardos.domain.logic.DuelWinner.TIE) {
                        Text(
                            stringResource(R.string.duel_margin, margin),
                            fontSize = 13.sp, color = navy.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(1, 2).forEach { p ->
                            val won = (p == 1 && winner == com.korkoor.pardos.domain.logic.DuelWinner.PLAYER_1) ||
                                (p == 2 && winner == com.korkoor.pardos.domain.logic.DuelWinner.PLAYER_2)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (won) com.korkoor.pardos.ui.design.Gold.copy(alpha = 0.18f) else navy.copy(alpha = 0.05f))
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    stringResource(R.string.player_label, p),
                                    fontSize = 10.sp, fontWeight = FontWeight.Black, color = navy.copy(alpha = 0.5f), letterSpacing = 1.sp
                                )
                                Text("${scores[p - 1]}", fontSize = 30.sp, fontWeight = FontWeight.Black, color = navy)
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    com.korkoor.pardos.ui.design.PrimaryButton(
                        text = stringResource(R.string.duel_rematch),
                        onClick = onRematch,
                        icon = androidx.compose.material.icons.Icons.Default.Refresh
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.duel_exit),
                        modifier = Modifier.clickable(onClick = onExit).padding(10.dp),
                        fontSize = 13.sp, fontWeight = FontWeight.Black, color = navy.copy(alpha = 0.5f), letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
