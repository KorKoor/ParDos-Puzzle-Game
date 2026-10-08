package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.economy.Economy

enum class AvatarSource { FREE, SHOP, SEASON }

enum class AnimalKind { FOX, CAT, PANDA, BUNNY, BEAR, FROG, OWL, PENGUIN, KOALA, RACCOON, CHICK, UNICORN, DRAGON, AXOLOTL, CAPYBARA, TIGER, LION, WOLF, SHEEP, HEDGEHOG, TURTLE }

enum class Accessory { NONE, CROWN, HELMET, HEADBAND, MOON, STAR, SCARF, FLOWERS, BOW, GLASSES, WIZARD, HEADPHONES, FLOWER_CROWN }

/** Paleta alternativa del animal (los avatares de temporada son versiones especiales). */
enum class AvatarVariant { NORMAL, GOLD, MIDNIGHT, JADE, RAINBOW }

/** Marco que rodea al avatar: sube con el valor del avatar; los de temporada llevan el prisma. */
enum class AvatarFrame { NONE, SOFT, SILVER, GOLD, PRISM }

/**
 * Un avatar. Los ids 1..10 son los clásicos (imágenes de la app) y siempre son gratis. Del 11 en adelante son
 * animalitos que la app dibuja: [animal], [accessory] y [variant] dicen cómo.
 */
data class AvatarDef(
    val id: Int,
    val name: String,
    val source: AvatarSource,
    val coinPrice: Int = 0,
    val animal: AnimalKind? = null,
    val accessory: Accessory = Accessory.NONE,
    val variant: AvatarVariant = AvatarVariant.NORMAL
) {
    val frame: AvatarFrame
        get() = when {
            source == AvatarSource.SEASON -> AvatarFrame.PRISM
            animal == null -> AvatarFrame.NONE
            coinPrice >= 2_500 -> AvatarFrame.GOLD
            coinPrice >= 1_500 -> AvatarFrame.SILVER
            else -> AvatarFrame.SOFT
        }
}

object Avatars {
    const val CLASSIC_COUNT = 10
    const val DEFAULT_ID = 1

    private fun shop(id: Int, name: String, price: Int, animal: AnimalKind, accessory: Accessory = Accessory.NONE) =
        AvatarDef(id, name, AvatarSource.SHOP, price, animal, accessory)

    private fun season(id: Int, name: String, animal: AnimalKind, accessory: Accessory, variant: AvatarVariant) =
        AvatarDef(id, name, AvatarSource.SEASON, 0, animal, accessory, variant)

    val classics: List<AvatarDef> = (1..CLASSIC_COUNT).map { AvatarDef(it, "Clásico $it", AvatarSource.FREE) }

    val shop: List<AvatarDef> = listOf(
        shop(21, "Pollito", Economy.AVATAR_PRICE_BASIC, AnimalKind.CHICK),
        shop(16, "Ranita", Economy.AVATAR_PRICE_BASIC, AnimalKind.FROG, Accessory.FLOWERS),
        shop(11, "Zorro", Economy.AVATAR_PRICE_COZY, AnimalKind.FOX),
        shop(12, "Gatito", Economy.AVATAR_PRICE_COZY, AnimalKind.CAT),
        shop(14, "Conejito", Economy.AVATAR_PRICE_COZY, AnimalKind.BUNNY),
        shop(15, "Osito", Economy.AVATAR_PRICE_COZY, AnimalKind.BEAR, Accessory.SCARF),
        shop(18, "Pingüino", Economy.AVATAR_PRICE_NICE, AnimalKind.PENGUIN, Accessory.SCARF),
        shop(19, "Koala", Economy.AVATAR_PRICE_NICE, AnimalKind.KOALA),
        shop(20, "Mapache", Economy.AVATAR_PRICE_NICE, AnimalKind.RACCOON),
        shop(13, "Panda", Economy.AVATAR_PRICE_RARE, AnimalKind.PANDA, Accessory.FLOWERS),
        shop(17, "Búho", Economy.AVATAR_PRICE_RARE, AnimalKind.OWL),
        shop(22, "Unicornio", Economy.AVATAR_PRICE_EPIC, AnimalKind.UNICORN),
        shop(23, "Dragón", Economy.AVATAR_PRICE_LEGENDARY, AnimalKind.DRAGON),
        shop(30, "Tortuguita", Economy.AVATAR_PRICE_BASIC, AnimalKind.TURTLE),
        shop(31, "Ovejita", Economy.AVATAR_PRICE_COZY, AnimalKind.SHEEP, Accessory.BOW),
        shop(32, "Erizo", Economy.AVATAR_PRICE_COZY, AnimalKind.HEDGEHOG),
        shop(39, "Zorro Estudioso", Economy.AVATAR_PRICE_NICE, AnimalKind.FOX, Accessory.GLASSES),
        shop(40, "Conejo DJ", Economy.AVATAR_PRICE_NICE, AnimalKind.BUNNY, Accessory.HEADPHONES),
        shop(35, "Lobo", Economy.AVATAR_PRICE_NICE, AnimalKind.WOLF),
        shop(33, "Capibara", Economy.AVATAR_PRICE_RARE, AnimalKind.CAPYBARA, Accessory.FLOWER_CROWN),
        shop(34, "Ajolote", Economy.AVATAR_PRICE_RARE, AnimalKind.AXOLOTL),
        shop(38, "Gato Mago", Economy.AVATAR_PRICE_RARE, AnimalKind.CAT, Accessory.WIZARD),
        shop(36, "Tigre", Economy.AVATAR_PRICE_EPIC, AnimalKind.TIGER),
        shop(37, "León", Economy.AVATAR_PRICE_LEGENDARY, AnimalKind.LION, Accessory.CROWN)
    ).sortedBy { it.coinPrice }

