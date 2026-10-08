package com.korkoor.pardos.domain.collection

import kotlin.random.Random

// ============================== COPIAS ==============================

/**
 * El álbum guarda cuántas copias tienes de cada pieza. La primera abre la pieza en el álbum; las demás son
 * **repetidas**: se pueden vender, reciclar en esencia o intercambiar con amigos.
 */
object Copies {
    fun owned(copies: Map<String, Int>): Set<String> = copies.filterValues { it >= 1 }.keys.filter { CollectibleCatalog.byId(it) != null }.toSet()

    fun count(copies: Map<String, Int>, id: String): Int = copies[id] ?: 0

    /** Copias de más (la primera se queda siempre en el álbum). */
    fun spare(copies: Map<String, Int>, id: String): Int = (count(copies, id) - 1).coerceAtLeast(0)

    fun spares(copies: Map<String, Int>): Map<String, Int> =
        copies.mapValues { spare(copies, it.key) }.filterValues { it > 0 }.filterKeys { CollectibleCatalog.byId(it) != null }

    fun totalSpares(copies: Map<String, Int>): Int = spares(copies).values.sum()

    fun add(copies: Map<String, Int>, id: String, n: Int = 1): Map<String, Int> =
        if (n == 0) copies else copies + (id to (count(copies, id) + n).coerceAtLeast(0))

    /** Pasa de la lista antigua (solo "la tienes / no la tienes") a copias: 1 por pieza. */
    fun fromOwnedSet(owned: Set<String>): Map<String, Int> = owned.associateWith { 1 }

    fun encode(copies: Map<String, Int>): String = copies.filterValues { it > 0 }.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decode(raw: String?): Map<String, Int> =
        if (raw.isNullOrBlank()) emptyMap()
        else raw.split(',').mapNotNull {
            val (k, v) = it.split(':').takeIf { p -> p.size == 2 } ?: return@mapNotNull null
            v.toIntOrNull()?.takeIf { n -> n > 0 }?.let { n -> k to n }
        }.toMap()
}

// ============================== MEJORAS ==============================

/** Mejora de una pieza Épica o Legendaria: [tenths] son décimas de punto (5 = 0,5 %). */
data class Perk(val kind: PerkKind, val tenths: Int) {
    val percent: Double get() = tenths / 10.0

    fun label(foil: Boolean = false): String {
        val t = if (foil) tenths * 2 else tenths
        val v = if (t % 10 == 0) "${t / 10}" else "${t / 10},${t % 10}"
        return when (kind) {
            PerkKind.COINS -> "+$v % de monedas"
            PerkKind.XP -> "+$v % de experiencia"
            PerkKind.SELL -> "+$v % al vender repetidas"
            PerkKind.LUCK -> "+$v % de carta extra en cada cofre"
            PerkKind.TOKENS -> "+$v % de ficha extra al día"
        }
    }
}

/**
 * Las piezas de alta rareza dan una mejora PERMANENTE (la del tipo de su serie). La versión Brillante la duplica.
 * Cada tipo tiene un tope para que el juego no se desbalancee.
 */
object PerkRules {
    private fun base(kind: PerkKind): Int = when (kind) {
        PerkKind.COINS -> 3
        PerkKind.XP -> 5
        PerkKind.SELL -> 10
        PerkKind.LUCK -> 5
        PerkKind.TOKENS -> 20
    }

    /** Topes en décimas de punto. */
    fun cap(kind: PerkKind): Int = when (kind) {
        PerkKind.COINS -> 150
        PerkKind.XP -> 200
        PerkKind.SELL -> 400
        PerkKind.LUCK -> 250
        PerkKind.TOKENS -> 500
    }

    fun perkOf(c: Collectible): Perk? = when (c.rarity) {
        Rarity.EPIC -> Perk(c.series.perk, base(c.series.perk))
        Rarity.LEGENDARY -> Perk(c.series.perk, base(c.series.perk) * 2)
        else -> null
    }

