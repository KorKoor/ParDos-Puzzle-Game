package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.economy.Economy

/** FREE = de todos · SHOP = monedas · SEASON = premio del Pase · PRESTIGE = se desbloquea con un rango (o con el Platino). */
enum class AvatarSource { FREE, SHOP, SEASON, PRESTIGE }

enum class AnimalKind { FOX, CAT, PANDA, BUNNY, BEAR, FROG, OWL, PENGUIN, KOALA, RACCOON, CHICK, UNICORN, DRAGON, AXOLOTL, CAPYBARA, TIGER, LION, WOLF, SHEEP, HEDGEHOG, TURTLE,
    PIG, MONKEY, HAMSTER, DEER, COW, DUCK, BAT, GHOST, PUMPKIN, ROBOT, ALIEN, DINO, PHOENIX }

enum class Accessory { NONE, CROWN, HELMET, HEADBAND, MOON, STAR, SCARF, FLOWERS, BOW, GLASSES, WIZARD, HEADPHONES, FLOWER_CROWN,
    WITCH, PIRATE, CHEF, SANTA, DEVIL, HALO, NINJA, TOPHAT, CAP, SUNGLASSES, CAPE, PARTY }

/** Paleta alternativa del animal (los avatares de temporada son versiones especiales). */
enum class AvatarVariant { NORMAL, GOLD, MIDNIGHT, JADE, RAINBOW, SAKURA, FROST, EMBER, SHADOW, CANDY, GALAXY, PLATINUM }

/** Escena de fondo del avatar. AUTO = la que toca por su variante (el degradado de siempre). */
enum class AvatarScene { AUTO, SPARKLES, RAYS, STARS, CLOUDS, HEARTS, SNOW, SPOOKY, PETALS, BUBBLES, CONFETTI, AURORA, FLAMES }

