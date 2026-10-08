package com.korkoor.pardos.ui.game.components

import com.korkoor.pardos.ui.design.ToyButton
import com.korkoor.pardos.ui.design.ToyTextButton
import com.korkoor.pardos.ui.design.JellySurface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.theme.GameTheme

@Composable
fun SecondChanceOverlay(
    onUseSecondChance: () -> Unit,
    onCancel: () -> Unit,
    currentTheme: GameTheme,
    reason: GameOverReason = GameOverReason.BOARD_FULL
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        JellySurface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.95f),
            border = BorderStroke(2.dp, currentTheme.accentColor.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE07A5F),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = (if (reason == GameOverReason.BOARD_FULL && !com.korkoor.pardos.ui.design.Season.halloween) stringResource(R.string.board_full) else reason.headline).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.accentColor,
                    letterSpacing = 2.sp
                )

                Text(
                    text = if (reason == GameOverReason.OUT_OF_MOVES) "¿Quieres movimientos extra?" else stringResource(R.string.second_chance_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = com.korkoor.pardos.ui.design.Navy
                )

                Text(
                    text = if (reason == GameOverReason.OUT_OF_MOVES) "Sigues con el mismo tablero y unos movimientos más para llegar a la meta."
                    else stringResource(R.string.second_chance_desc),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(Modifier.height(24.dp))

                ToyButton(
                    onClick = onUseSecondChance,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81B29A)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.continue_game_button), // ✅ "CONTINUAR JUEGO"
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                ToyTextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.retry_level_button), // ✅ "NO, REINTENTAR NIVEL"
                        color = Color.Gray
                    )
                }
            }
        }
    }
}