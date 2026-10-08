package com.korkoor.pardos.domain.shop

/** Estilos visuales de las fichas. El dibujo vive en la app; aquí solo reglas y precios. */
enum class TileSkin(
    val id: String,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0
) {
    JELLY("jelly"),                       // el original, gratis
    FLAT("flat", coinPrice = 150),
    WOOD("wood", coinPrice = 300),
    GLASS("glass", coinPrice = 500),
    NEON("neon", gemPrice = 8);

    val isFree: Boolean get() = coinPrice == 0 && gemPrice == 0

    companion object {
        val DEFAULT = JELLY

        fun fromId(id: String?): TileSkin = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** Estado de skins del jugador (puro, sin guardado): sirve para razonar compras y equipado. */
data class SkinInventory(
    val owned: Set<String> = setOf(TileSkin.DEFAULT.id),
    val equipped: String = TileSkin.DEFAULT.id
) {
    fun owns(skin: TileSkin): Boolean = skin.isFree || skin.id in owned

    sealed interface Purchase {
        data class Ok(val inventory: SkinInventory, val coinsLeft: Int, val gemsLeft: Int) : Purchase
        data object AlreadyOwned : Purchase
        data object NotEnoughCoins : Purchase
        data object NotEnoughGems : Purchase
    }

    /** Intenta comprar [skin] con el saldo dado. No modifica nada si no se puede. */
    fun buy(skin: TileSkin, coins: Int, gems: Int): Purchase = when {
        owns(skin) -> Purchase.AlreadyOwned
        coins < skin.coinPrice -> Purchase.NotEnoughCoins
        gems < skin.gemPrice -> Purchase.NotEnoughGems
        else -> Purchase.Ok(
            inventory = copy(owned = owned + skin.id),
            coinsLeft = coins - skin.coinPrice,
            gemsLeft = gems - skin.gemPrice
        )
    }

    /** Equipa una skin solo si ya es del jugador. */
    fun equip(skin: TileSkin): SkinInventory = if (owns(skin)) copy(equipped = skin.id) else this
}