    /** Suma de mejoras por tipo (décimas), con topes. */
    fun totals(owned: Set<String>, foil: Set<String> = emptySet()): Map<PerkKind, Int> {
        val sums = PerkKind.entries.associateWith { 0 }.toMutableMap()
        for (id in owned) {
            val c = CollectibleCatalog.byId(id) ?: continue
            val perk = c.perk ?: continue
            sums[perk.kind] = (sums[perk.kind] ?: 0) + perk.tenths * (if (id in foil) 2 else 1)
        }
        return sums.mapValues { (k, v) -> v.coerceAtMost(cap(k)) }
    }

    fun tenths(kind: PerkKind, owned: Set<String>, foil: Set<String> = emptySet()): Int = totals(owned, foil)[kind] ?: 0

    /** Puntos porcentuales enteros de monedas extra por mejoras. */
    fun coinPercent(owned: Set<String>, foil: Set<String> = emptySet()): Int = tenths(PerkKind.COINS, owned, foil) / 10

    /** Multiplicador de XP (1.0 = sin mejora). */
    fun xpMultiplier(owned: Set<String>, foil: Set<String> = emptySet()): Double = 1.0 + tenths(PerkKind.XP, owned, foil) / 1000.0

    /** Probabilidad (0..1) de carta extra en cada cofre. */
    fun extraCardChance(owned: Set<String>, foil: Set<String> = emptySet()): Double = tenths(PerkKind.LUCK, owned, foil) / 1000.0

    /** Probabilidad (0..1) de ficha de intercambio extra cada día. */
    fun dailyTokenChance(owned: Set<String>, foil: Set<String> = emptySet()): Double = tenths(PerkKind.TOKENS, owned, foil) / 1000.0
}

// ============================== VENTA Y RECICLAJE ==============================

object SellRules {
    /** Monedas por vender una copia repetida, con la mejora de venta ([sellTenths] = décimas de punto). */
    fun value(c: Collectible, sellTenths: Int = 0): Int = (c.rarity.sellCoins * (1000 + sellTenths) / 1000.0).toInt()

    fun canSell(copies: Map<String, Int>, id: String, qty: Int = 1): Boolean = qty >= 1 && Copies.spare(copies, id) >= qty

    /** Esencia por reciclar una copia repetida. */
    fun recycleValue(c: Collectible): Int = c.rarity.shardValue

    /** Todas las repetidas de una rareza (o de menor) para vender de golpe: nunca toca la primera copia. */
    fun bulk(copies: Map<String, Int>, upTo: Rarity): List<Pair<Collectible, Int>> =
        Copies.spares(copies).mapNotNull { (id, n) ->
            val c = CollectibleCatalog.byId(id) ?: return@mapNotNull null
            if (c.rarity.ordinal <= upTo.ordinal) c to n else null
        }
}

// ============================== INTERCAMBIO ==============================

/** Ofrecer tu pieza [give] a cambio de [want] (misma rareza). */
data class TradeOffer(val give: String, val want: String)

data class TradeSuggestion(val give: Collectible, val want: Collectible)

object TradeRules {
    const val EXPIRE_DAYS = 7
    const val MAX_PENDING_OUT = 5

    /** Fichas que cuesta proponer un intercambio de piezas de esa rareza. */
    fun cost(r: Rarity): Int = r.tradeTokens

    sealed interface Check {
        data object Ok : Check
        data object SameRarityOnly : Check
        data object NoSpareToGive : Check
        data object NotEnoughTokens : Check
        data object Unknown : Check
        data object SamePiece : Check
        data object TooManyPending : Check
    }