/** Marco que rodea al avatar: sube con el valor del avatar; los de temporada llevan el prisma. */
enum class AvatarFrame { NONE, SOFT, SILVER, GOLD, PRISM, MYTHIC }

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
    val variant: AvatarVariant = AvatarVariant.NORMAL,
    val scene: AvatarScene = AvatarScene.AUTO,
    /** Solo para PRESTIGE: el rango que lo desbloquea (null = el Platino). */
    val unlockRank: com.korkoor.pardos.domain.prestige.PrestigeRank? = null
) {
    /** Rareza para mostrar (color y etiqueta): sale de cómo se consigue y de su precio. */
    val rarity: com.korkoor.pardos.domain.collection.Rarity
        get() = when {
            source == AvatarSource.PRESTIGE -> com.korkoor.pardos.domain.collection.Rarity.LEGENDARY
            source == AvatarSource.SEASON -> com.korkoor.pardos.domain.collection.Rarity.EPIC
            coinPrice >= 3_000 -> com.korkoor.pardos.domain.collection.Rarity.LEGENDARY
            coinPrice >= 2_000 -> com.korkoor.pardos.domain.collection.Rarity.EPIC
            coinPrice >= 1_000 -> com.korkoor.pardos.domain.collection.Rarity.RARE
            else -> com.korkoor.pardos.domain.collection.Rarity.COMMON
        }

    /** De la colección de Noche de brujas (murciélagos, fantasmas, calabazas, brujas y diablillos). */
    val isHalloween: Boolean
        get() = animal in listOf(AnimalKind.BAT, AnimalKind.GHOST, AnimalKind.PUMPKIN) ||
            accessory in listOf(Accessory.WITCH, Accessory.DEVIL, Accessory.CAPE) || scene == AvatarScene.SPOOKY

    val frame: AvatarFrame
        get() = when {
            source == AvatarSource.PRESTIGE -> AvatarFrame.MYTHIC
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

    private fun shop(
        id: Int, name: String, price: Int, animal: AnimalKind, accessory: Accessory = Accessory.NONE,
        variant: AvatarVariant = AvatarVariant.NORMAL, scene: AvatarScene = AvatarScene.AUTO
    ) = AvatarDef(id, name, AvatarSource.SHOP, price, animal, accessory, variant, scene)

    private fun season(
        id: Int, name: String, animal: AnimalKind, accessory: Accessory, variant: AvatarVariant, scene: AvatarScene = AvatarScene.AUTO
    ) = AvatarDef(id, name, AvatarSource.SEASON, 0, animal, accessory, variant, scene)

    private fun prestige(
        id: Int, name: String, rank: com.korkoor.pardos.domain.prestige.PrestigeRank?, animal: AnimalKind, accessory: Accessory,
        variant: AvatarVariant, scene: AvatarScene
    ) = AvatarDef(id, name, AvatarSource.PRESTIGE, 0, animal, accessory, variant, scene, rank)

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
        shop(37, "León", Economy.AVATAR_PRICE_LEGENDARY, AnimalKind.LION, Accessory.CROWN),
        // ---- Noche de brujas
        shop(50, "Murciélago", Economy.AVATAR_PRICE_NICE, AnimalKind.BAT, scene = AvatarScene.SPOOKY),
        shop(51, "Fantasmita", Economy.AVATAR_PRICE_NICE, AnimalKind.GHOST, scene = AvatarScene.SPOOKY),
        shop(52, "Calabaza", Economy.AVATAR_PRICE_NICE, AnimalKind.PUMPKIN, scene = AvatarScene.SPOOKY),
        shop(53, "Gato Bruja", Economy.AVATAR_PRICE_RARE, AnimalKind.CAT, Accessory.WITCH, AvatarVariant.SHADOW, AvatarScene.SPOOKY),
        shop(54, "Conde Murciélago", Economy.AVATAR_PRICE_RARE, AnimalKind.BAT, Accessory.CAPE, AvatarVariant.SHADOW, AvatarScene.SPOOKY),
        shop(55, "Fantasma Mago", Economy.AVATAR_PRICE_RARE, AnimalKind.GHOST, Accessory.WIZARD, AvatarVariant.NORMAL, AvatarScene.SPOOKY),
        shop(56, "Calabaza Diablillo", Economy.AVATAR_PRICE_RARE, AnimalKind.PUMPKIN, Accessory.DEVIL, AvatarVariant.EMBER, AvatarScene.FLAMES),
        shop(58, "Zorro Brujo", Economy.AVATAR_PRICE_RARE, AnimalKind.FOX, Accessory.WITCH, AvatarVariant.NORMAL, AvatarScene.SPOOKY),
        shop(57, "Lobo Sombrío", Economy.AVATAR_PRICE_EPIC, AnimalKind.WOLF, Accessory.NONE, AvatarVariant.SHADOW, AvatarScene.SPOOKY),
        shop(59, "Búho Nocturno", Economy.AVATAR_PRICE_EPIC, AnimalKind.OWL, Accessory.WITCH, AvatarVariant.SHADOW, AvatarScene.STARS),
        shop(60, "Dragón Infernal", Economy.AVATAR_PRICE_LEGENDARY, AnimalKind.DRAGON, Accessory.DEVIL, AvatarVariant.EMBER, AvatarScene.FLAMES),
        // ---- Nuevos amigos
        shop(61, "Cerdito", Economy.AVATAR_PRICE_BASIC, AnimalKind.PIG, Accessory.BOW),
        shop(62, "Patito", Economy.AVATAR_PRICE_BASIC, AnimalKind.DUCK, scene = AvatarScene.BUBBLES),
        shop(63, "Hámster", Economy.AVATAR_PRICE_BASIC, AnimalKind.HAMSTER),
        shop(76, "Rana Gorra", Economy.AVATAR_PRICE_BASIC, AnimalKind.FROG, Accessory.CAP),
        shop(64, "Monito", Economy.AVATAR_PRICE_COZY, AnimalKind.MONKEY),
        shop(65, "Vaquita", Economy.AVATAR_PRICE_COZY, AnimalKind.COW, Accessory.FLOWERS),
        shop(79, "Capi Fiesta", Economy.AVATAR_PRICE_COZY, AnimalKind.CAPYBARA, Accessory.PARTY, scene = AvatarScene.CONFETTI),
        shop(81, "Cerdito Pirata", Economy.AVATAR_PRICE_COZY, AnimalKind.PIG, Accessory.PIRATE),
        shop(84, "Pato Chef", Economy.AVATAR_PRICE_COZY, AnimalKind.DUCK, Accessory.CHEF),
        shop(85, "Vaca DJ", Economy.AVATAR_PRICE_COZY, AnimalKind.COW, Accessory.HEADPHONES),
        shop(66, "Ciervo", Economy.AVATAR_PRICE_NICE, AnimalKind.DEER, Accessory.FLOWER_CROWN, scene = AvatarScene.PETALS),
        shop(67, "Dino", Economy.AVATAR_PRICE_NICE, AnimalKind.DINO),
        shop(71, "Pingüino Pirata", Economy.AVATAR_PRICE_NICE, AnimalKind.PENGUIN, Accessory.PIRATE),
        shop(72, "Oso Chef", Economy.AVATAR_PRICE_NICE, AnimalKind.BEAR, Accessory.CHEF),
        shop(75, "Mapache Cool", Economy.AVATAR_PRICE_NICE, AnimalKind.RACCOON, Accessory.SUNGLASSES),
        shop(80, "Koala Santa", Economy.AVATAR_PRICE_NICE, AnimalKind.KOALA, Accessory.SANTA, scene = AvatarScene.SNOW),
        shop(82, "Hámster Astronauta", Economy.AVATAR_PRICE_NICE, AnimalKind.HAMSTER, Accessory.HELMET, AvatarVariant.MIDNIGHT, AvatarScene.STARS),
        shop(68, "Robot", Economy.AVATAR_PRICE_RARE, AnimalKind.ROBOT, scene = AvatarScene.SPARKLES),
        shop(69, "Alienígena", Economy.AVATAR_PRICE_RARE, AnimalKind.ALIEN, scene = AvatarScene.STARS),
        shop(73, "Panda Ninja", Economy.AVATAR_PRICE_RARE, AnimalKind.PANDA, Accessory.NINJA),
        shop(74, "Gato Elegante", Economy.AVATAR_PRICE_RARE, AnimalKind.CAT, Accessory.TOPHAT),
        shop(77, "Conejo Ángel", Economy.AVATAR_PRICE_RARE, AnimalKind.BUNNY, Accessory.HALO, scene = AvatarScene.CLOUDS),
        shop(78, "Zorro Diablillo", Economy.AVATAR_PRICE_RARE, AnimalKind.FOX, Accessory.DEVIL, AvatarVariant.EMBER, AvatarScene.FLAMES),
        shop(83, "Mono Mago", Economy.AVATAR_PRICE_RARE, AnimalKind.MONKEY, Accessory.WIZARD, scene = AvatarScene.STARS),
        shop(86, "Gatito Sakura", Economy.AVATAR_PRICE_RARE, AnimalKind.CAT, Accessory.BOW, AvatarVariant.SAKURA, AvatarScene.PETALS),
        shop(87, "Conejo Caramelo", Economy.AVATAR_PRICE_RARE, AnimalKind.BUNNY, Accessory.NONE, AvatarVariant.CANDY, AvatarScene.CONFETTI),
        shop(88, "Osito de Hielo", Economy.AVATAR_PRICE_RARE, AnimalKind.BEAR, Accessory.SCARF, AvatarVariant.FROST, AvatarScene.SNOW),
        shop(89, "Pingüino Escarcha", Economy.AVATAR_PRICE_EPIC, AnimalKind.PENGUIN, Accessory.CROWN, AvatarVariant.FROST, AvatarScene.SNOW),
        shop(92, "Tigre Brasa", Economy.AVATAR_PRICE_EPIC, AnimalKind.TIGER, Accessory.NONE, AvatarVariant.EMBER, AvatarScene.FLAMES),
        shop(93, "Ajolote Candy", Economy.AVATAR_PRICE_EPIC, AnimalKind.AXOLOTL, Accessory.PARTY, AvatarVariant.CANDY, AvatarScene.CONFETTI),
        shop(94, "Robot Dorado", Economy.AVATAR_PRICE_EPIC, AnimalKind.ROBOT, Accessory.CROWN, AvatarVariant.GOLD, AvatarScene.SPARKLES),
        shop(95, "Alien Galaxia", Economy.AVATAR_PRICE_EPIC, AnimalKind.ALIEN, Accessory.HELMET, AvatarVariant.GALAXY, AvatarScene.STARS),
        shop(96, "Dino de Jade", Economy.AVATAR_PRICE_EPIC, AnimalKind.DINO, Accessory.CROWN, AvatarVariant.JADE, AvatarScene.BUBBLES),
        shop(70, "Fénix", Economy.AVATAR_PRICE_LEGENDARY, AnimalKind.PHOENIX, Accessory.NONE, AvatarVariant.NORMAL, AvatarScene.FLAMES),
        shop(90, "León Galaxia", Economy.AVATAR_PRICE_LEGENDARY, AnimalKind.LION, Accessory.CROWN, AvatarVariant.GALAXY, AvatarScene.STARS),
        shop(91, "Unicornio Galaxia", Economy.AVATAR_PRICE_LEGENDARY, AnimalKind.UNICORN, Accessory.STAR, AvatarVariant.GALAXY, AvatarScene.AURORA)
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

    /**
     * Más exclusivos del Pase (nunca se venden). Cada temporada reparte cinco de estos: dos en la vía gratis y tres en la premium.
     * El grupo rota entre 3 sets de cinco.
     */
    val passExtras: List<AvatarDef> = listOf(
        season(100, "Fénix Dorado", AnimalKind.PHOENIX, Accessory.CROWN, AvatarVariant.GOLD, AvatarScene.FLAMES),
        season(101, "Gata Bruja Dorada", AnimalKind.CAT, Accessory.WITCH, AvatarVariant.GOLD, AvatarScene.SPOOKY),
        season(102, "Rey Murciélago", AnimalKind.BAT, Accessory.CROWN, AvatarVariant.MIDNIGHT, AvatarScene.SPOOKY),
        season(103, "Robot Arcoíris", AnimalKind.ROBOT, Accessory.STAR, AvatarVariant.RAINBOW, AvatarScene.CONFETTI),
        season(104, "Dino Arcoíris", AnimalKind.DINO, Accessory.PARTY, AvatarVariant.RAINBOW, AvatarScene.CONFETTI),
        season(105, "Ciervo Aurora", AnimalKind.DEER, Accessory.FLOWER_CROWN, AvatarVariant.JADE, AvatarScene.AURORA),
        season(106, "Alien Neón", AnimalKind.ALIEN, Accessory.HEADPHONES, AvatarVariant.GALAXY, AvatarScene.STARS),
        season(107, "Calabaza Real", AnimalKind.PUMPKIN, Accessory.CROWN, AvatarVariant.GOLD, AvatarScene.SPOOKY),
        season(108, "Fantasma Estelar", AnimalKind.GHOST, Accessory.STAR, AvatarVariant.MIDNIGHT, AvatarScene.STARS),
        season(109, "Mono Samurái", AnimalKind.MONKEY, Accessory.HEADBAND, AvatarVariant.EMBER, AvatarScene.FLAMES),
        season(110, "Panda Astronauta", AnimalKind.PANDA, Accessory.HELMET, AvatarVariant.GALAXY, AvatarScene.STARS),
        season(111, "Vaca Estelar", AnimalKind.COW, Accessory.STAR, AvatarVariant.GALAXY, AvatarScene.STARS),
        season(112, "Pato Real", AnimalKind.DUCK, Accessory.CROWN, AvatarVariant.GOLD, AvatarScene.SPARKLES),
        season(113, "Hámster Ninja", AnimalKind.HAMSTER, Accessory.NINJA, AvatarVariant.SHADOW, AvatarScene.SPOOKY),
        season(114, "Ciervo de Invierno", AnimalKind.DEER, Accessory.SCARF, AvatarVariant.FROST, AvatarScene.SNOW)
    )

    /** Se ganan subiendo de rango de prestigio (y el último, con el Platino). Se conceden solos al llegar. */
    val prestige: List<AvatarDef> = listOf(
        prestige(120, "Gatito Aprendiz", com.korkoor.pardos.domain.prestige.PrestigeRank.APPRENTICE, AnimalKind.CAT, Accessory.CAP, AvatarVariant.NORMAL, AvatarScene.SPARKLES),
        prestige(121, "Búho Adepto", com.korkoor.pardos.domain.prestige.PrestigeRank.ADEPT, AnimalKind.OWL, Accessory.GLASSES, AvatarVariant.JADE, AvatarScene.STARS),
        prestige(122, "Lobo Experto", com.korkoor.pardos.domain.prestige.PrestigeRank.EXPERT, AnimalKind.WOLF, Accessory.SUNGLASSES, AvatarVariant.NORMAL, AvatarScene.RAYS),
        prestige(123, "Panda Maestro", com.korkoor.pardos.domain.prestige.PrestigeRank.MASTER, AnimalKind.PANDA, Accessory.HEADBAND, AvatarVariant.GOLD, AvatarScene.RAYS),
        prestige(124, "Dragón Gran Maestro", com.korkoor.pardos.domain.prestige.PrestigeRank.GRANDMASTER, AnimalKind.DRAGON, Accessory.CROWN, AvatarVariant.JADE, AvatarScene.AURORA),
        prestige(125, "Fénix de la Leyenda", com.korkoor.pardos.domain.prestige.PrestigeRank.LEGEND, AnimalKind.PHOENIX, Accessory.CROWN, AvatarVariant.GOLD, AvatarScene.RAYS),
        prestige(126, "Unicornio Mítico", com.korkoor.pardos.domain.prestige.PrestigeRank.MYTHIC, AnimalKind.UNICORN, Accessory.CROWN, AvatarVariant.GALAXY, AvatarScene.AURORA),
        prestige(127, "Zorro Platino", null, AnimalKind.FOX, Accessory.HALO, AvatarVariant.PLATINUM, AvatarScene.AURORA)
    )

    val all: List<AvatarDef> = classics + shop.sortedBy { it.id } + seasonal + passExtras + prestige

    /** Avatar extra de la vía del Pase [slot] (0..4) de [seasonId]. */
    fun seasonExtra(seasonId: Int, slot: Int): AvatarDef = passExtras[(seasonId.mod(3)) * 5 + slot.mod(5)]

    /** Avatares de prestigio que ya tocan con [rank] (y el del Platino si [platinum]). */
    fun prestigeUnlocked(rank: com.korkoor.pardos.domain.prestige.PrestigeRank, platinum: Boolean): List<AvatarDef> =
        prestige.filter { val r = it.unlockRank; if (r == null) platinum else rank.ordinal >= r.ordinal }

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
