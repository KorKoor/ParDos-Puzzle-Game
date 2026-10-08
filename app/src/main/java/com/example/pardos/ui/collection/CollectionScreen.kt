package com.korkoor.pardos.ui.collection

import com.korkoor.pardos.ui.design.CozyText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.korkoor.pardos.data.local.CollectionManager
import com.korkoor.pardos.domain.collection.AlbumBonus
import com.korkoor.pardos.domain.collection.*
import com.korkoor.pardos.ui.design.*

/** Mapea la clave de icono del catálogo a un icono real de la app. */
fun collectibleIcon(key: String): ImageVector = when (key) {
    "florist" -> Icons.Rounded.LocalFlorist
    "spa" -> Icons.Rounded.Spa
    "eco" -> Icons.Rounded.Eco
    "grass" -> Icons.Rounded.Grass
    "park" -> Icons.Rounded.Park
    "forest" -> Icons.Rounded.Forest
    "nature" -> Icons.Rounded.Nature
    "pets" -> Icons.Rounded.Pets
    "sunny" -> Icons.Rounded.WbSunny
    "night" -> Icons.Rounded.NightsStay
    "cloud" -> Icons.Rounded.Cloud
    "snow" -> Icons.Rounded.AcUnit
    "air" -> Icons.Rounded.Air
    "bolt" -> Icons.Rounded.Bolt
    "public" -> Icons.Rounded.Public
    "rocket" -> Icons.Rounded.RocketLaunch
    "coffee" -> Icons.Rounded.Coffee
    "tea" -> Icons.Rounded.EmojiFoodBeverage
    "cake" -> Icons.Rounded.Cake
    "icecream" -> Icons.Rounded.Icecream
    "cookie" -> Icons.Rounded.Cookie
    "lunch" -> Icons.Rounded.LunchDining
    "bakery" -> Icons.Rounded.BakeryDining
    "ramen" -> Icons.Rounded.RamenDining
    "hiking" -> Icons.Rounded.Hiking
    "sailing" -> Icons.Rounded.Sailing
    "kayak" -> Icons.Rounded.Kayaking
    "anchor" -> Icons.Rounded.Anchor
    "landscape" -> Icons.Rounded.Landscape
    "waves" -> Icons.Rounded.Waves
    "explore" -> Icons.Rounded.Explore
    "map" -> Icons.Rounded.Map
    "brush" -> Icons.Rounded.Brush
    "palette" -> Icons.Rounded.Palette
    "music" -> Icons.Rounded.MusicNote
    "casino" -> Icons.Rounded.Casino
    "extension" -> Icons.Rounded.Extension
    "toys" -> Icons.Rounded.Toys
    "camera" -> Icons.Rounded.PhotoCamera
    "piano" -> Icons.Rounded.Piano
    "savings" -> Icons.Rounded.Savings
    "toll" -> Icons.Rounded.Toll
    "key" -> Icons.Rounded.Key
    "shield" -> Icons.Rounded.Shield
    "trophy" -> Icons.Rounded.EmojiEvents
    "premium" -> Icons.Rounded.WorkspacePremium
    "star" -> Icons.Rounded.AutoAwesome
    "diamond" -> Icons.Rounded.Diamond
    "set_meal" -> Icons.Rounded.SetMeal
    "water_drop" -> Icons.Rounded.WaterDrop
    "bubble_chart" -> Icons.Rounded.BubbleChart
    "beach_access" -> Icons.Rounded.BeachAccess
    "pool" -> Icons.Rounded.Pool
    "surfing" -> Icons.Rounded.Surfing
    "scuba_diving" -> Icons.Rounded.ScubaDiving
    "water" -> Icons.Rounded.Water
    "celebration" -> Icons.Rounded.Celebration
    "confirmation_number" -> Icons.Rounded.ConfirmationNumber
    "card_giftcard" -> Icons.Rounded.CardGiftcard
    "fireplace" -> Icons.Rounded.Fireplace
    "theater_comedy" -> Icons.Rounded.TheaterComedy
    "attractions" -> Icons.Rounded.Attractions
    "festival" -> Icons.Rounded.Festival
    "flare" -> Icons.Rounded.Flare
    "nightlight" -> Icons.Rounded.Nightlight
    "emoji_objects" -> Icons.Rounded.EmojiObjects
    "local_fire_department" -> Icons.Rounded.LocalFireDepartment
    "scatter_plot" -> Icons.Rounded.ScatterPlot
    "auto_stories" -> Icons.Rounded.AutoStories
    "science" -> Icons.Rounded.Science
    "blur_circular" -> Icons.Rounded.BlurCircular
    "auto_fix_high" -> Icons.Rounded.AutoFixHigh
    else -> Icons.Rounded.Star
}

fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF8FA39A)
    Rarity.RARE -> GemBlue
    Rarity.EPIC -> Violet
    Rarity.LEGENDARY -> Gold
}

fun rarityName(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"; Rarity.RARE -> "Rara"; Rarity.EPIC -> "Épica"; Rarity.LEGENDARY -> "Legendaria"
}

fun chestName(t: ChestType): String = when (t) {
    ChestType.COMMON -> "Cofre común"; ChestType.RARE -> "Cofre raro"; ChestType.EPIC -> "Cofre épico"
}

fun chestColor(t: ChestType): Color = when (t) {
    ChestType.COMMON -> Color(0xFF8FA39A); ChestType.RARE -> GemBlue; ChestType.EPIC -> Violet
}

@Composable
fun CollectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { CollectionManager(context) }
    val owned by manager.owned.collectAsState()
    val shards by manager.shards.collectAsState()
    val chests by manager.chests.collectAsState()
    val claimed by manager.claimedSeries.collectAsState()
    val albumClaimed by manager.albumClaimed.collectAsState()
    val foil by manager.foil.collectAsState()
    val gemsNow by com.korkoor.pardos.data.local.EconomyManager(context).gems.collectAsState()
    val haptic = com.korkoor.pardos.ui.design.rememberGameHaptics()

    var opening by remember { mutableStateOf<ChestResult?>(null) }
    var detail by remember { mutableStateOf<Collectible?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    val total = CollectibleCatalog.all.size
    val have = owned.size

    Box(modifier = Modifier.fillMaxSize().pardosBackdrop()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = "Álbum", title = "Colección", onBack = onBack) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = Violet, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$shards", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Progreso total ---
                item {
                    PardosCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.XLarge), elevation = 8.dp) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$have", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Navy)
                                Text(" / $total piezas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkTertiary, modifier = Modifier.padding(bottom = 5.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            ProgressBar(have.toFloat() / total, Gold)
                            val bonus = AlbumBonus.breakdown(owned, foil)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.MonetizationOn, null, tint = Gold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Bono del álbum: +${bonus.total}% monedas", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
                                    Text(
                                        "+1% cada 8 piezas · +${AlbumBonus.PER_SERIES}% por serie completa · +${AlbumBonus.FULL_ALBUM}% álbum entero · +1% cada ${FoilRules.PER_PERCENT} brillantes (máx. ${AlbumBonus.MAX_PERCENT}%)",
                                        fontSize = 10.sp, color = InkSecondary, lineHeight = 13.sp
                                    )
                                }
                            }
                            if (CollectibleCatalog.isAlbumComplete(owned)) {
                                Spacer(Modifier.height(12.dp))
                                if (manager.isAlbumClaimable) {
                                    PrimaryButton("RECLAMAR ÁLBUM COMPLETO", onClick = {
                                        manager.claimAlbum(); toast = "¡Skin Oro Real y 50 gemas!"
                                    }, height = 48.dp, fontSize = 12.sp)
                                } else {
                                    Text("¡Álbum completo! Skin Oro Real desbloqueada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage)
                                }
                            } else {
                                Spacer(Modifier.height(6.dp))
                                Text("Completa las ${Series.entries.size} series para ganar la skin exclusiva Oro Real.", fontSize = 11.sp, color = InkSecondary)
                            }
                        }
                    }
                }

                // --- Cofres sin abrir ---
                item {
                    SectionLabel("Tus cofres")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChestType.entries.forEach { t ->
                            val n = chests[t] ?: 0
                            ChestSlot(t, n, Modifier.weight(1f)) {
                                if (n > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    opening = manager.openChest(t)
                                }
                            }
                        }
                    }
                    Text(
                        "Gánalos con logros, rachas, capítulos o cómpralos en la tienda. Las repetidas se convierten en esencia para crear piezas que te falten.",
                        fontSize = 11.sp, color = InkSecondary, lineHeight = 15.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // --- Esencia ---
                item {
                    val packsToday = manager.shardPacksToday()
                    val canBuy = ShardShop.canBuy(gemsNow, packsToday)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.Medium)).background(Violet.copy(alpha = 0.10f)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = Violet, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Esencia: $shards", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
                            Text("Crea piezas que te faltan o hazlas Brillantes. Hoy: $packsToday/${ShardShop.MAX_PACKS_PER_DAY} packs", fontSize = 11.sp, color = InkSecondary, lineHeight = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                .background(if (canBuy) Violet else Navy.copy(alpha = 0.08f))
                                .clickable(enabled = canBuy) {
                                    toast = if (manager.buyShardPack()) "+${ShardShop.SHARDS_PER_PACK} de esencia" else "No se pudo comprar"
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+${ShardShop.SHARDS_PER_PACK}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (canBuy) Color.White else Navy.copy(alpha = 0.35f))
                            Spacer(Modifier.width(6.dp))
                            CozyText("${ShardShop.GEMS_PER_PACK} ◆", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (canBuy) Color.White else Navy.copy(alpha = 0.35f))
                        }
                    }
                }

                toast?.let { msg ->
                    item {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.Medium)).background(Sage.copy(alpha = 0.16f))
                                .clickable { toast = null }.padding(14.dp)
                        ) { Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy) }
                    }
                }

                // --- Series ---
                Series.entries.forEach { series ->
                    item(key = series.id) {
                        SeriesBlock(
                            series = series,
                            owned = owned,
                            foil = foil,
                            claimed = series.id in claimed,
                            claimable = manager.isSeriesClaimable(series),
                            onClaim = {
                                manager.claimSeries(series)?.let { (c, g) -> toast = "¡Serie ${series.nameEs} completa! +$c monedas y +$g gemas" }
                            },
                            onPick = { detail = it }
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // --- Apertura de cofre ---
        opening?.let { res -> ChestOpeningDialog(res) { opening = null } }

        // --- Detalle / crear ---
        detail?.let { c ->
            CollectibleDialog(
                c = c,
                owned = c.id in owned,
                foil = c.id in foil,
                canFoil = FoilRules.canUpgrade(c, owned, foil, shards),
                shards = shards,
                onCraft = {
                    if (manager.craft(c)) toast = "¡Creaste ${c.nameEs}!"
                    detail = null
                },
                onFoil = {
                    if (manager.upgradeFoil(c)) toast = "¡${c.nameEs} ahora es Brillante!"
                    detail = null
                },
                onDismiss = { detail = null }
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "p")
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(anim.coerceAtLeast(0.02f)).background(color, CircleShape))
    }
}

@Composable
private fun ChestSlot(type: ChestType, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = chestColor(type)
    val bounce by rememberInfiniteTransition(label = "cs").animateFloat(
        0f, if (count > 0) -4f else 0f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.Large))
            .background(Color.White)
            .border(if (count > 0) 2.dp else 1.dp, if (count > 0) color else Navy.copy(alpha = 0.08f), RoundedCornerShape(Radius.Large))
            .clickable(enabled = count > 0, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.offset(y = bounce.dp).size(64.dp), contentAlignment = Alignment.Center) {
            com.korkoor.pardos.ui.design.TreasureChest(
                type = type, state = if (count > 0) com.korkoor.pardos.ui.design.ChestState.READY else com.korkoor.pardos.ui.design.ChestState.LOCKED,
                modifier = Modifier.fillMaxSize(), animate = count > 0
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("x$count", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (count > 0) Navy else Navy.copy(alpha = 0.3f))
        Text(
            when (type) { ChestType.COMMON -> "COMÚN"; ChestType.RARE -> "RARO"; ChestType.EPIC -> "ÉPICO" },
            fontSize = 9.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun SeriesBlock(
    series: Series, owned: Set<String>, foil: Set<String>, claimed: Boolean, claimable: Boolean,
    onClaim: () -> Unit, onPick: (Collectible) -> Unit
) {
    val (have, total) = CollectibleCatalog.progress(series, owned)
    PardosCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.Large), elevation = 5.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(series.nameEs, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Navy)
                    CozyText("$have/$total · premio: ${series.rewardCoins} monedas + ${series.rewardGems}◆", fontSize = 11.sp, color = InkSecondary)
                }
                when {
                    claimed -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Sage, modifier = Modifier.size(26.dp))
                    claimable -> Text(
                        "RECLAMAR",
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Gold).clickable(onClick = onClaim).padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp
                    )
                    else -> Unit
                }
            }
            Spacer(Modifier.height(10.dp))
            // Cuadrícula fija 4x2 (sin scroll anidado)
            val items = CollectibleCatalog.inSeries(series)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            CollectibleTile(c, c.id in owned, Modifier.weight(1f), foil = c.id in foil) { onPick(c) }
                        }
                    }
                }
            }
        }
    }
}

