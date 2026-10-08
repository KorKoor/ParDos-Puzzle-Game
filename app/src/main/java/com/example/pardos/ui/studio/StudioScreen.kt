package com.korkoor.pardos.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.domain.shop.Hsl
import com.korkoor.pardos.domain.shop.ParticleKind
import com.korkoor.pardos.domain.shop.SkinStyle
import com.korkoor.pardos.domain.shop.StudioBackground
import com.korkoor.pardos.domain.shop.StudioConfig
import com.korkoor.pardos.domain.shop.StudioPresets
import com.korkoor.pardos.domain.shop.StudioTone
import com.korkoor.pardos.domain.shop.TileFinish
import com.korkoor.pardos.domain.shop.TileSkin
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.components.AmbientParticles
import com.korkoor.pardos.ui.game.components.tileLook
import com.korkoor.pardos.ui.theme.GameTheme

private val finishNames = mapOf(
    TileFinish.JELLY to "Gelatina", TileFinish.FLAT to "Plano", TileFinish.GLASS to "Cristal", TileFinish.WOOD to "Madera",
    TileFinish.NEON to "Neón", TileFinish.PORCELAIN to "Porcelana", TileFinish.METAL to "Metal"
)
private val toneNames = mapOf(StudioTone.PASTEL to "Suave", StudioTone.VIVID to "Vivo", StudioTone.DEEP to "Profundo")
private val bgNames = mapOf(StudioBackground.LIGHT to "Claro", StudioBackground.TINTED to "Teñido", StudioBackground.DARK to "Oscuro")
private val particleNames = mapOf(
    ParticleKind.NONE to "Ninguna", ParticleKind.PETALS to "Pétalos", ParticleKind.SNOW to "Nieve", ParticleKind.LEAVES to "Hojas",
    ParticleKind.FIREFLIES to "Luciérnagas", ParticleKind.BUBBLES to "Burbujas", ParticleKind.STARS to "Estrellas",
    ParticleKind.EMBERS to "Brasas", ParticleKind.SPRINKLES to "Confeti", ParticleKind.RAIN to "Lluvia", ParticleKind.SAND to "Arena",
    ParticleKind.HEARTS to "Corazones", ParticleKind.SPARKLES to "Destellos", ParticleKind.SNOWFLAKE to "Copos",
    ParticleKind.FIREWORKS to "Fuegos", ParticleKind.METEORS to "Meteoros", ParticleKind.FLOWERS to "Flores"
)

/**
 * Editor de la skin Studio. Se puede probar todo sin pagar; guardar y equipar requiere la compra.
 * Cada cambio se ve al instante en un tablero de muestra con el fondo y las partículas reales.
 */
