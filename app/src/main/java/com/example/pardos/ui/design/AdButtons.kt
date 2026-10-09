package com.korkoor.pardos.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.ui.game.logic.AdManager

/**
 * Botón de "ver un anuncio y llevarte un premio". Enseña con claridad qué se gana y que es gratis; mientras el anuncio se
 * prepara muestra una ruedita (y al tocarlo avisa si todavía no está), así nunca parece roto.
 */
@Composable
fun WatchAdButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    tag: String? = "GRATIS",
    color: Color = Sage,
    pulse: Boolean = false,
    enabled: Boolean = true,
    minHeight: Dp = 56.dp,
    /** VIP: el mismo premio sin anuncio (se enseña una corona en vez del botón de reproducir). */
    adFree: Boolean = false
) {
    val ready by AdManager.rewardedReady.collectAsState()
    ToyButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(if (pulse && enabled) Modifier.breathing(0.03f, 1100) else Modifier).heightIn(min = minHeight),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (adFree) Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, modifier = Modifier.size(26.dp), tint = Color.White)
        else if (ready) Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(26.dp), tint = Color.White)
        else CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        Spacer(Modifier.width(10.dp))
        // La columna cede espacio si el texto es largo (así la etiqueta de la derecha nunca se aplasta)
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f, fill = false)) {
            CozyText(label, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sublabel != null) CozyText(sublabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.88f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (tag != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                tag, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White, maxLines = 1, softWrap = false,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.25f)).padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
