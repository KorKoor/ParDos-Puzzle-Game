package com.korkoor.pardos.ui.game.components

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.game.logic.AdManager
import com.korkoor.pardos.ui.game.GameViewModel

private val LabelColor = Color(0xFF3D405B)

@Composable
fun PowerUpBar(
    onCleanClick: () -> Unit,
    onMergeClick: () -> Unit,
    viewModel: GameViewModel,
    activity: Activity?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isVip by remember { com.korkoor.pardos.data.local.EconomyManager(context).isVip }.collectAsState()
    // VIP: los poderes que piden anuncio se activan directo
    fun withAd(action: () -> Unit) {
        if (isVip) action() else activity?.let { act -> AdManager.showRewardedAd(act) { action() } }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top
    ) {
        PowerUpButton(
            label = stringResource(R.string.clean_powerup),
            icon = Icons.Default.AutoFixHigh,
            color = Color(0xFF81B29A),
            lastUseTime = viewModel.lastCleanTime,
            viewModel = viewModel,
            onClick = onCleanClick
        )
        PowerUpButton(
            label = stringResource(R.string.merge_powerup),
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFFE0A93B),
            lastUseTime = viewModel.lastMergeTime,
            viewModel = viewModel,
            onClick = onMergeClick
        )
        PowerUpButton(
            label = stringResource(R.string.powerup_clean_manual),
            icon = Icons.Default.AutoFixNormal,
            color = Color(0xFFE07A5F),
            lastUseTime = 0L,
            viewModel = viewModel,
            forceAdMode = true,
            onClick = { withAd { viewModel.activateSelectMode("SINGLE_CLEAN") } }
        )
        PowerUpButton(
            label = stringResource(R.string.powerup_merge_manual),
            icon = Icons.Default.AllInclusive,
            color = Color(0xFF6C63FF),
            lastUseTime = 0L,
            viewModel = viewModel,
            forceAdMode = true,
            onClick = { withAd { viewModel.activateSelectMode("MANUAL_MERGE") } }
        )
    }
}

/**
 * Botón de poder. Tres estados:
 *  - disponible: tarjeta blanca con el icono a color
 *  - recarga: tarjeta atenuada con la cuenta atrás
 *  - anuncio (forceAdMode): tarjeta con una insignia de "play" en la esquina
 */
@Composable
private fun PowerUpButton(
    label: String,
    icon: ImageVector,
    color: Color,
    lastUseTime: Long,
    viewModel: GameViewModel,
    forceAdMode: Boolean = false,
    onClick: () -> Unit
) {
    val currentTime by viewModel.currentTimeProvider.collectAsState()
    val isAvailable = !forceAdMode && viewModel.isPowerUpAvailable(lastUseTime, currentTime)
    val isCooldown = !forceAdMode && !isAvailable
    val remainingText = if (isCooldown) viewModel.getRemainingTime(lastUseTime, currentTime) else ""

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "PowerUpScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).scale(scale)
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            color = if (isCooldown) Color(0xFFEDEBE6) else Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = if (isCooldown) 0.dp else if (isPressed) 2.dp else 6.dp,
            border = BorderStroke(1.5.dp, color.copy(alpha = if (isCooldown) 0.15f else 0.35f)),
            modifier = Modifier.size(62.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCooldown) {
                    Text(
                        text = remainingText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = LabelColor.copy(alpha = 0.55f)
                    )
                } else {
                    Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
                }
                if (forceAdMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(18.dp)
                            .background(color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.ad_label), tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = LabelColor.copy(alpha = if (isCooldown) 0.4f else 0.75f),
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
            letterSpacing = 0.5.sp
        )
    }
}
