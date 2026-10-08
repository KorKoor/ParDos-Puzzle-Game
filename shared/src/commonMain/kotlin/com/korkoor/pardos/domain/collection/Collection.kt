package com.korkoor.pardos.domain.collection

import kotlin.random.Random

enum class Rarity(val dropWeightOrder: Int, val shardValue: Int, val craftCost: Int) {
    COMMON(0, 10, 40),
    RARE(1, 25, 100),
    EPIC(2, 60, 250),
    LEGENDARY(3, 150, 600)
}

/** Series coleccionables (9). Cada una tiene 8 piezas: 4 comunes, 2 raras, 1 épica y 1 legendaria. */
enum class Series(val id: String, val nameEs: String, val nameEn: String, val rewardCoins: Int, val rewardGems: Int) {
    GARDEN("garden", "Jardín Zen", "Zen Garden", 400, 5),
    SKY("sky", "Cielo", "Sky", 400, 5),
    TEA("tea", "Mesa de Té", "Tea Table", 400, 5),
    ADVENTURE("adventure", "Aventura", "Adventure", 600, 8),
    STUDIO("studio", "Estudio", "Studio", 600, 8),
    TREASURE("treasure", "Tesoros", "Treasures", 900, 15),
    OCEAN("ocean", "Mar Profundo", "Deep Sea", 700, 10),
    FESTIVAL("festival", "Fiesta", "Festival", 700, 10),
    MAGIC("magic", "Noche Mágica", "Magic Night", 1_000, 18)
}

/** Una pieza del álbum. [iconKey] se traduce a un icono en la app. */
data class Collectible(
    val id: String,
    val series: Series,
    val rarity: Rarity,
    val iconKey: String,
    val nameEs: String,
    val nameEn: String
)

object CollectibleCatalog {
    private val rarityOrder = listOf(
        Rarity.COMMON, Rarity.COMMON, Rarity.COMMON, Rarity.COMMON,
        Rarity.RARE, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY
    )

    private fun series(s: Series, vararg items: Triple<String, String, String>): List<Collectible> {
        require(items.size == 8)
        return items.mapIndexed { i, (icon, es, en) ->
            Collectible("${s.id}_${i + 1}", s, rarityOrder[i], icon, es, en)
        }
    }

