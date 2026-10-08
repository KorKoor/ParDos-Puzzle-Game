package com.korkoor.pardos.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.theme.GameTheme

private val Navy = Color(0xFF3D405B)
private const val TOTAL_LEVELS = 2400

@Composable
fun ModeSelectionScreen(
    onModeSelected: (GameMode) -> Unit,
    onBack: () -> Unit,
    currentTheme: GameTheme
) {
    val context = LocalContext.current
    val campaignLevel = remember { ProfileManager(context).getProfile().currentCampaignLevel }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(currentTheme.colors))) {
        PicnicBackgroundOptimized(color = currentTheme.accentColor.copy(alpha = 0.05f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // --- Cabecera ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(onClick = onBack, shape = CircleShape, color = Color.White, shadowElevation = 6.dp, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = Navy)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.mode_selection_subtitle),
                        fontSize = 11.sp, fontWeight = FontWeight.Black,
                        color = Navy.copy(alpha = 0.5f), letterSpacing = 3.sp
                    )
                    Text(stringResource(R.string.mode_selection_title), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Campaña: tarjeta principal con tu progreso ---
                CampaignHero(
                    level = campaignLevel,
                    onClick = { onModeSelected(GameMode.CLASICO) }
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "OTROS MODOS",
                    fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = Navy.copy(alpha = 0.45f), letterSpacing = 3.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                ModeRow(
                    title = stringResource(R.string.mode_tables_title),
                    description = stringResource(R.string.mode_tables_desc),
                    icon = Icons.Rounded.Calculate,
                    color = Color(0xFF4E8FA6),
                    onClick = { onModeSelected(GameMode.TABLAS) }
                )
                ModeRow(
                    title = stringResource(R.string.mode_challenge_title),
                    description = stringResource(R.string.mode_challenge_desc),
                    icon = Icons.Rounded.Timer,
                    color = Color(0xFFE07A5F),
                    onClick = { onModeSelected(GameMode.DESAFIO) }
                )
                ModeRow(
                    title = stringResource(R.string.mode_zen_title),
                    description = stringResource(R.string.mode_zen_desc),
                    icon = Icons.Rounded.SelfImprovement,
                    color = Color(0xFFE0A93B),
                    onClick = { onModeSelected(GameMode.ZEN) }
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.mode_footer_hint),
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = Navy.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CampaignHero(level: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val progress = (level.toFloat() / TOTAL_LEVELS).coerceIn(0f, 1f)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, spotColor = Color(0xFF6B9E86))
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF7FB69C), Color(0xFF5A8C74))))
                .padding(22.dp)
        ) {
            // Fichas decorativas
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-14).dp)
                    .size(90.dp)
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp))
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(50.dp).background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.mode_campaign_title),
                            fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp
                        )
                        Text(
                            stringResource(R.string.menu_play_subtitle, level),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.mode_campaign_desc),
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceAtLeast(0.02f))
                            .background(Color.White, CircleShape)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "$level / $TOTAL_LEVELS",
                    fontSize = 10.sp, fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.75f), letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun ModeRow(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(54.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.sp)
                Spacer(Modifier.height(3.dp))
                Text(description, fontSize = 12.sp, color = Navy.copy(alpha = 0.55f), lineHeight = 16.sp, maxLines = 3)
            }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
    }
}