    fun canPropose(copies: Map<String, Int>, tokens: Int, offer: TradeOffer, pendingOut: Int = 0): Check {
        val give = CollectibleCatalog.byId(offer.give)
        val want = CollectibleCatalog.byId(offer.want)
        if (give == null || want == null) return Check.Unknown
        if (give.id == want.id) return Check.SamePiece
        if (give.rarity != want.rarity) return Check.SameRarityOnly
        if (Copies.spare(copies, give.id) < 1) return Check.NoSpareToGive
        if (pendingOut >= MAX_PENDING_OUT) return Check.TooManyPending
        if (tokens < cost(give.rarity)) return Check.NotEnoughTokens
        return Check.Ok
    }

    /** Quien recibe la propuesta entrega [TradeOffer.want]: necesita una repetida de esa pieza. */
    fun canAccept(copies: Map<String, Int>, offer: TradeOffer): Boolean {
        val give = CollectibleCatalog.byId(offer.give) ?: return false
        val want = CollectibleCatalog.byId(offer.want) ?: return false
        return give.rarity == want.rarity && give.id != want.id && Copies.spare(copies, want.id) >= 1
    }

    /**
     * Intercambios que les convienen a los dos: tú das una repetida que tu amigo no tiene y él te da una repetida
     * que a ti te falta, de la misma rareza. Primero las más raras.
     */
    fun suggestions(
        myCopies: Map<String, Int>, theirSpares: Set<String>, theirOwned: Set<String>, limit: Int = 12
    ): List<TradeSuggestion> {
        val myOwned = Copies.owned(myCopies)
        val wants = theirSpares.mapNotNull { CollectibleCatalog.byId(it) }.filter { it.id !in myOwned }
        val gives = Copies.spares(myCopies).keys.mapNotNull { CollectibleCatalog.byId(it) }.filter { it.id !in theirOwned }
        val out = mutableListOf<TradeSuggestion>()
        for (r in Rarity.entries.reversed()) {
            val w = wants.filter { it.rarity == r }
            val g = gives.filter { it.rarity == r }.toMutableList()
            for (want in w) {
                val give = g.firstOrNull() ?: break
                g.remove(give)
                out += TradeSuggestion(give, want)
            }
        }
        return out.take(limit)
    }
}

/** Fichas de intercambio: de dónde salen y cuánto cuestan. */
object TokenRules {
    const val DAILY_LOGIN = 1
    const val ALL_DAILY_MISSIONS = 1
    const val WEEKLY_MISSION = 1
    const val RANK_UP = 3
    const val PLATINUM = 10
    const val MAX_STOCK = 40

    const val GEMS_PER_TOKEN = 15
    const val MAX_BOUGHT_PER_DAY = 3

    fun canBuy(gems: Int, boughtToday: Int, stock: Int): Boolean = gems >= GEMS_PER_TOKEN && boughtToday < MAX_BOUGHT_PER_DAY && stock < MAX_STOCK
}

// ============================== SOBRE DE SERIE ==============================

/**
 * Sobre de una serie concreta: 3 cartas de esa serie. Si todavía te falta alguna, **siempre** trae al menos una nueva.
 * Cuesta monedas (o gemas) y sirve para rematar series que casi completas.
 */
object SeriesPackRules {
    const val CARDS = 3
    const val COINS = 900
    const val GEMS = 45
    private val weights = listOf(450, 400, 130, 20)

    fun canOpen(series: Series, owned: Set<String>): Boolean = !CollectibleCatalog.isSeriesComplete(series, owned)

