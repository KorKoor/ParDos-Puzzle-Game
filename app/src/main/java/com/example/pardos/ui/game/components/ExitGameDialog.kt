package com.korkoor.pardos.ui.game.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.design.InkSecondary
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.rewards.CardDialog
import com.korkoor.pardos.ui.rewards.DialogButton
import com.korkoor.pardos.ui.theme.GameTheme

/**
 * Salir de un nivel. Dos botones claros y siempre visibles: seguir jugando (el principal, a color) y salir al menú (secundario, en rojo
 * terroso). Antes el botón de salir salía blanco sobre blanco y solo se veía "continuar".
 */
@Composable
fun ExitGameDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") currentTheme: GameTheme
) {
    CardDialog(onDismiss) {
        Text(
            text = stringResource(R.string.exit_dialog_title),
            fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.exit_dialog_message),
            fontSize = 14.sp, lineHeight = 20.sp, color = InkSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        DialogButton(stringResource(R.string.exit_dialog_dismiss), Color(0xFF4F8F72), onClick = onDismiss)
        Spacer(Modifier.height(10.dp))
        DialogButton(stringResource(R.string.exit_dialog_confirm), Color(0xFFB4413C), onClick = onConfirm)
    }
}
