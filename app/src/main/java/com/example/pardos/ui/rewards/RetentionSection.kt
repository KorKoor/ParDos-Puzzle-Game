package com.korkoor.pardos.ui.rewards

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.retention.LeagueResult
import com.korkoor.pardos.domain.retention.LevelReward

private enum class HubDialog { CHEST, PIGGY }

/**
 * Bloque de retención del menú: la franja "Tu día" y todos los diálogos que salen de ella
 * (cofre gratis, hucha) más los avisos al entrar (regalo de regreso, premios por nivel).
 * [suppress] retiene los avisos mientras haya otros diálogos de bienvenida encima.
 */
@Composable
fun RetentionSection(
    suppress: Boolean,
    piggyPrice: String?,
    onSeason: () -> Unit,
    onWheel: () -> Unit,
    onAlbum: () -> Unit,
    onBuyPiggy: () -> Unit,
    modifier: Modifier = Modifier,
    onProfileChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val retention = remember { RetentionManager(context) }
    var dialog by remember { mutableStateOf<HubDialog?>(null) }
    var checkIn by remember { mutableStateOf<RetentionManager.CheckIn?>(null) }
    var levelRewards by remember { mutableStateOf<List<LevelReward>>(emptyList()) }
    var leagueResult by remember { mutableStateOf<LeagueResult?>(null) }
    var repairStreak by remember { mutableStateOf<Int?>(null) }
    var unlockedSkins by remember { mutableStateOf<List<com.korkoor.pardos.domain.shop.TileSkin>>(emptyList()) }

    // Si el jugador llegó tocando un aviso de cofre u hucha, abre ese diálogo
    LaunchedEffect(Unit) {
        when (com.korkoor.pardos.notifications.NotificationRoute.pendingDialog) {
            "chest" -> dialog = HubDialog.CHEST
            "piggy" -> dialog = HubDialog.PIGGY
        }
        com.korkoor.pardos.notifications.NotificationRoute.pendingDialog = null
    }

    // Una vez por apertura del menú (ambas operaciones son idempotentes por día / por nivel)
    LaunchedEffect(Unit) {
        val ci = retention.checkIn()
        if (ci.comeback != null) checkIn = ci
        levelRewards = retention.collectLevelRewards(ProfileManager(context).getProfile().playerLevel)
        leagueResult = retention.pendingLeagueResult()
        repairStreak = ProfileManager(context).pendingStreakRepair()
        retention.checkHiddenSkins()
        unlockedSkins = retention.takePendingReveals()
    }

    Column(modifier = modifier) {
        TodayStrip(
            retention = retention,
            onChest = { dialog = HubDialog.CHEST },
            onWheel = onWheel,
            onSeason = onSeason,
            onPiggy = { dialog = HubDialog.PIGGY }
        )
    }

    if (!suppress) {
        val ci = checkIn
        when {
            repairStreak != null -> StreakRepairDialog(repairStreak!!) { repairStreak = null; onProfileChanged() }
            ci?.comeback != null -> ComebackDialog(ci.comeback, ci.daysAway) { checkIn = null }
            levelRewards.isNotEmpty() -> LevelUpDialog(levelRewards) { levelRewards = emptyList() }
            leagueResult != null -> LeagueResultDialog(leagueResult!!) { retention.claimLeagueResult(); leagueResult = null }
            unlockedSkins.isNotEmpty() -> SkinUnlockedDialog(unlockedSkins) { unlockedSkins = emptyList() }
        }
    }

    when (dialog) {
        HubDialog.CHEST -> FreeChestDialog(retention, onOpenAlbum = onAlbum, onDismiss = { dialog = null })
        HubDialog.PIGGY -> PiggyDialog(retention, piggyPrice, onBuy = { onBuyPiggy() }, onDismiss = { dialog = null })
        null -> Unit
    }
}
