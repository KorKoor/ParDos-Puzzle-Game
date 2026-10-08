package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.economy.Economy
import kotlin.math.ceil
import kotlin.math.ln

/** De dónde sale un efecto de fusión. */
enum class FxSource { FREE, SHOP, STARTER, SEASON }

/**
 * Efectos de fusión: lo que sale volando cuando dos fichas se juntan. Son solo cosméticos (no cambian la jugabilidad).
 * [id] se guarda en disco, no lo cambies. El dibujo vive en la app (`MergeFxCanvas`).
 */
enum class MergeFx(
    val id: String,
    val displayName: String,
    val blurb: String,
    val rarity: Rarity,
    val coinPrice: Int,
    val gemPrice: Int,
    val source: FxSource
) {
    CLASSIC("classic", "Clásico", "Solo el rebote de gelatina de siempre.", Rarity.COMMON, 0, 0, FxSource.FREE),
    SPARKS("sparks", "Chispas", "Destellos dorados que saltan de la ficha.", Rarity.COMMON, Economy.MERGE_FX_PRICE_COMMON, 0, FxSource.SHOP),
    BUBBLES("bubbles", "Burbujas", "Burbujitas que suben y se deshacen.", Rarity.COMMON, Economy.MERGE_FX_PRICE_COMMON, 0, FxSource.SHOP),
    PETALS("petals", "Pétalos", "Una lluvia de pétalos de cerezo.", Rarity.RARE, Economy.MERGE_FX_PRICE_RARE, 0, FxSource.SHOP),
    HEARTS("hearts", "Corazones", "Corazoncitos que flotan hacia arriba.", Rarity.RARE, Economy.MERGE_FX_PRICE_RARE, 0, FxSource.STARTER),
    CONFETTI("confetti", "Confeti", "Fiesta en cada fusión.", Rarity.RARE, Economy.MERGE_FX_PRICE_RARE, 0, FxSource.SHOP),
    STARS("stars", "Estrellas", "Estrellitas que salen disparadas.", Rarity.EPIC, 0, Economy.MERGE_FX_GEMS_EPIC, FxSource.SHOP),
    RIPPLE("ripple", "Ondas", "Ondas de luz que se expanden como en un estanque.", Rarity.EPIC, 0, Economy.MERGE_FX_GEMS_EPIC, FxSource.SHOP),
    LIGHTNING("lightning", "Rayo", "Un relámpago morado cruza la ficha.", Rarity.LEGENDARY, 0, Economy.MERGE_FX_GEMS_LEGENDARY, FxSource.SEASON),
    FIREWORKS("fireworks", "Fuegos artificiales", "Estallidos de colores como en Año Nuevo.", Rarity.LEGENDARY, 0, Economy.MERGE_FX_GEMS_LEGENDARY, FxSource.SHOP);

    val isFree: Boolean get() = coinPrice == 0 && gemPrice == 0 && source == FxSource.FREE

    /** Se puede comprar en la tienda con monedas o gemas. */
    val buyable: Boolean get() = source == FxSource.SHOP

    companion object {
        val DEFAULT = CLASSIC
        fun fromId(id: String?): MergeFx = entries.firstOrNull { it.id == id } ?: DEFAULT

        /** Cuántas partículas suelta una fusión: crece (despacio) con el valor de la ficha. Siempre entre 8 y 26. */
        fun particleCount(tileValue: Int): Int {
            val v = tileValue.coerceAtLeast(2)
            val steps = (ln(v.toDouble()) / ln(2.0)).toInt()   // 2→1, 4→2, 8→3 ...
            return (6 + steps * 2).coerceIn(8, 26)
        }
    }
}

/** Efectos que el jugador tiene y el que lleva puesto. */
data class MergeFxInventory(
    val owned: Set<String> = setOf(MergeFx.DEFAULT.id),
    val equipped: String = MergeFx.DEFAULT.id
) {
    fun owns(fx: MergeFx): Boolean = fx.isFree || fx.id in owned

    sealed interface Purchase {
        data class Ok(val inventory: MergeFxInventory, val coinsLeft: Int, val gemsLeft: Int) : Purchase
        data object AlreadyOwned : Purchase
        data object NotEnoughCoins : Purchase
        data object NotEnoughGems : Purchase
        /** Solo se consigue con el pack inicial o el pase. */
        data object NotPurchasable : Purchase
    }

    fun buy(fx: MergeFx, coins: Int, gems: Int, discountPercent: Int = 0): Purchase {
        if (owns(fx)) return Purchase.AlreadyOwned
        if (!fx.buyable) return Purchase.NotPurchasable
        val factor = (100 - discountPercent.coerceIn(0, 90)) / 100.0
        val coinCost = ceil(fx.coinPrice * factor).toInt()
        val gemCost = ceil(fx.gemPrice * factor).toInt()
        return when {
            coins < coinCost -> Purchase.NotEnoughCoins
            gems < gemCost -> Purchase.NotEnoughGems
            else -> Purchase.Ok(copy(owned = owned + fx.id), coins - coinCost, gems - gemCost)
        }
    }

    fun grant(fx: MergeFx): MergeFxInventory = copy(owned = owned + fx.id)

    /** Equipa un efecto solo si es del jugador; si no, no cambia nada. */
    fun equip(fx: MergeFx): MergeFxInventory = if (owns(fx)) copy(equipped = fx.id) else this
}