@Composable
fun StudioScreen(onBack: () -> Unit, price: String?, onBuy: () -> Unit) {
    val context = LocalContext.current
    val economy = remember { EconomyManager(context) }
    val saved by economy.studioConfig.collectAsState()
    val ownedSkins by economy.ownedSkins.collectAsState()
    val owned = TileSkin.STUDIO.id in ownedSkins
    var draft by remember { mutableStateOf(saved) }
    var message by remember { mutableStateOf<String?>(null) }
    val style = remember(draft) { draft.toStyle() }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = "Skin de autor", title = "Studio", onBack = onBack)

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudioPreview(style)

                message?.let {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Sage.copy(alpha = 0.15f)).clickable { message = null }.padding(14.dp)) {
                        Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                    }
                }

                Section("PUNTOS DE PARTIDA")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StudioPresets.all) { p ->
                        val st = remember(p) { p.config.toStyle() }
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
                                .border(1.dp, Navy.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .clickable { draft = p.config }.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0, 5, 11).forEach { i -> Box(Modifier.size(14.dp).background(Color(st.tilePalette!![i]), CircleShape)) ; Spacer(Modifier.width(3.dp)) }
                            Spacer(Modifier.width(4.dp))
                            Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy)
                        }
                    }
                }

                Section("ACABADO")
                ChipRow(TileFinish.entries, draft.finish, { finishNames[it] ?: it.name }) { draft = draft.copy(finish = it) }

                Section("COLOR INICIAL")
                HueBar(draft.hue) { draft = draft.copy(hue = it) }
                Section("COLOR FINAL")
                HueBar(draft.hue2) { draft = draft.copy(hue2 = it) }

                Section("INTENSIDAD")
                ChipRow(StudioTone.entries, draft.tone, { toneNames[it] ?: it.name }) { draft = draft.copy(tone = it) }

                Section("FONDO")
                if (draft.finish == TileFinish.NEON) {
                    Text("El neón siempre va sobre fondo oscuro.", fontSize = 11.sp, color = InkTertiary)
                } else {
                    ChipRow(StudioBackground.entries, draft.background, { bgNames[it] ?: it.name }) { draft = draft.copy(background = it) }
                }

                Section("PARTÍCULAS")
                ChipRow(particleNames.keys.toList(), draft.particles, { particleNames[it] ?: it.name }) { draft = draft.copy(particles = it) }

                Spacer(Modifier.height(8.dp))
            }

            // Barra de acción fija
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (owned) {
                    PrimaryButton("Guardar y equipar", onClick = {
                        economy.saveStudioConfig(draft)
                        economy.equipSkin(TileSkin.STUDIO)
                        message = "¡Tu diseño está equipado!"
                    }, icon = Icons.Rounded.Check)
                } else {
                    PrimaryButton(
                        if (price != null) "Desbloquear Studio · $price" else "Studio · pronto",
                        onClick = onBuy, icon = Icons.Rounded.LockOpen, enabled = price != null
                    )
                    Text(
                        "Prueba todo gratis. Compra única: la skin y el editor son tuyos para siempre.",
                        fontSize = 10.sp, color = InkTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(6.dp))
    SectionLabel(text)
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { o ->
            val sel = o == selected
            Text(
                label(o), fontSize = 12.sp, fontWeight = FontWeight.Black,
                color = if (sel) Color.White else Navy,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (sel) Sage else Color.White)
                    .border(1.dp, if (sel) Sage else Navy.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { onSelect(o) }.padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

/** Barra con todos los tonos; se toca o se arrastra para elegir. */
@Composable
private fun HueBar(hue: Int, onChange: (Int) -> Unit) {
    val colors = remember { (0..12).map { Color(Hsl.argb(it * 30f, 0.8f, 0.55f)) } }
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(colors))
            .pointerInput(Unit) { detectTapGestures { o -> onChange((o.x / size.width * 360f).toInt().coerceIn(0, 359)) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { c, _ -> onChange((c.position.x / size.width * 360f).toInt().coerceIn(0, 359)) } }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val x = maxWidth * (hue / 360f)
            Box(
                Modifier.offset(x = (x - 14.dp).coerceAtLeast(0.dp).coerceAtMost(maxWidth - 28.dp), y = 4.dp)
                    .size(32.dp).shadow(4.dp, CircleShape).background(Color(Hsl.argb(hue.toFloat(), 0.8f, 0.55f)), CircleShape)
                    .border(3.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun StudioPreview(style: SkinStyle) {
    val top = Color(style.bgTop ?: 0xFFF9F9F9)
    val bottom = Color(style.bgBottom ?: 0xFFF3F0E9)
    val values = listOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096)
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.XLarge))
            .background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        AmbientParticles(kind = style.particles, tint = Color(style.particleTint), modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("TU TABLERO", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, color = Color(style.ink ?: 0xFF3D405B))
            values.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { StyleTile(style, it, 66.dp) } }
            }
        }
    }
}

@Composable
private fun StyleTile(style: SkinStyle, value: Int, size: Dp) {
    val look = tileLook(style, value, GameTheme.Zen)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(size)
            .then(if (look.glow != null) Modifier.shadow(8.dp, shape, spotColor = look.glow, ambientColor = look.glow) else Modifier.shadow(look.elevation.dp, shape))
            .clip(shape)
            .background(look.background)
            .then(if (look.border != null) Modifier.border(1.5.dp, look.border, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (look.gloss) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.5f to Color.Transparent)))
        Text("$value", fontSize = if (value >= 1000) 15.sp else if (value >= 100) 18.sp else 22.sp, fontWeight = FontWeight.Black, color = look.text)
    }
}
