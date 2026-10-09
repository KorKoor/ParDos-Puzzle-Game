package com.korkoor.pardos.ui.game.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import com.korkoor.pardos.domain.shop.ContinueOffer
import com.korkoor.pardos.ui.design.CozyText
import com.korkoor.pardos.ui.design.GemBlue
import com.korkoor.pardos.ui.design.Icon
import com.korkoor.pardos.ui.design.JellySurface
import com.korkoor.pardos.ui.design.ToyButton
import com.korkoor.pardos.ui.design.ToyTextButton
import com.korkoor.pardos.ui.design.WatchAdButton
import com.korkoor.pardos.ui.design.breathing
import com.korkoor.pardos.ui.design.popIn
import com.korkoor.pardos.ui.theme.GameTheme

/**
 * Al perder: seguir jugando es lo que más se agradece, y se puede de tres formas honestas: viendo un anuncio (gratis),
 * pagando gemas, o —si el jugador no tiene gemas— comprando un pack pequeño sin salir de la partida. VIP continúa sin anuncio.
 */
@Composable
fun SecondChanceOverlay(
    onUseSecondChance: () -> Unit,
    onCancel: () -> Unit,
    currentTheme: GameTheme,
    reason: GameOverReason = GameOverReason.BOARD_FULL,
    gems: Int = 0,
    vip: Boolean = false,
    onPayGems: () -> Unit = {},
    /** Precio con formato local del pack pequeño de gemas (null si la tienda aún no lo ha dado). */
    gemPackPrice: String? = null,
    gemPackGems: Int = 0,
    onBuyGems: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        JellySurface(
            modifier = Modifier.fillMaxWidth(0.88f).popIn(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.97f),
            border = BorderStroke(2.dp, currentTheme.accentColor.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE07A5F),
                    modifier = Modifier.size(60.dp).breathing(0.08f, 700)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = (if (reason == GameOverReason.BOARD_FULL && !com.korkoor.pardos.ui.design.Season.halloween) stringResource(R.string.board_full) else reason.headline).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.accentColor,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
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
                    modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
                )

                // 1) La vía gratis: anuncio (VIP continúa directamente)
                if (vip) {
                    ToyButton(
                        onClick = onUseSecondChance,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81B29A)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(stringResource(R.string.continue_game_button), fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("VIP", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                } else {
                    WatchAdButton(
                        label = stringResource(R.string.continue_game_button),
                        sublabel = "Mira un anuncio corto",
                        onClick = onUseSecondChance,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF81B29A),
                        pulse = true
                    )
                }

                // 2) La vía rápida: gemas (si no le alcanzan, se le ofrece completar sin salir de la partida)
                Spacer(Modifier.height(10.dp))
                if (ContinueOffer.canPayWithGems(gems)) {
                    ToyButton(
                        onClick = onPayGems,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GemBlue),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Sin anuncio", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Spacer(Modifier.width(10.dp))
                        CozyText("${ContinueOffer.GEMS} ◆", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                } else if (gemPackPrice != null && gemPackGems > 0) {
                    ToyButton(
                        onClick = onBuyGems,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GemBlue),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CozyText("Sin anuncio · ${ContinueOffer.GEMS} ◆", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                            CozyText("Te faltan ${ContinueOffer.missing(gems)} ◆ · $gemPackGems ◆ por $gemPackPrice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                        }
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
