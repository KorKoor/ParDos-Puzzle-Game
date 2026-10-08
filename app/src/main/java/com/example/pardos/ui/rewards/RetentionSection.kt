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
    var showWhatsNew by remember { mutableStateOf(false) }
    var showPrimer by remember { mutableStateOf(false) }
    var showHalloween by remember { mutableStateOf(false) }
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

        // Noche de brujas: bienvenida una vez por año (y solo a quien ya jugaba)
        val hwKey = "halloween_welcome_" + com.korkoor.pardos.domain.retention.Civil.fromEpochDay(com.korkoor.pardos.data.local.LocalDay.today()).year
        // Novedades: una vez por versión y solo para quien ya jugaba
        val settings = context.getSharedPreferences("pardos_settings", android.content.Context.MODE_PRIVATE)
        val version = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0)
        ).toInt()
        val profile = ProfileManager(context).getProfile()
        val fresh = profile.playerLevel <= 1 && profile.currentXp == 0
        showHalloween = com.korkoor.pardos.ui.design.Season.halloween && !fresh &&
            !context.getSharedPreferences("pardos_settings", android.content.Context.MODE_PRIVATE).getBoolean(hwKey, false)
        if (com.korkoor.pardos.domain.retention.WhatsNew.shouldShow(settings.getInt("whats_new_seen", 0), version, fresh)) showWhatsNew = true
        else if (!settings.contains("whats_new_seen")) settings.edit().putInt("whats_new_seen", version).apply()

        // Permiso de avisos: se pregunta tras la primera victoria, explicando qué se avisará (y sin insistir)
        val needed = android.os.Build.VERSION.SDK_INT >= 33
        val lastAsk = settings.getInt("notif_primer_day", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        showPrimer = com.korkoor.pardos.domain.retention.NotificationPrimer.shouldAsk(
            permissionNeeded = needed,
            granted = com.korkoor.pardos.notifications.PardosNotifier.canPost(context),
            enabledInSettings = com.korkoor.pardos.data.local.SettingsManager(context).notificationsEnabled.value,
            hasWonAGame = !fresh,
            lastAskDay = lastAsk,
            askCount = settings.getInt("notif_primer_count", 0),
            today = com.korkoor.pardos.data.local.LocalDay.today()
        )
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
            showPrimer && !showWhatsNew -> NotificationPrimerDialog(
                onAccept = {
                    showPrimer = false
                    markPrimerAsked(context)
                    (context as? android.app.Activity)?.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 7001)
                },
                onLater = { showPrimer = false; markPrimerAsked(context) }
            )
            showHalloween && !showWhatsNew && !showPrimer -> {
                val eco = remember { com.korkoor.pardos.data.local.EconomyManager(context) }
                val owned = com.korkoor.pardos.domain.shop.TileSkin.HALLOWEEN.id in eco.ownedSkins.value
                val equipped = eco.equippedSkin.value == com.korkoor.pardos.domain.shop.TileSkin.HALLOWEEN
                val seen = {
                    showHalloween = false
                    val year = com.korkoor.pardos.domain.retention.Civil.fromEpochDay(com.korkoor.pardos.data.local.LocalDay.today()).year
                    context.getSharedPreferences("pardos_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("halloween_welcome_$year", true).apply()
                }
                HalloweenWelcomeDialog(
                    ownsSkin = owned, skinEquipped = equipped,
                    onEquip = { eco.equipSkin(com.korkoor.pardos.domain.shop.TileSkin.HALLOWEEN); seen() },
                    onDismiss = seen
                )
            }
            showWhatsNew -> WhatsNewDialog {
                showWhatsNew = false
                val v = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)).toInt()
                context.getSharedPreferences("pardos_settings", android.content.Context.MODE_PRIVATE).edit().putInt("whats_new_seen", v).apply()
            }
        }
    }

    when (dialog) {
        HubDialog.CHEST -> FreeChestDialog(retention, onOpenAlbum = onAlbum, onDismiss = { dialog = null })
        HubDialog.PIGGY -> PiggyDialog(retention, piggyPrice, onBuy = { onBuyPiggy() }, onDismiss = { dialog = null })
        null -> Unit
    }
}


private fun markPrimerAsked(context: android.content.Context) {
    val prefs = context.getSharedPreferences("pardos_settings", android.content.Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("notif_primer_day", com.korkoor.pardos.data.local.LocalDay.today())
        .putInt("notif_primer_count", prefs.getInt("notif_primer_count", 0) + 1)
        .apply()
}
