package com.korkoor.pardos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.SettingsManager
import com.korkoor.pardos.ui.design.*

/** Ajustes: lo básico que todo jugador espera poder apagar. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    val sound by settings.soundEnabled.collectAsState()
    val music by settings.musicEnabled.collectAsState()
    val haptics by settings.hapticsEnabled.collectAsState()
    val notifications by settings.notificationsEnabled.collectAsState()
    val autoNext by settings.autoNextEnabled.collectAsState()
    val sfxVol by settings.sfxVolume.collectAsState()
    val musicVol by settings.musicVolume.collectAsState()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(
        modifier = Modifier.fillMaxSize().pardosBackdrop().statusBarsPadding().navigationBarsPadding()
    ) {
        PardosTopBar(title = "Ajustes", eyebrow = "Tu experiencia", onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Sonido y tacto")
            PardosCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    SettingRow(Icons.Rounded.VolumeUp, Sage, "Efectos de sonido", "Fusiones, combos, menús y premios", sound) {
                        settings.setSound(it)
                        if (it) com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.UI_ON)
                    }
                    if (sound) VolumeRow(Sage, sfxVol, { settings.setSfxVolume(it) }) {
                        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.COIN)
                    }
                    SettingRow(Icons.Rounded.MusicNote, Violet, "Música", "Calma en el menú y más energía cuando entras en racha", music) { settings.setMusic(it) }
                    if (music) VolumeRow(Violet, musicVol, { settings.setMusicVolume(it) }) { }
                    SettingRow(Icons.Rounded.Vibration, Terracotta, "Vibración", "Un toque suave al mover y fusionar", haptics) { settings.setHaptics(it) }
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("Ritmo de juego")
            PardosCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    SettingRow(
                        Icons.Rounded.PlayArrow, Sage, "Siguiente nivel automático",
                        "Tras ganar pasas solo al siguiente. Un toque en la pantalla lo pausa.", autoNext
                    ) { settings.setAutoNext(it) }
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("Avisos")
            PardosCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    SettingRow(
                        Icons.Rounded.NotificationsActive, Gold, "Recordatorios",
                        "Cofre listo, racha en riesgo y premios por cobrar. Nunca de noche.", notifications
                    ) { settings.setNotifications(it) }
                    TestNotificationRow()
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("Privacidad")
            PardosCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    LinkRow(Icons.Rounded.PrivacyTip, GemBlue, "Política de privacidad", "Qué guardamos y cómo pedir que se borre") {
                        runCatching {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_URL)))
                        }
                    }
                    // La ley (EEE, Reino Unido y algunos estados de EE. UU.) obliga a poder cambiar la decisión sobre anuncios
                    if (com.korkoor.pardos.ui.game.logic.AdManager.privacyOptionsRequired) {
                        LinkRow(Icons.Rounded.Tune, Violet, "Anuncios y privacidad", "Cambia lo que elegiste sobre anuncios personalizados") {
                            (context as? android.app.Activity)?.let { com.korkoor.pardos.ui.game.logic.AdManager.showPrivacyOptions(it) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "ParDos${if (version.isNotBlank()) " · versión $version" else ""}\nKorKoor Studios",
                fontSize = 12.sp, color = InkTertiary, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private const val PRIVACY_URL = "https://www.korwork.org/ParDos-Puzzle-Game/"

/** Fila que abre algo (una página o un formulario) en vez de activar un interruptor. */
@Composable
private fun LinkRow(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, color, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(subtitle, fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = InkTertiary, modifier = Modifier.size(22.dp))
    }
}

/** Barra de volumen bajo un interruptor de sonido. */
@Composable
private fun VolumeRow(color: Color, value: Float, onChange: (Float) -> Unit, onFinished: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 74.dp, end = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.VolumeDown, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        androidx.compose.material3.Slider(
            value = value, onValueChange = onChange, onValueChangeFinished = onFinished,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.18f)
            )
        )
        Icon(Icons.Rounded.VolumeUp, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingRow(icon: ImageVector, color: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, color, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(subtitle, fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        ToySwitch(checked = checked, onCheckedChange = onChange)
    }
}


/** Botón para ver cómo llega un aviso (también pide el permiso en Android 13+). */
@Composable
private fun TestNotificationRow() {
    val context = LocalContext.current
    var note by remember { mutableStateOf<String?>(null) }
    val halloween = remember {
        com.korkoor.pardos.domain.retention.SeasonalCopy.isHalloweenWindow(com.korkoor.pardos.data.local.LocalDay.today())
    }
    fun send() {
        val ok = com.korkoor.pardos.notifications.PardosNotifier.show(
            context,
            if (halloween) "¡Los avisos funcionan!" else "¡Los avisos funcionan!",
            if (halloween) "Así te avisaremos cuando tu cofre embrujado esté listo." else "Así te avisaremos cuando tu cofre esté listo.",
            id = 99, key = "free_chest"
        )
        note = if (ok) "Enviado. Mira tu barra de notificaciones." else "No hay permiso para mostrar avisos."
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) send() else note = "Sin permiso no podemos avisarte. Actívalo en los ajustes del teléfono." }

    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            if (android.os.Build.VERSION.SDK_INT >= 33 && !com.korkoor.pardos.notifications.PardosNotifier.canPost(context)) {
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else send()
        }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(Icons.Rounded.Send, Sage, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Enviar aviso de prueba", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(note ?: "Comprueba que te llegan y cómo se ven.", fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
        }
    }
}