/** Destello que recorre la pieza de izquierda a derecha: así se distinguen las Brillantes. */
fun Modifier.foilShimmer(): Modifier = composed {
    val t by rememberInfiniteTransition(label = "foil").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "foilT"
    )
    drawWithContent {
        drawContent()
        val w = size.width
        val x = (t * 2.2f - 0.6f) * w
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color(0xFFFFE9A8).copy(alpha = 0.35f), Color.Transparent),
                start = Offset(x - w * 0.35f, 0f), end = Offset(x + w * 0.35f, size.height)
            )
        )
    }
}

@Composable
fun CollectibleTile(c: Collectible, owned: Boolean, modifier: Modifier = Modifier, foil: Boolean = false, onClick: () -> Unit) {
    val color = rarityColor(c.rarity)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(shape)
            .background(if (owned) color.copy(alpha = 0.12f) else Navy.copy(alpha = 0.05f))
            .border(if (foil) 2.dp else if (owned) 1.5.dp else 1.dp, if (foil) Gold else if (owned) color.copy(alpha = 0.6f) else Navy.copy(alpha = 0.06f), shape)
            .then(if (foil) Modifier.foilShimmer() else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (owned) {
            Icon(collectibleIcon(c.iconKey), contentDescription = c.nameEs, tint = color, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(4.dp))
            Text(c.nameEs, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Navy, maxLines = 1, textAlign = TextAlign.Center)
        } else {
            Icon(collectibleIcon(c.iconKey), contentDescription = null, tint = Navy.copy(alpha = 0.12f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text("?", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun CollectibleDialog(c: Collectible, owned: Boolean, foil: Boolean, canFoil: Boolean, shards: Int, onCraft: () -> Unit, onFoil: () -> Unit, onDismiss: () -> Unit) {
    val color = rarityColor(c.rarity)
    val canCraft = CraftRules.canCraft(c, if (owned) setOf(c.id) else emptySet(), shards)
    Dialog(onDismissRequest = onDismiss) {
        JellyColumn(
            modifier = Modifier.popIn(), shape = RoundedCornerShape(Radius.XLarge), fill = Cream, lipHeight = 8.dp,
            padding = PaddingValues(24.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(96.dp).background(color.copy(alpha = if (owned) 0.18f else 0.07f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(collectibleIcon(c.iconKey), contentDescription = null, tint = if (owned) color else Navy.copy(alpha = 0.18f), modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(if (owned) c.nameEs else "???", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
            Text(rarityName(c.rarity).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 2.sp)
            Text(c.series.nameEs, fontSize = 12.sp, color = InkSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(18.dp))
            if (owned) {
                Text("Si te sale repetida vale ${c.rarity.shardValue} de esencia.", fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                if (foil) {
                    Text("✦ BRILLANTE", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Gold, letterSpacing = 2.sp)
                } else {
                    PrimaryButton(
                        text = "HACER BRILLANTE · ${FoilRules.cost(c)} esencia",
                        onClick = onFoil, enabled = canFoil, height = 50.dp, fontSize = 12.sp
                    )
                    Text(
                        if (canFoil) "Brilla en tu álbum y suma monedas extra para siempre." else "Te faltan ${FoilRules.cost(c) - shards} de esencia. Cada 6 brillantes = +1% de monedas.",
                        fontSize = 11.sp, color = InkSecondary, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center
                    )
                }
            } else {
                PrimaryButton(
                    text = "CREAR · ${c.rarity.craftCost} esencia",
                    onClick = onCraft, enabled = canCraft, height = 50.dp, fontSize = 12.sp
                )
                Text(
                    if (canCraft) "Gasta tu esencia para conseguirla ya." else "Te faltan ${c.rarity.craftCost - shards} de esencia (consíguela con repetidas).",
                    fontSize = 11.sp, color = InkSecondary, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChestOpeningDialog(result: ChestResult, onDone: () -> Unit) {
    // Las cartas se revelan una a una
    var revealed by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        for (i in 1..result.drops.size) {
            val rarity = result.drops[i - 1].collectible.rarity
            // Suspense: las piezas buenas se hacen esperar un poco más
            kotlinx.coroutines.delay(if (rarity.ordinal >= Rarity.EPIC.ordinal) 950L else 550L)
            revealed = i
            haptic.performHapticFeedback(if (rarity.ordinal >= Rarity.RARE.ordinal) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }
    }
    val all = revealed >= result.drops.size
    val best = result.drops.maxOf { it.collectible.rarity.ordinal }
    Dialog(onDismissRequest = { if (all) onDone() }) {
        JellyColumn(
            modifier = Modifier.popIn(), shape = RoundedCornerShape(Radius.XLarge), fill = Cream, lipHeight = 8.dp,
            padding = PaddingValues(20.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when {
                    !all -> "Abriendo…"
                    best == Rarity.LEGENDARY.ordinal -> "¡LEGENDARIA!"
                    best == Rarity.EPIC.ordinal -> "¡Pieza épica!"
                    else -> "¡Tu botín!"
                },
                fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = if (all && best == Rarity.LEGENDARY.ordinal) Gold else Navy
            )
            Spacer(Modifier.height(16.dp))
            result.drops.chunked(2).forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    row.forEachIndexed { colIdx, drop ->
                        val idx = rowIdx * 2 + colIdx
                        DropCard(drop, visible = idx < revealed, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            val shards = result.drops.sumOf { it.shards }
            if (all && shards > 0) {
                Text("+$shards de esencia por repetidas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Violet)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(6.dp))
            PrimaryButton(if (all) "GENIAL" else "...", onClick = onDone, enabled = all, height = 50.dp)
        }
    }
}

@Composable
private fun DropCard(drop: Drop, visible: Boolean, modifier: Modifier) {
    val c = drop.collectible
    val color = rarityColor(c.rarity)
    // Estallido de partículas al revelar: más vistoso cuanto más rara es la pieza
    val burstFx = when (c.rarity) {
        Rarity.COMMON -> com.korkoor.pardos.domain.shop.MergeFx.CLASSIC
        Rarity.RARE -> com.korkoor.pardos.domain.shop.MergeFx.SPARKS
        Rarity.EPIC -> com.korkoor.pardos.domain.shop.MergeFx.STARS
        Rarity.LEGENDARY -> com.korkoor.pardos.domain.shop.MergeFx.FIREWORKS
    }
    val pulse by rememberInfiniteTransition(label = "legend").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "legendA"
    )
    val glow = if (visible && c.rarity == Rarity.LEGENDARY) pulse else 0f
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
    AnimatedVisibility(visible = visible, modifier = Modifier.fillMaxWidth(), enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.Large)).background(color.copy(alpha = 0.12f + 0.12f * glow))
                .border(if (glow > 0f) 3.dp else 2.dp, if (glow > 0f) Gold.copy(alpha = 0.5f + 0.5f * glow) else color, RoundedCornerShape(Radius.Large)).padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(collectibleIcon(c.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(6.dp))
            Text(c.nameEs, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1)
            Text(rarityName(c.rarity).uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.sp)
            if (drop.isNew) {
                Text("¡NUEVA!", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Sage, modifier = Modifier.padding(top = 4.dp))
            } else {
                Text("repetida", fontSize = 9.sp, color = InkTertiary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    com.korkoor.pardos.ui.game.components.MergeBurst(burstFx, if (visible) 1 else 0, 1024, 96.dp, Modifier.zIndex(5f))
    }
}