    /** Exclusivos del Pase de temporada, por pares: [par*2] sale en la vía gratis y [par*2+1] en la premium. */
    val seasonal: List<AvatarDef> = listOf(
        season(24, "Zorro Dorado", AnimalKind.FOX, Accessory.CROWN, AvatarVariant.GOLD),
        season(25, "Gato Astronauta", AnimalKind.CAT, Accessory.HELMET, AvatarVariant.MIDNIGHT),
        season(26, "Panda Samurái", AnimalKind.PANDA, Accessory.HEADBAND, AvatarVariant.NORMAL),
        season(27, "Conejo Lunar", AnimalKind.BUNNY, Accessory.MOON, AvatarVariant.MIDNIGHT),
        season(28, "Dragón de Jade", AnimalKind.DRAGON, Accessory.CROWN, AvatarVariant.JADE),
        season(29, "Unicornio Arcoíris", AnimalKind.UNICORN, Accessory.STAR, AvatarVariant.RAINBOW),
        season(41, "Capibara Zen", AnimalKind.CAPYBARA, Accessory.FLOWER_CROWN, AvatarVariant.JADE),
        season(42, "León Dorado", AnimalKind.LION, Accessory.CROWN, AvatarVariant.GOLD),
        season(43, "Ajolote Arcoíris", AnimalKind.AXOLOTL, Accessory.BOW, AvatarVariant.RAINBOW),
        season(44, "Lobo Nocturno", AnimalKind.WOLF, Accessory.MOON, AvatarVariant.MIDNIGHT)
    )

    val all: List<AvatarDef> = classics + shop.sortedBy { it.id } + seasonal

    fun byId(id: Int): AvatarDef = all.firstOrNull { it.id == id } ?: classics.first()

    fun exists(id: Int): Boolean = all.any { it.id == id }

    /** Los clásicos son de todos; los demás hay que tenerlos. */
    fun isOwned(id: Int, owned: Set<Int>): Boolean = id in 1..CLASSIC_COUNT || id in owned

    /** Avatar que da la vía GRATIS del pase de [seasonId] (rota cada mes entre los pares). */
    fun seasonFree(seasonId: Int): AvatarDef = seasonal[(seasonId.mod(seasonal.size / 2)) * 2]

    /** Avatar que da la vía PREMIUM del pase de [seasonId]. */
    fun seasonPremium(seasonId: Int): AvatarDef = seasonal[(seasonId.mod(seasonal.size / 2)) * 2 + 1]

    sealed interface Purchase {
        data class Ok(val coinsLeft: Int) : Purchase
        data object AlreadyOwned : Purchase
        data object NotEnoughCoins : Purchase
        /** Es del pase de temporada o ya no existe: no se vende. */
        data object NotPurchasable : Purchase
    }

    fun buy(id: Int, owned: Set<Int>, coins: Int): Purchase {
        val def = all.firstOrNull { it.id == id } ?: return Purchase.NotPurchasable
        if (isOwned(id, owned)) return Purchase.AlreadyOwned
        if (def.source != AvatarSource.SHOP) return Purchase.NotPurchasable
        if (coins < def.coinPrice) return Purchase.NotEnoughCoins
        return Purchase.Ok(coins - def.coinPrice)
    }
}