    val all: List<Collectible> = buildList {
        addAll(series(Series.GARDEN,
            Triple("florist", "Margarita", "Daisy"), Triple("spa", "Loto", "Lotus"),
            Triple("eco", "Brote", "Sprout"), Triple("grass", "Trébol", "Clover"),
            Triple("park", "Cerezo", "Cherry tree"), Triple("forest", "Bosque", "Forest"),
            Triple("nature", "Colibrí", "Hummingbird"), Triple("pets", "Zorro de jade", "Jade fox")))
        addAll(series(Series.SKY,
            Triple("sunny", "Amanecer", "Sunrise"), Triple("night", "Luna", "Moon"),
            Triple("cloud", "Nube", "Cloud"), Triple("snow", "Copo", "Snowflake"),
            Triple("air", "Brisa", "Breeze"), Triple("bolt", "Rayo", "Lightning"),
            Triple("public", "Planeta", "Planet"), Triple("rocket", "Cohete", "Rocket")))
        addAll(series(Series.TEA,
            Triple("coffee", "Matcha", "Matcha"), Triple("tea", "Jazmín", "Jasmine"),
            Triple("cake", "Mochi", "Mochi"), Triple("icecream", "Helado", "Ice cream"),
            Triple("cookie", "Galleta", "Cookie"), Triple("lunch", "Bao", "Bao"),
            Triple("bakery", "Pan dulce", "Sweet bread"), Triple("ramen", "Ramen", "Ramen")))
        addAll(series(Series.ADVENTURE,
            Triple("hiking", "Sendero", "Trail"), Triple("sailing", "Velero", "Sailboat"),
            Triple("kayak", "Kayak", "Kayak"), Triple("anchor", "Ancla", "Anchor"),
            Triple("landscape", "Montaña", "Mountain"), Triple("waves", "Ola", "Wave"),
            Triple("explore", "Brújula", "Compass"), Triple("map", "Mapa del tesoro", "Treasure map")))
        addAll(series(Series.STUDIO,
            Triple("brush", "Pincel", "Brush"), Triple("palette", "Paleta", "Palette"),
            Triple("music", "Nota", "Note"), Triple("casino", "Dado", "Die"),
            Triple("extension", "Pieza", "Puzzle piece"), Triple("toys", "Peluche", "Plush"),
            Triple("camera", "Cámara", "Camera"), Triple("piano", "Piano", "Piano")))
        addAll(series(Series.TREASURE,
            Triple("savings", "Alcancía", "Piggy bank"), Triple("toll", "Moneda", "Coin"),
            Triple("key", "Llave", "Key"), Triple("shield", "Escudo", "Shield"),
            Triple("trophy", "Trofeo", "Trophy"), Triple("premium", "Medalla", "Medal"),
            Triple("star", "Estrella fugaz", "Shooting star"), Triple("diamond", "Diamante", "Diamond")))
        addAll(series(Series.OCEAN,
            Triple("set_meal", "Pez payaso", "Clownfish"), Triple("water_drop", "Gota", "Droplet"),
            Triple("bubble_chart", "Burbujas", "Bubbles"), Triple("beach_access", "Sombrilla", "Parasol"),
            Triple("pool", "Poza", "Tide pool"), Triple("surfing", "Surfista", "Surfer"),
            Triple("scuba_diving", "Buzo", "Diver"), Triple("water", "Ballena azul", "Blue whale")))
        addAll(series(Series.FESTIVAL,
            Triple("celebration", "Gorro", "Party hat"), Triple("confirmation_number", "Entrada", "Ticket"),
            Triple("card_giftcard", "Regalo", "Gift"), Triple("fireplace", "Fogata", "Bonfire"),
            Triple("theater_comedy", "Máscara", "Mask"), Triple("attractions", "Rueda de feria", "Ferris wheel"),
            Triple("festival", "Carpa", "Big top"), Triple("flare", "Fuegos artificiales", "Fireworks")))
        addAll(series(Series.MAGIC,
            Triple("nightlight", "Farol", "Lantern"), Triple("emoji_objects", "Lámpara", "Lamp"),
            Triple("local_fire_department", "Vela", "Candle"), Triple("scatter_plot", "Constelación", "Constellation"),
            Triple("auto_stories", "Libro de hechizos", "Spellbook"), Triple("science", "Poción", "Potion"),
            Triple("blur_circular", "Bola de cristal", "Crystal ball"), Triple("auto_fix_high", "Varita estelar", "Star wand")))
    }

    private val byId = all.associateBy { it.id }
    fun byId(id: String): Collectible? = byId[id]
    fun inSeries(s: Series): List<Collectible> = all.filter { it.series == s }
    fun ofRarity(r: Rarity): List<Collectible> = all.filter { it.rarity == r }

    /** Piezas de una serie que el jugador ya tiene. */
    fun progress(s: Series, owned: Set<String>): Pair<Int, Int> =
        inSeries(s).count { it.id in owned } to inSeries(s).size

    fun isSeriesComplete(s: Series, owned: Set<String>): Boolean = inSeries(s).all { it.id in owned }
    fun isAlbumComplete(owned: Set<String>): Boolean = all.all { it.id in owned }
}

// ============================== COFRES ==============================

enum class ChestType(
    val cards: Int,
    /** Pesos de aparición (comunes, raras, épicas, legendarias), sobre 1000. */
    val weights: List<Int>,
    /** Al menos una carta de esta rareza o mejor. */
    val guaranteed: Rarity?
) {
    COMMON(2, listOf(800, 170, 28, 2), null),
    RARE(3, listOf(450, 400, 130, 20), Rarity.RARE),
    EPIC(4, listOf(200, 450, 280, 70), Rarity.EPIC)
}

/** Contadores de "garantía" persistentes: evitan rachas de mala suerte muy largas. */
data class PityState(val sinceEpic: Int = 0, val sinceLegendary: Int = 0)

data class Drop(val collectible: Collectible, val isNew: Boolean, val shards: Int)

data class ChestResult(val drops: List<Drop>, val pity: PityState)

object ChestRules {
    /** Tras tantos cofres sin una épica (o mejor) se fuerza una. */
    const val PITY_EPIC = 12
    /** Tras tantos cofres sin una legendaria se fuerza una. */
    const val PITY_LEGENDARY = 45
    /** Probabilidad de preferir una pieza que aún no tienes (menos repetidas frustrantes). */
    private const val PREFER_NEW = 0.75