    fun open(series: Series, owned: Set<String>, random: Random = Random.Default): List<Drop> {
        val all = CollectibleCatalog.inSeries(series)
        fun roll(): Rarity {
            var r = random.nextInt(weights.sum())
            for (i in weights.indices) { if (r < weights[i]) return Rarity.entries[i]; r -= weights[i] }
            return Rarity.COMMON
        }
        val ownedNow = owned.toMutableSet()
        val drawn = mutableSetOf<String>()
        val picks = MutableList(CARDS) { roll() }
        val result = mutableListOf<Drop>()
        for ((i, r) in picks.withIndex()) {
            val pool = all.filter { it.rarity == r && it.id !in drawn }.ifEmpty { all.filter { it.id !in drawn } }
            var c = pool.random(random)
            // la última carta es siempre una pieza nueva si aún falta alguna y no ha salido ninguna
            val noNewYet = result.none { it.isNew }
            if (i == picks.lastIndex && noNewYet) {
                val missing = all.filter { it.id !in ownedNow && it.id !in drawn }
                if (missing.isNotEmpty()) c = missing.filter { it.rarity.ordinal <= r.ordinal }.ifEmpty { missing }.random(random)
            }
            val isNew = c.id !in ownedNow
            ownedNow += c.id; drawn += c.id
            result += Drop(c, isNew, if (isNew) 0 else c.rarity.shardValue)
        }
        return result
    }
}

// ============================== VITRINA ==============================

/** Piezas que exhibes en tu perfil. Empiezas con 3 huecos y puedes abrir más con gemas. */
object Showcase {
    const val BASE_SLOTS = 3
    const val MAX_SLOTS = 9

    /** Gemas para abrir el hueco número [slot] (4.º..9.º). */
    fun unlockCost(slot: Int): Int = when (slot) {
        4 -> 30
        5 -> 50
        6 -> 80
        7 -> 120
        8 -> 160
        9 -> 220
        else -> 0
    }

    fun sanitize(ids: List<String>, owned: Set<String>, slots: Int): List<String> =
        ids.filter { it in owned && CollectibleCatalog.byId(it) != null }.distinct().take(slots.coerceIn(BASE_SLOTS, MAX_SLOTS))
}

// ============================== PISTAS ==============================

/** Pistas tipo "te falta 1": la serie más cerca de completarse y cómo conseguir lo que falta. */
data class AlbumHint(val series: Series, val missing: List<Collectible>, val owned: Int) {
    val total: Int get() = owned + missing.size
}

object AlbumHints {
    /** Series incompletas ordenadas por lo poco que les falta (y, a igualdad, por cuánto valen). */
    fun closest(owned: Set<String>, limit: Int = 3): List<AlbumHint> =
        Series.entries.mapNotNull { s ->
            val items = CollectibleCatalog.inSeries(s)
            val missing = items.filter { it.id !in owned }
            if (missing.isEmpty() || missing.size == items.size) null else AlbumHint(s, missing, items.size - missing.size)
        }.sortedWith(compareBy<AlbumHint> { it.missing.size }.thenByDescending { it.series.rewardCoins }).take(limit)
}

// ============================== ÁLBUM COMPACTO ==============================

/**
 * Un conjunto de piezas en pocas letras (un bit por pieza del catálogo, en hexadecimal): así tu perfil puede decir
 * qué tienes y qué te sobra sin pesar casi nada. El orden es el del catálogo, que solo crece por el final.
 */
object AlbumBits {
    fun encode(ids: Collection<String>): String {
        val set = ids.toHashSet()
        val sb = StringBuilder()
        val all = CollectibleCatalog.all
        var i = 0
        while (i < all.size) {
            var nibble = 0
            for (b in 0 until 4) {
                val idx = i + b
                if (idx < all.size && all[idx].id in set) nibble = nibble or (1 shl b)
            }
            sb.append("0123456789abcdef"[nibble])
            i += 4
        }
        return sb.toString()
    }

    fun decode(raw: String?): Set<String> {
        if (raw.isNullOrEmpty()) return emptySet()
        val all = CollectibleCatalog.all
        val out = HashSet<String>()
        for ((pos, ch) in raw.withIndex()) {
            val nibble = "0123456789abcdef".indexOf(ch.lowercaseChar())
            if (nibble < 0) continue
            for (b in 0 until 4) {
                val idx = pos * 4 + b
                if (idx < all.size && (nibble and (1 shl b)) != 0) out += all[idx].id
            }
        }
        return out
    }
}
