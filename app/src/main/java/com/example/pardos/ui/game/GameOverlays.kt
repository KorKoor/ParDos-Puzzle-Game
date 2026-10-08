package com.korkoor.pardos.ui.game

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
    modifier: Modifier = Modifier
) {
    val currentTime by viewModel.currentTimeProvider.collectAsState()

    PowerUpBar(
        viewModel = viewModel,
        modifier = modifier,
        activity = activity,
        onCleanClick = {
            if (viewModel.isPowerUpAvailable(viewModel.lastCleanTime, currentTime)) {
                viewModel.useCleanPowerUp()
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
    currentTheme: GameTheme
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            color = Color(0xFFF5F0E6),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.board_full),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF5D4037)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.no_moves),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF5D4037).copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8D6E63)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.retry), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ComboIndicator(count: Int, accentColor: Color) {
    androidx.compose.animation.AnimatedVisibility(
        visible = count > 1,
        enter = scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn() + expandIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Text(
            text = stringResource(R.string.combo_multiplier, count),
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                shadow = Shadow(
                    Color.Black.copy(alpha = 0.5f),
                    offset = Offset(4f, 6.dp.value),
                    blurRadius = 12f
                )
            ),
            modifier = Modifier.graphicsLayer {
                rotationZ = -5f
            }
        )
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
