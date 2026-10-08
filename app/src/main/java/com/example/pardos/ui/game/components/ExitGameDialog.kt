package com.korkoor.pardos.ui.game.components

import com.korkoor.pardos.ui.design.ToyButton
import com.korkoor.pardos.ui.design.ToyTextButton
import com.korkoor.pardos.ui.design.ToyAlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.R
@Composable
fun ExitGameDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    currentTheme: GameTheme
) {
    ToyAlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = stringResource(R.string.exit_dialog_title), // ✅ "¿Deseas salir?"
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = com.korkoor.pardos.ui.design.Navy,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.exit_dialog_message), // ✅ "Tu progreso se perderá..."
                fontSize = 14.sp,
                color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            ToyButton(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentTheme.accentColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.exit_dialog_confirm), // ✅ "SÍ, SALIR"
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            ToyTextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.exit_dialog_dismiss), // ✅ "CONTINUAR JUGANDO"
                    color = currentTheme.accentColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    )
}