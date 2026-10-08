package com.korkoor.pardos.domain.collection

import kotlin.random.Random

enum class Rarity(val dropWeightOrder: Int, val shardValue: Int, val craftCost: Int, val sellCoins: Int, val tradeTokens: Int) {
    COMMON(0, 10, 40, 12, 1),
    RARE(1, 25, 100, 40, 2),
    EPIC(2, 60, 250, 130, 4),
    LEGENDARY(3, 150, 600, 420, 8)
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

/** [shards] = lo que daría reciclarla; la repetida se guarda como copia (vender, intercambiar o reciclar). [bonus] = carta extra de la suerte. */
data class Drop(val collectible: Collectible, val isNew: Boolean, val shards: Int, val bonus: Boolean = false)

data class ChestResult(val drops: List<Drop>, val pity: PityState)

object ChestRules {
    /** Tras tantos cofres sin una épica (o mejor) se fuerza una. */
    const val PITY_EPIC = 12
    /** Tras tantos cofres sin una legendaria se fuerza una. */
    const val PITY_LEGENDARY = 45
    /** Probabilidad de preferir una pieza que aún no tienes. Con 320 piezas algunas repetidas son parte del juego (se venden o se intercambian). */
    private const val PREFER_NEW = 0.6

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

    /**
     * [extraCardChance] (0..1) = probabilidad de una carta extra gratis (mejora de suerte, ver [PerkRules]).
     */
    fun open(
        type: ChestType, owned: Set<String>, pity: PityState, random: Random = Random.Default,
        extraCardChance: Double = 0.0
    ): ChestResult {
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

        // Carta extra de la suerte (después de las garantías, para que no las pise)
        val withBonus = extraCardChance > 0.0 && random.nextDouble() < extraCardChance
        if (withBonus) rarities += rollRarity(type.weights, random)

        // Se reparten las cartas evitando repetir la misma pieza dentro del mismo cofre
        val ownedNow = owned.toMutableSet()
        val drawn = mutableSetOf<String>()
        val drops = rarities.mapIndexed { idx, r ->
            val c = pick(r, ownedNow, drawn, random)
            val isNew = c.id !in ownedNow
            ownedNow += c.id
            drawn += c.id
            Drop(c, isNew, if (isNew) 0 else c.rarity.shardValue, bonus = withBonus && idx == rarities.lastIndex)
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
    const val PER_PERCENT = 16
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
