package com.korkoor.pardos.ui.rewards

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.ui.game.logic.AdManager
import com.korkoor.pardos.ui.design.*

/** "¡Perdiste tu racha!": ofrece recuperarla hoy con gemas o con un anuncio. */
@Composable
fun StreakRepairDialog(lostStreak: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val profile = remember { ProfileManager(context) }
    val economy = remember { EconomyManager(context) }
    val gems by economy.gems.collectAsState()
    val cost = remember { profile.streakRepairGemCost() }
    val adAvailable = remember { profile.streakRepairAdAvailable() }

    CardDialog(onDismiss = { profile.declineStreakRepair(); onDone() }) {
        IconTile(Icons.Rounded.LocalFireDepartment, Terracotta, size = 72.dp, shape = androidx.compose.foundation.shape.CircleShape)
        Spacer(Modifier.height(12.dp))
        Text("¡Perdiste tu racha de $lostStreak días!", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
        Text(
            "Todavía puedes recuperarla, pero solo hoy.",
            fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        DialogButton("Recuperar por $cost gemas", GemBlue, enabled = gems >= cost) {
            if (profile.repairStreak(withGems = true)) onDone()
        }
        if (adAvailable) {
            Spacer(Modifier.height(8.dp))
            val vip = com.korkoor.pardos.data.local.EconomyManager(androidx.compose.ui.platform.LocalContext.current).isVip.value
            DialogButton(if (vip) "Recuperar gratis (VIP)" else "Recuperar viendo un anuncio", Violet) {
                if (vip) { if (profile.repairStreak(withGems = false)) onDone() }
                else activity?.let { act -> AdManager.showRewardedAd(act) { if (profile.repairStreak(withGems = false)) onDone() } }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Empezar de nuevo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary,
            modifier = Modifier.clickable { profile.declineStreakRepair(); onDone() }.padding(8.dp)
        )
    }
}