    private fun rollRarity(weights: List<Int>, random: Random): Rarity {
        val total = weights.sum()
        var r = random.nextInt(total)
        for (i in weights.indices) {
            if (r < weights[i]) return Rarity.entries[i]
            r -= weights[i]
        }
        return Rarity.COMMON
    }

    /** Elige una pieza de la rareza dada sin repetir las ya sacadas en este mismo cofre ([drawn]). */
    private fun pick(rarity: Rarity, owned: Set<String>, drawn: Set<String>, random: Random): Collectible {
        val pool = CollectibleCatalog.ofRarity(rarity).filter { it.id !in drawn }
            .ifEmpty { CollectibleCatalog.ofRarity(rarity) }
        val unowned = pool.filter { it.id !in owned }
        return if (unowned.isNotEmpty() && random.nextDouble() < PREFER_NEW) unowned.random(random) else pool.random(random)
    }

    private fun atLeast(r: Rarity, min: Rarity) = r.ordinal >= min.ordinal

    fun open(type: ChestType, owned: Set<String>, pity: PityState, random: Random = Random.Default): ChestResult {
        val rarities = MutableList(type.cards) { rollRarity(type.weights, random) }

        // Garantía propia del cofre
        type.guaranteed?.let { min ->
            if (rarities.none { atLeast(it, min) }) rarities[rarities.lastIndex] = min
        }
        // Garantías acumuladas
        if (pity.sinceLegendary + 1 >= PITY_LEGENDARY && rarities.none { it == Rarity.LEGENDARY }) {
            rarities[rarities.lastIndex] = Rarity.LEGENDARY
        } else if (pity.sinceEpic + 1 >= PITY_EPIC && rarities.none { atLeast(it, Rarity.EPIC) }) {
            rarities[rarities.lastIndex] = Rarity.EPIC
        }

        // Se reparten las cartas evitando repetir la misma pieza dentro del mismo cofre
        val ownedNow = owned.toMutableSet()
        val drawn = mutableSetOf<String>()
        val drops = rarities.map { r ->
            val c = pick(r, ownedNow, drawn, random)
            val isNew = c.id !in ownedNow
            ownedNow += c.id
            drawn += c.id
            Drop(c, isNew, if (isNew) 0 else c.rarity.shardValue)
        }

        val gotEpic = drops.any { atLeast(it.collectible.rarity, Rarity.EPIC) }
        val gotLegendary = drops.any { it.collectible.rarity == Rarity.LEGENDARY }
        val newPity = PityState(
            sinceEpic = if (gotEpic) 0 else pity.sinceEpic + 1,
            sinceLegendary = if (gotLegendary) 0 else pity.sinceLegendary + 1
        )
        return ChestResult(drops, newPity)
    }
}

// ============================== CREAR ==============================

object CraftRules {
    fun cost(c: Collectible): Int = c.rarity.craftCost

    /** Se puede crear una pieza que aún no tienes y para la que alcanza la esencia. */
    fun canCraft(c: Collectible, owned: Set<String>, shards: Int): Boolean =
        c.id !in owned && shards >= cost(c)
}


// ============================== BRILLANTES ==============================

/**
 * Una pieza que ya tienes se puede convertir en **Brillante** gastando esencia. Es el destino de las repetidas
 * y da un bono permanente de monedas (ver [AlbumBonus]). Nunca hace falta para completar el álbum.
 */
object FoilRules {
    /** Esencia necesaria: el doble de lo que cuesta crear la pieza. */
    fun cost(c: Collectible): Int = c.rarity.craftCost * 2

    fun canUpgrade(c: Collectible, owned: Set<String>, foil: Set<String>, shards: Int): Boolean =
        c.id in owned && c.id !in foil && shards >= cost(c)

    /** Cada tantas piezas brillantes se suma 1 % de monedas. */
    const val PER_PERCENT = 6
    fun coinPercent(foil: Set<String>): Int = foil.count { CollectibleCatalog.byId(it) != null } / PER_PERCENT
}

/** Esencia a cambio de gemas: un atajo para quien no quiere esperar a las repetidas (sumidero de gemas). */
object ShardShop {
    const val SHARDS_PER_PACK = 100
    const val GEMS_PER_PACK = 25
    /** Packs que se pueden comprar al día (evita vaciar el álbum de golpe). */
    const val MAX_PACKS_PER_DAY = 4

    fun canBuy(gems: Int, boughtToday: Int): Boolean = gems >= GEMS_PER_PACK && boughtToday < MAX_PACKS_PER_DAY
}
