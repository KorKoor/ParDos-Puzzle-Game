package com.korkoor.pardos.domain.collection

/** Dibujo de fondo de una serie (la app lo pinta con Canvas detrás de cada carta). */
enum class SeriesMotif { DOTS, RAYS, WAVES, STARS, LEAVES, SNOW, DIAMONDS, BUBBLES, HILLS, EMBERS, RAIN, HEARTS, CONFETTI, STRIPES, CRYSTALS, CHECKER }

/** Mejora que dan las piezas Épicas y Legendarias de una serie (ver [PerkRules]). */
enum class PerkKind(val labelEs: String) {
    COINS("monedas"),
    XP("experiencia"),
    SELL("al vender repetidas"),
    LUCK("cartas extra en cofres"),
    TOKENS("fichas de intercambio")
}

/**
 * Series coleccionables (32). Cada una tiene 10 piezas: 4 comunes, 3 raras, 2 épicas y 1 legendaria.
 * [glyph] es el emoji de reserva y [top]/[bottom]/[accent] pintan el fondo de sus cartas.
 */
enum class Series(
    val id: String, val nameEs: String, val nameEn: String,
    val rewardCoins: Int, val rewardGems: Int,
    val glyph: String, val top: Long, val bottom: Long, val accent: Long,
    val motif: SeriesMotif, val perk: PerkKind
) {
    GARDEN("garden", "Jardín Zen", "Zen Garden", 400, 5, "🌱", 0xFFD9F2E3, 0xFF9ED8B5, 0xFF3E8A5A, SeriesMotif.LEAVES, PerkKind.COINS),
    SKY("sky", "Cielo", "Sky", 400, 5, "☁️", 0xFFCDE8FF, 0xFFE9F4FF, 0xFF5D9BD9, SeriesMotif.RAYS, PerkKind.XP),
    TEA("tea", "Mesa de Té", "Tea Table", 400, 5, "🍵", 0xFFF3E6CF, 0xFFD9B98C, 0xFF8A5A2B, SeriesMotif.CHECKER, PerkKind.SELL),
    ADVENTURE("adventure", "Aventura", "Adventure", 600, 8, "⛺", 0xFFFFE3B8, 0xFFE8A55A, 0xFF8A4A16, SeriesMotif.HILLS, PerkKind.LUCK),
    STUDIO("studio", "Estudio", "Studio", 600, 8, "🎨", 0xFFFFE0F0, 0xFFD9C2FF, 0xFF8E5BD0, SeriesMotif.CONFETTI, PerkKind.XP),
    TREASURE("treasure", "Tesoros", "Treasures", 900, 15, "💎", 0xFFFFF0B8, 0xFFE8B83A, 0xFF8A5F0C, SeriesMotif.DIAMONDS, PerkKind.COINS),
    OCEAN("ocean", "Mar Profundo", "Deep Sea", 700, 10, "🐚", 0xFFB8E6F5, 0xFF2F7FA8, 0xFF174A6B, SeriesMotif.BUBBLES, PerkKind.SELL),
    FESTIVAL("festival", "Fiesta", "Festival", 700, 10, "🎉", 0xFFFFD6E5, 0xFFFFB84A, 0xFFD6336C, SeriesMotif.CONFETTI, PerkKind.TOKENS),
    MAGIC("magic", "Noche Mágica", "Magic Night", 1_000, 18, "✨", 0xFF2B1B5E, 0xFF5A3A9E, 0xFFFFD36E, SeriesMotif.STARS, PerkKind.LUCK),
    FARM("farm", "Granja Feliz", "Happy Farm", 500, 6, "🌾", 0xFFFFF3C4, 0xFFC9E8A0, 0xFF6B8E23, SeriesMotif.HILLS, PerkKind.COINS),
    JUNGLE("jungle", "Selva", "Jungle", 600, 8, "🌴", 0xFFBDEBB0, 0xFF3E9A55, 0xFF1D5A30, SeriesMotif.LEAVES, PerkKind.XP),
    ARCTIC("arctic", "Ártico", "Arctic", 600, 8, "❄️", 0xFFE6F4FF, 0xFFA8D4F2, 0xFF3F7DB8, SeriesMotif.SNOW, PerkKind.SELL),
    DESERT("desert", "Desierto", "Desert", 600, 8, "🐫", 0xFFFFE2A8, 0xFFE59A4A, 0xFF8A4A16, SeriesMotif.DOTS, PerkKind.COINS),
    SPACE("space", "Espacio", "Space", 800, 12, "🛸", 0xFF120A3A, 0xFF3A2A8F, 0xFF8EC9FF, SeriesMotif.STARS, PerkKind.XP),
    CANDY("candy", "Dulcería", "Candy Shop", 600, 8, "🍬", 0xFFFFE0F1, 0xFFFFB3D6, 0xFFE0559A, SeriesMotif.STRIPES, PerkKind.TOKENS),
    FRUIT("fruit", "Huerto", "Orchard", 500, 6, "🍎", 0xFFFFE9D6, 0xFFFFB38A, 0xFFD6402B, SeriesMotif.DOTS, PerkKind.SELL),
    ORCHESTRA("orchestra", "Orquesta", "Orchestra", 700, 10, "🎼", 0xFFE8E0FF, 0xFFB8A6F0, 0xFF5B3FB8, SeriesMotif.WAVES, PerkKind.XP),
    SPORTS("sports", "Deportes", "Sports", 600, 8, "⚽", 0xFFD6F5D0, 0xFF6BC76B, 0xFF1F7A2E, SeriesMotif.STRIPES, PerkKind.LUCK),
    TRANSPORT("transport", "Transportes", "Transport", 600, 8, "🚦", 0xFFFFE8E0, 0xFFFFA58A, 0xFFD6402B, SeriesMotif.CHECKER, PerkKind.TOKENS),
    KINGDOM("kingdom", "Reino Encantado", "Enchanted Kingdom", 900, 14, "🏰", 0xFFF3E0FF, 0xFFC59BFF, 0xFF6A3FB8, SeriesMotif.DIAMONDS, PerkKind.COINS),
    HALLOWEEN("halloween", "Noche de Brujas", "Witching Night", 1_000, 18, "🎃", 0xFF1B0F2E, 0xFF6B2D7A, 0xFFFF8A1F, SeriesMotif.EMBERS, PerkKind.LUCK),
    CHRISTMAS("christmas", "Invierno Mágico", "Winter Magic", 800, 12, "🎄", 0xFF0F3A2A, 0xFF2E7A55, 0xFFFFD36E, SeriesMotif.SNOW, PerkKind.TOKENS),
    FUTURE("future", "Futuro", "Future", 800, 12, "🤖", 0xFF0A1F2B, 0xFF0E5A66, 0xFF5CF2D0, SeriesMotif.CRYSTALS, PerkKind.XP),
    DINO("dino", "Dinosaurios", "Dinosaurs", 700, 10, "🦖", 0xFFE3F0C8, 0xFF9ACD5A, 0xFF3F6B22, SeriesMotif.LEAVES, PerkKind.COINS),
    BUGS("bugs", "Bichos del Jardín", "Garden Bugs", 500, 6, "🐞", 0xFFFFF0B8, 0xFFB8E06B, 0xFF4F7A12, SeriesMotif.DOTS, PerkKind.SELL),
    BIRDS("birds", "Aves", "Birds", 600, 8, "🦅", 0xFFD8EEFF, 0xFF8EC9F0, 0xFF2F6FA8, SeriesMotif.WAVES, PerkKind.LUCK),
    MYTH("myth", "Criaturas Míticas", "Mythic Creatures", 1_000, 18, "🦄", 0xFFF0DCFF, 0xFFB57CF0, 0xFF6A2FB8, SeriesMotif.CRYSTALS, PerkKind.XP),
    WORLDFOOD("worldfood", "Cocina del Mundo", "World Kitchen", 600, 8, "🍽️", 0xFFFFE9C9, 0xFFF2A65A, 0xFFB8531F, SeriesMotif.CHECKER, PerkKind.SELL),
    LANDMARKS("landmarks", "Maravillas del Mundo", "World Wonders", 800, 12, "🧳", 0xFFE0F0FF, 0xFFF2D6A8, 0xFF8A5F0C, SeriesMotif.HILLS, PerkKind.COINS),
    SUMMER("summer", "Verano", "Summer", 600, 8, "🏖️", 0xFFFFF0B8, 0xFF6BD0F0, 0xFFF29A1F, SeriesMotif.WAVES, PerkKind.TOKENS),
    TEMPLE("temple", "Templo Zen", "Zen Temple", 800, 12, "⛩️", 0xFFFFE3E0, 0xFFE05A5A, 0xFF8A1F1F, SeriesMotif.RAIN, PerkKind.LUCK),
    PETS("pets", "Mascotas", "Pets", 500, 6, "🐾", 0xFFFFF0E0, 0xFFFFC98A, 0xFFB8641F, SeriesMotif.HEARTS, PerkKind.TOKENS)
}

/** Una pieza del álbum. [glyph] es el emoji que la ilustra; el fondo sale de su serie. */
data class Collectible(
    val id: String,
    val series: Series,
    val rarity: Rarity,
    val glyph: String,
    val nameEs: String,
    val nameEn: String,
    val descEs: String
) {
    /** Posición dentro de la serie (1..10). */
    val number: Int get() = id.substringAfterLast('_').toIntOrNull() ?: 0

    /** Mejora que da (solo Épicas y Legendarias). */
    val perk: Perk? get() = PerkRules.perkOf(this)
}

object CollectibleCatalog {
    /** Rareza por posición dentro de la serie: 4 comunes, 3 raras (5, 6, 9), 2 épicas (7, 10) y 1 legendaria (8). */
    private val rarityOrder = listOf(
        Rarity.COMMON, Rarity.COMMON, Rarity.COMMON, Rarity.COMMON,
        Rarity.RARE, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY, Rarity.RARE, Rarity.EPIC
    )
    const val PIECES_PER_SERIES = 10

    private class P(val glyph: String, val es: String, val en: String, val desc: String)

    private fun p(glyph: String, es: String, en: String, desc: String) = P(glyph, es, en, desc)

    private fun series(s: Series, vararg items: P): List<Collectible> {
        require(items.size == PIECES_PER_SERIES) { "${s.id}: ${items.size} piezas" }
        return items.mapIndexed { i, it -> Collectible("${s.id}_${i + 1}", s, rarityOrder[i], it.glyph, it.es, it.en, it.desc) }
    }

    val all: List<Collectible> = buildList {
        addAll(series(Series.GARDEN,
            p("🌼", "Margarita", "Daisy", "Se abre con el primer sol y nunca tiene prisa."),
            p("🌺", "Loto", "Lotus", "Flota en calma aunque el agua se mueva."),
            p("🌱", "Brote", "Sprout", "Todo gran jardín empezó con una semillita."),
            p("🍀", "Trébol", "Clover", "Dicen que trae suerte a quien se detiene a mirarlo."),
            p("🌸", "Cerezo", "Cherry tree", "Sus pétalos caen como una lluvia lenta y rosa."),
            p("🌲", "Bosque", "Forest", "Mil árboles respirando al mismo tiempo."),
            p("🐦", "Colibrí", "Hummingbird", "Su aleteo es tan rápido que parece quedarse quieto."),
            p("🦊", "Zorro de jade", "Jade fox", "Guardián del jardín: solo aparece cuando todo está en paz."),
            p("🦋", "Mariposa", "Butterfly", "Cada ala es un pequeño vitral."),
            p("⛲", "Fuente de la calma", "Calm fountain", "El agua cuenta historias a quien sabe escuchar.")))
        addAll(series(Series.SKY,
            p("🌅", "Amanecer", "Sunrise", "El cielo estrena colores cada mañana."),
            p("🌙", "Luna", "Moon", "Vigila tus sueños sin hacer ruido."),
            p("☁️", "Nube", "Cloud", "Viaja sin maleta y sin destino."),
            p("❄️", "Copo", "Snowflake", "No hay dos iguales, igual que tú."),
            p("💨", "Brisa", "Breeze", "Se lleva las preocupaciones de paso."),
            p("⚡", "Rayo", "Lightning", "Un segundo de luz que lo cambia todo."),
            p("🌍", "Planeta", "Planet", "Visto desde arriba, todo cabe en una sola mirada."),
            p("🚀", "Cohete", "Rocket", "Cuenta hasta tres y deja el suelo atrás."),
            p("🌈", "Arcoíris", "Rainbow", "Aparece justo cuando dejas de buscarlo."),
            p("🌠", "Lluvia de estrellas", "Meteor shower", "Pide un deseo… o varios, esta noche sobran.")))
        addAll(series(Series.TEA,
            p("🍵", "Matcha", "Matcha", "Verde, espumoso y perfecto para frenar el día."),
            p("☕", "Jazmín", "Jasmine", "Un aroma que huele a tarde tranquila."),
            p("🍡", "Mochi", "Mochi", "Suave por fuera, dulce por dentro."),
            p("🍦", "Helado", "Ice cream", "Se derrite rápido: disfrútalo ya."),
            p("🍪", "Galleta", "Cookie", "Una sola nunca es suficiente."),
            p("🥠", "Galleta de la fortuna", "Fortune cookie", "Rómpela con cuidado: el mensaje es para ti."),
            p("🥐", "Pan dulce", "Sweet bread", "Recién horneado y todavía tibio."),
            p("🍜", "Ramen", "Ramen", "Un caldo que abraza desde dentro."),
            p("🍰", "Pastel", "Cake", "Para celebrar algo, o para no celebrar nada."),
            p("🍯", "Miel de azahar", "Blossom honey", "Dorada como una tarde de verano.")))
        addAll(series(Series.ADVENTURE,
            p("🥾", "Sendero", "Trail", "Cada paso abre un camino nuevo."),
            p("⛵", "Velero", "Sailboat", "El viento decide, tú solo sonríes."),
            p("🛶", "Kayak", "Kayak", "Remar en silencio también es una aventura."),
            p("⚓", "Ancla", "Anchor", "Para quedarte cuando el lugar vale la pena."),
            p("⛰️", "Montaña", "Mountain", "No importa la altura: importa llegar a la cima."),
            p("🌊", "Ola", "Wave", "Fuerte, constante y siempre vuelve."),
            p("🧭", "Brújula", "Compass", "Siempre señala hacia donde quieres ir."),
            p("🗺️", "Mapa del tesoro", "Treasure map", "La X marca el lugar… y también el principio."),
            p("🏕️", "Campamento", "Campsite", "Una fogata, un cielo enorme y buena compañía."),
            p("🎒", "Mochila del explorador", "Explorer's pack", "Lleva poco, pero justo lo necesario.")))
        addAll(series(Series.STUDIO,
            p("🖌️", "Pincel", "Brush", "Una pincelada y el lienzo cobra vida."),
            p("🎨", "Paleta", "Palette", "Todos los colores caben si los mezclas bien."),
            p("🎵", "Nota", "Note", "Sola es breve; en compañía, canción."),
            p("🎲", "Dado", "Die", "Un poco de azar le hace bien a cualquier plan."),
            p("🧩", "Pieza", "Puzzle piece", "Parece que sobra, hasta que encaja."),
            p("🧸", "Peluche", "Plush", "Guarda más recuerdos que una caja fuerte."),
            p("📷", "Cámara", "Camera", "Congela un instante para siempre."),
            p("🎹", "Piano", "Piano", "Ochenta y ocho teclas, infinitas historias."),
            p("🎬", "Claqueta", "Clapperboard", "¡Luces, cámara… y a por la siguiente toma!"),
            p("🎤", "Micrófono de oro", "Golden mic", "Hasta el susurro más bajo suena enorme aquí.")))
        addAll(series(Series.TREASURE,
            p("🐷", "Alcancía", "Piggy bank", "Se llena despacito, moneda a moneda."),
            p("💰", "Bolsa de oro", "Gold pouch", "Pesa lo justo para alegrarte el día."),
            p("🔑", "Llave", "Key", "Siempre abre algo, aunque no sepas qué."),
            p("🛡️", "Escudo", "Shield", "Resiste todo menos las ganas de seguir."),
            p("🏆", "Trofeo", "Trophy", "Brilla más cuando lo ganas jugando limpio."),
            p("🏅", "Medalla", "Medal", "Una pequeña prueba de que lo lograste."),
            p("⭐", "Estrella fugaz", "Shooting star", "Dura un parpadeo, se recuerda toda la vida."),
            p("💎", "Diamante", "Diamond", "Tantos años de presión para brillar así."),
            p("💍", "Anillo antiguo", "Ancient ring", "Perteneció a alguien que sabía guardar secretos."),
            p("🗝️", "Llave maestra", "Master key", "Abre cualquier cofre… menos el de los recuerdos.")))
        addAll(series(Series.OCEAN,
            p("🐠", "Pez payaso", "Clownfish", "Vive feliz entre anémonas que a otros les pican."),
            p("💧", "Gota", "Droplet", "Pequeña, pero sin ella no habría mar."),
            p("🐡", "Pez globo", "Pufferfish", "Cuando se asusta, se hace gigante."),
            p("⛱️", "Sombrilla", "Parasol", "Un rincón de sombra frente a la inmensidad."),
            p("🦀", "Cangrejo", "Crab", "Camina de lado y llega igual."),
            p("🏄", "Surfista", "Surfer", "Cabalga la ola sin pedir permiso."),
            p("🦈", "Tiburón", "Shark", "Elegante y silencioso: el dueño del azul."),
            p("🐋", "Ballena azul", "Blue whale", "Su canto cruza océanos enteros."),
            p("🐙", "Pulpo", "Octopus", "Ocho brazos y una gran inteligencia."),
            p("🦑", "Calamar luminoso", "Glowing squid", "Enciende su propia luz en la oscuridad del fondo.")))
        addAll(series(Series.FESTIVAL,
            p("🎉", "Confeti", "Confetti", "Hace llover alegría sobre cualquier día gris."),
            p("🎟️", "Entrada", "Ticket", "El pase para la mejor noche del año."),
            p("🎁", "Regalo", "Gift", "Lo mejor es adivinar qué hay dentro."),
            p("🔥", "Fogata", "Bonfire", "Todos se juntan alrededor, nadie se queda afuera."),
            p("🎭", "Máscara", "Mask", "Detrás de ella, cualquiera puede ser otra persona."),
            p("🎡", "Rueda de feria", "Ferris wheel", "Sube despacio y regala la mejor vista."),
            p("🎪", "Carpa", "Big top", "Dentro de ella ocurre la magia."),
            p("🎆", "Fuegos artificiales", "Fireworks", "Un estallido que ilumina todos los rostros."),
            p("🎈", "Globo", "Balloon", "Nunca te sueltes de la ilusión."),
            p("🎠", "Carrusel", "Carousel", "Da vueltas y vueltas… y siempre quieres una más.")))
        addAll(series(Series.MAGIC,
            p("🏮", "Farol", "Lantern", "Ilumina el camino sin quemar la noche."),
            p("💡", "Lámpara", "Lamp", "Una idea brillante encendida a tiempo."),
            p("🕯️", "Vela", "Candle", "Pequeña llama, enorme compañía."),
            p("🌌", "Constelación", "Constellation", "Unir puntos también puede ser un arte."),
            p("📖", "Libro de hechizos", "Spellbook", "Sus páginas susurran cuando nadie mira."),
            p("🧪", "Poción", "Potion", "Burbujea con colores que no existen."),
            p("🔮", "Bola de cristal", "Crystal ball", "Muestra el futuro… a quien sabe esperar."),
            p("🌟", "Varita estelar", "Star wand", "Cada destello concede un pequeño milagro."),
            p("🦉", "Búho sabio", "Wise owl", "Lo ha visto todo y aun así sigue curioso."),
            p("🧙", "Mago de la noche", "Night wizard", "Sabe un truco para cada estrella.")))
        addAll(series(Series.FARM,
            p("🐔", "Gallina", "Hen", "Pone un huevo y lo anuncia como si fuera una hazaña."),
            p("🐄", "Vaca", "Cow", "Calma absoluta, leche fresca."),
            p("🐖", "Cerdito", "Piglet", "Feliz entre el lodo y los abrazos."),
            p("🐑", "Oveja", "Sheep", "Contar ovejas jamás fue tan agradable."),
            p("🐎", "Caballo", "Horse", "Corre al ritmo del viento."),
            p("🚜", "Tractor", "Tractor", "Pequeño motor, enormes cosechas."),
            p("🌽", "Maíz de oro", "Golden corn", "Cada grano brilla como un tesoro."),
            p("🐓", "Gallo de oro", "Golden rooster", "Despierta al sol antes que el sol."),
            p("🐣", "Pollito recién nacido", "Newborn chick", "Todavía trae un pedacito de cáscara en la cabeza."),
            p("🌾", "Campo de trigo", "Wheat field", "Se mece como un mar dorado.")))
        addAll(series(Series.JUNGLE,
            p("🐒", "Mono", "Monkey", "Siempre tiene tiempo para una travesura."),
            p("🦜", "Loro", "Parrot", "Repite lo que oye, pero inventa lo que siente."),
            p("🐍", "Serpiente", "Snake", "Silenciosa, elegante y muy respetada."),
            p("🐸", "Rana", "Frog", "Un salto, un croar y la lluvia llega."),
            p("🐘", "Elefante", "Elephant", "Nunca olvida a quien lo cuidó."),
            p("🦍", "Gorila", "Gorilla", "Fuerte por fuera, tierno por dentro."),
            p("🦏", "Rinoceronte", "Rhino", "Una armadura viviente con corazón noble."),
            p("🐆", "Jaguar espíritu", "Spirit jaguar", "Se mueve entre las sombras sin romper una hoja."),
            p("🦒", "Jirafa", "Giraffe", "Ve el mundo desde donde nadie más llega."),
            p("🐅", "Tigre de la selva", "Jungle tiger", "Su rugido hace temblar la espesura.")))
        addAll(series(Series.ARCTIC,
            p("🐧", "Pingüino", "Penguin", "Camina como si llevara smoking a una fiesta."),
            p("⛄", "Muñeco de nieve", "Snowman", "Sonríe aunque sepa que el sol se acerca."),
            p("🎿", "Esquí", "Ski", "Deslizarse es la forma más bonita de bajar."),
            p("🛷", "Trineo", "Sled", "Cuesta abajo y con risas aseguradas."),
            p("🐻", "Oso polar", "Polar bear", "Su pelaje parece blanco, pero guarda todos los colores del hielo."),
            p("🦌", "Reno", "Reindeer", "Guía a los demás con su paso firme sobre la nieve."),
            p("🏔️", "Glaciar", "Glacier", "Un río de hielo que avanza sin prisa."),
            p("🐲", "Dragón de hielo", "Ice dragon", "Su aliento congela hasta los pensamientos."),
            p("🧤", "Guantes de lana", "Wool gloves", "Tejidos con cariño para dedos fríos."),
            p("💠", "Cristal de escarcha", "Frost crystal", "Se forma una vez cada mil inviernos.")))
        addAll(series(Series.DESERT,
            p("🌵", "Cactus", "Cactus", "Pincha por fuera, guarda agua por dentro."),
            p("🐪", "Camello", "Camel", "Cruza arenas infinitas sin quejarse."),
            p("🦂", "Escorpión", "Scorpion", "Pequeño, atento y siempre listo."),
            p("🦎", "Lagartija", "Lizard", "Toma el sol y no se preocupa por nada."),
            p("🏜️", "Dunas", "Dunes", "Cambian de forma cada vez que miras hacia otro lado."),
            p("⛺", "Tienda beduina", "Bedouin tent", "Un hogar que se lleva a cuestas."),
            p("🏺", "Ánfora", "Amphora", "Guarda historias más antiguas que el desierto."),
            p("🗿", "Esfinge", "Sphinx", "Hace una sola pregunta y espera mil años."),
            p("🦅", "Águila del desierto", "Desert eagle", "Dibuja círculos perfectos sobre el calor."),
            p("🧞", "Genio de la lámpara", "Lamp genie", "Concede tres deseos y, a veces, un consejo.")))
        addAll(series(Series.SPACE,
            p("🛰️", "Satélite", "Satellite", "Da vueltas al mundo sin cansarse."),
            p("☄️", "Cometa", "Comet", "Pasa una vez y deja una estela inolvidable."),
            p("👽", "Alienígena", "Alien", "Viene en son de paz… y con muchas preguntas."),
            p("🔭", "Telescopio", "Telescope", "Acerca lo que parece imposible."),
            p("☀️", "Sol", "Sun", "La estrella que nos hace madrugar con gusto."),
            p("🌕", "Luna llena", "Full moon", "Redonda, brillante y un poco misteriosa."),
            p("🛸", "OVNI", "UFO", "Nadie lo ha visto… pero todos lo buscan."),
            p("👨‍🚀", "Astronauta", "Astronaut", "Flota entre estrellas con una sonrisa dentro del casco."),
            p("🌎", "Planeta azul", "Blue planet", "Un puntito azul que lo contiene todo."),
            p("🕳️", "Agujero negro", "Black hole", "Se lo traga todo, hasta la luz.")))
        addAll(series(Series.CANDY,
            p("🍭", "Paleta de caramelo", "Lollipop", "Girar, lamer, sonreír."),
            p("🍫", "Chocolate", "Chocolate", "Una barrita y el mundo mejora."),
            p("🍬", "Caramelo", "Candy", "Envuelto en papel brillante, escondido en tu bolsillo."),
            p("🍩", "Dona", "Doughnut", "El agujero también es parte del encanto."),
            p("🧁", "Cupcake", "Cupcake", "Una pequeña torre de azúcar y felicidad."),
            p("🍮", "Flan", "Flan", "Tiembla de emoción cada vez que lo sirven."),
            p("🎂", "Pastel de cumpleaños", "Birthday cake", "Pide un deseo antes de soplar."),
            p("🍨", "Gran helado", "Grand sundae", "Tres bolas, crema y una cereza al final."),
            p("🥧", "Pay de manzana", "Apple pie", "Huele a hogar y a domingo."),
            p("🍿", "Palomitas de caramelo", "Candy popcorn", "Crujen, brillan y se acaban demasiado pronto.")))
        addAll(series(Series.FRUIT,
            p("🍎", "Manzana", "Apple", "Una al día mantiene la sonrisa."),
            p("🍌", "Plátano", "Banana", "Viene en su propio envoltorio ecológico."),
            p("🍇", "Uvas", "Grapes", "Siempre llegan en racimo… y en buena compañía."),
            p("🍓", "Fresa", "Strawberry", "Pequeña, roja y tentadora."),
            p("🍑", "Durazno", "Peach", "Suave como un abrazo de verano."),
            p("🍍", "Piña", "Pineapple", "Lleva corona, pero es muy amistosa."),
            p("🍉", "Sandía", "Watermelon", "Un picnic entero en una sola fruta."),
            p("🍏", "Manzana dorada", "Golden apple", "Dicen que quien la prueba nunca olvida su sabor."),
            p("🥝", "Kiwi", "Kiwi", "Peludo por fuera, esmeralda por dentro."),
            p("🥑", "Aguacate", "Avocado", "Siempre listo… justo cuando ya se pasó.")))
        addAll(series(Series.ORCHESTRA,
            p("🎸", "Guitarra", "Guitar", "Seis cuerdas y una historia por contar."),
            p("🥁", "Tambor", "Drum", "Marca el latido de la fiesta."),
            p("🎺", "Trompeta", "Trumpet", "Anuncia que algo importante comienza."),
            p("🎻", "Violín", "Violin", "Hace llorar y sonreír en la misma nota."),
            p("🎷", "Saxofón", "Saxophone", "Suena a noche de ciudad y luces doradas."),
            p("🎧", "Audífonos", "Headphones", "Un mundo privado entre tus oídos."),
            p("🎼", "Partitura", "Sheet music", "Todo concierto empieza con una hoja de papel."),
            p("📀", "Disco de oro", "Golden record", "Solo los grandes éxitos llegan a brillar así."),
            p("📻", "Radio antigua", "Old radio", "Todavía guarda las canciones de otra época."),
            p("🎶", "Coro celestial", "Heavenly choir", "Mil voces que respiran como una sola.")))
        addAll(series(Series.SPORTS,
            p("⚽", "Balón", "Football", "Rueda, rebota y reúne a todo el barrio."),
            p("🏀", "Baloncesto", "Basketball", "Un salto, un giro y canasta."),
            p("🎾", "Tenis", "Tennis", "Va y viene, y siempre cuenta."),
            p("⚾", "Béisbol", "Baseball", "Nueve entradas de pura emoción."),
            p("🏈", "Fútbol americano", "American football", "Forma ovalada, impulso imparable."),
            p("🏐", "Voleibol", "Volleyball", "No dejes que toque el suelo."),
            p("🏊", "Natación", "Swimming", "Cada brazada es una pequeña victoria."),
            p("🥇", "Medalla de oro", "Gold medal", "Lo que se logra con constancia y un poquito de magia."),
            p("🥊", "Guante de boxeo", "Boxing glove", "Para luchar con respeto y cabeza fría."),
            p("🚴", "Ciclismo", "Cycling", "Pedalea hasta que el horizonte te alcance.")))
        addAll(series(Series.TRANSPORT,
            p("🚗", "Auto", "Car", "Cuatro ruedas y todas las carreteras por delante."),
            p("🚲", "Bicicleta", "Bicycle", "Dos ruedas, cero prisa."),
            p("🚌", "Autobús", "Bus", "Siempre hay un asiento junto a la ventana."),
            p("🚕", "Taxi", "Taxi", "Te lleva a donde sea, con historias de regalo."),
            p("🚂", "Tren de vapor", "Steam train", "Su silbato suena a viaje largo."),
            p("🚁", "Helicóptero", "Helicopter", "Despega sin pista y aterriza donde importa."),
            p("✈️", "Avión", "Airplane", "Une dos mundos en una sola tarde."),
            p("🚄", "Tren bala", "Bullet train", "Tan rápido que el paisaje se vuelve acuarela."),
            p("🛵", "Scooter", "Scooter", "Pequeño, ágil y siempre a la moda."),
            p("🚢", "Crucero", "Cruise ship", "Una ciudad flotante rumbo al horizonte.")))
        addAll(series(Series.KINGDOM,
            p("🗡️", "Espada", "Sword", "Más valiente cuando no hace falta usarla."),
            p("🏹", "Arco", "Bow", "Un buen tiro empieza con respirar hondo."),
            p("🏇", "Jinete", "Rider", "Galopa al amanecer con el estandarte al viento."),
            p("🔔", "Campana", "Bell", "Anuncia buenas noticias a todo el pueblo."),
            p("🏰", "Castillo", "Castle", "Torres altas y cuentos que nunca terminan."),
            p("🧚", "Hada", "Fairy", "Un polvito de brillo y todo es posible."),
            p("👸", "Princesa", "Princess", "Su mayor tesoro es su valentía."),
            p("🐉", "Dragón", "Dragon", "Guarda el reino… y su colección de monedas."),
            p("👑", "Corona", "Crown", "Pesa más por la responsabilidad que por el oro."),
            p("🤴", "Príncipe", "Prince", "Aprendió que gobernar es escuchar.")))
        addAll(series(Series.HALLOWEEN,
            p("🎃", "Calabaza", "Pumpkin", "Se ríe con la luz de una vela dentro."),
            p("👻", "Fantasma", "Ghost", "Más tímido que aterrador."),
            p("🦇", "Murciélago", "Bat", "Duerme de día, baila de noche."),
            p("🕷️", "Araña", "Spider", "Teje obras de arte con paciencia infinita."),
            p("🕸️", "Telaraña", "Cobweb", "Adorna cada rincón del castillo olvidado."),
            p("💀", "Calavera", "Skull", "Sonríe aun sin labios."),
            p("🧛", "Vampiro", "Vampire", "Elegante, pálido y puntual a la medianoche."),
            p("🧙‍♀️", "Bruja de la luna", "Moon witch", "Cabalga entre nubes con su gato y su escoba."),
            p("⚗️", "Caldero burbujeante", "Bubbling cauldron", "Huele raro, pero la receta es secreta."),
            p("🧟", "Zombi", "Zombie", "Camina lento, pero nunca se pierde una fiesta.")))
        addAll(series(Series.CHRISTMAS,
            p("🎄", "Árbol", "Tree", "Cada esfera cuenta un recuerdo."),
            p("🎅", "Santa", "Santa", "Reparte regalos y carcajadas."),
            p("🧦", "Calcetín", "Stocking", "Cuelga de la chimenea esperando sorpresas."),
            p("🧣", "Bufanda", "Scarf", "Tres vueltas al cuello y a disfrutar del frío."),
            p("⛷️", "Esquiador", "Skier", "Baja entre pinos blancos como una pluma."),
            p("⛸️", "Patinaje", "Ice skating", "Figuras de hielo, risas de invierno."),
            p("🤶", "Señora Claus", "Mrs. Claus", "La que de verdad dirige la fábrica de juguetes."),
            p("🎇", "Luces de invierno", "Winter lights", "Hacen que la noche más larga parezca de fiesta."),
            p("🥛", "Leche tibia", "Warm milk", "Va perfecta con una galleta junto al fuego."),
            p("🏡", "Cabaña nevada", "Snowy cabin", "Humea la chimenea y todo huele a canela.")))
        addAll(series(Series.FUTURE,
            p("🤖", "Robot", "Robot", "Obedece órdenes, pero ya sueña con bailar."),
            p("💻", "Laptop", "Laptop", "Una ventana a todo lo que pueda imaginarse."),
            p("📱", "Teléfono", "Phone", "Cabe en la mano, conecta al planeta."),
            p("🔋", "Batería", "Battery", "Energía en reserva para el siguiente gran paso."),
            p("📡", "Antena", "Antenna", "Escucha señales que nadie más oye."),
            p("🎮", "Videojuego", "Video game", "Un mundo entero detrás de un botón."),
            p("🕹️", "Arcade", "Arcade", "Fichas, luces y récords imposibles."),
            p("🧠", "Mente artificial", "Artificial mind", "Piensa tan rápido que a veces se sorprende."),
            p("🔬", "Microscopio", "Microscope", "Descubre universos dentro de una gota."),
            p("🧲", "Imán cuántico", "Quantum magnet", "Atrae lo que parecía imposible juntar.")))
        addAll(series(Series.DINO,
            p("🥚", "Huevo fósil", "Fossil egg", "Esperó sesenta millones de años para abrirse."),
            p("🦴", "Hueso", "Bone", "Una pista enorme sobre un gigante."),
            p("🌿", "Helecho", "Fern", "El desayuno favorito de los grandes."),
            p("🌋", "Volcán", "Volcano", "Despierta con un bostezo de fuego."),
            p("🦕", "Diplodocus", "Diplodocus", "Cuello largo, paso tranquilo, corazón enorme."),
            p("🐊", "Cocodrilo", "Crocodile", "Casi no ha cambiado desde los tiempos de los dinos."),
            p("🦖", "T-Rex", "T-Rex", "El rey de los rugidos y de los brazos pequeños."),
            p("🐚", "Amonita dorada", "Golden ammonite", "Una espiral perfecta atrapada en la roca."),
            p("🌳", "Bosque prehistórico", "Prehistoric forest", "Árboles gigantes que daban sombra a los gigantes."),
            p("🐾", "Huella gigante", "Giant footprint", "Una pisada y se estremece el valle.")))
        addAll(series(Series.BUGS,
            p("🐝", "Abeja", "Bee", "Trabaja zumbando y comparte miel."),
            p("🐜", "Hormiga", "Ant", "Carga cien veces su peso sin quejarse."),
            p("🐛", "Oruga", "Caterpillar", "Come, descansa y sueña con volar."),
            p("🐌", "Caracol", "Snail", "Lleva su casa a cuestas y llega a todos lados."),
            p("🐞", "Mariquita", "Ladybug", "Puntos negros que traen buena suerte."),
            p("🦗", "Grillo", "Cricket", "Su canción acompaña las noches de verano."),
            p("🌻", "Girasol zumbón", "Buzzing sunflower", "Todas las abejas del barrio lo visitan."),
            p("🪲", "Escarabajo dorado", "Golden beetle", "Su caparazón brilla como una joya viva."),
            p("🦠", "Microbio simpático", "Friendly microbe", "Chiquitito, pero muy importante."),
            p("💐", "Pradera florida", "Flower meadow", "Un tapiz de colores lleno de visitantes diminutos.")))
        addAll(series(Series.BIRDS,
            p("🐥", "Polluelo", "Chick", "Piar es su manera de decir hola."),
            p("🦆", "Pato", "Duck", "Se desliza por el estanque sin mojar su dignidad."),
            p("🕊️", "Paloma", "Dove", "Lleva paz sobre sus alas blancas."),
            p("🦃", "Pavo", "Turkey", "Presume su plumaje en cada desfile."),
            p("🦚", "Pavo real", "Peacock", "Despliega mil ojos de colores."),
            p("🦩", "Flamenco", "Flamingo", "Descansa en una pata y presume su rosa."),
            p("🐦‍⬛", "Cuervo", "Raven", "Inteligente y misterioso, guarda secretos."),
            p("🦢", "Cisne", "Swan", "Un poema blanco sobre el agua quieta."),
            p("🪶", "Pluma", "Feather", "Cayó del cielo y todavía conserva su vuelo."),
            p("🦤", "Dodó", "Dodo", "Ya no existe, pero todos lo recuerdan con cariño.")))
        addAll(series(Series.MYTH,
            p("🍄", "Hongo mágico", "Magic mushroom", "Crece donde bailan las hadas."),
            p("🔱", "Tridente", "Trident", "Domina las mareas con un solo golpe."),
            p("⚔️", "Espadas cruzadas", "Crossed swords", "Un duelo antiguo que aún resuena."),
            p("💫", "Polvo estelar", "Stardust", "Cae de las estrellas y huele a deseo."),
            p("🧜", "Sirena", "Mermaid", "Su canto se escucha desde el fondo del mar."),
            p("🧝", "Elfo", "Elf", "Vive siglos y recuerda cada hoja."),
            p("🦄", "Unicornio", "Unicorn", "Aparece cuando crees en él."),
            p("🦁", "Grifo real", "Royal griffin", "Mitad águila, mitad león, completamente leal."),
            p("🧿", "Amuleto", "Amulet", "Aleja la mala suerte con una sola mirada."),
            p("⚜️", "Emblema ancestral", "Ancestral emblem", "Marca de una antigua estirpe de héroes.")))
        addAll(series(Series.WORLDFOOD,
            p("🌮", "Taco", "Taco", "Doblado con amor, comido con alegría."),
            p("🍕", "Pizza", "Pizza", "Redonda como la luna, mejor que la luna."),
            p("🍔", "Hamburguesa", "Burger", "Un sándwich que se siente como un abrazo."),
            p("🍟", "Papas fritas", "Fries", "Siempre se acaban antes de lo que quisieras."),
            p("🍣", "Sushi", "Sushi", "Pequeñas obras de arte para comer."),
            p("🥟", "Dumpling", "Dumpling", "Un regalito caliente escondido en masa."),
            p("🍛", "Curry", "Curry", "Especias que cuentan un viaje entero."),
            p("🥘", "Paella real", "Royal paella", "Se comparte en el centro de la mesa."),
            p("🍝", "Pasta", "Pasta", "Cada hebra sabe a domingo en familia."),
            p("🥗", "Ensalada del chef", "Chef's salad", "Fresca, colorida y llena de sorpresas.")))
        addAll(series(Series.LANDMARKS,
            p("🗼", "Torre de Tokio", "Tokyo Tower", "Se enciende en rojo sobre la ciudad que nunca duerme."),
            p("🗽", "Estatua de la Libertad", "Statue of Liberty", "Da la bienvenida con una antorcha."),
            p("🏯", "Castillo japonés", "Japanese castle", "Techos curvos y siglos de historia."),
            p("🕌", "Mezquita", "Mosque", "Cúpulas y minaretes que tocan el cielo."),
            p("🏛️", "Partenón", "Parthenon", "Sus columnas guardan el eco de la Antigua Grecia."),
            p("⛩️", "Torii", "Torii gate", "Una puerta roja entre dos mundos."),
            p("🌉", "Puente colgante", "Suspension bridge", "Une dos orillas con un trazo de acero."),
            p("🏟️", "Coliseo", "Colosseum", "Aún se oyen los aplausos de hace dos mil años."),
            p("⛪", "Catedral", "Cathedral", "Vitrales que pintan la luz de colores."),
            p("🏝️", "Isla secreta", "Secret island", "Un trocito de paraíso para quien lo encuentre.")))
        addAll(series(Series.SUMMER,
            p("🍹", "Cóctel tropical", "Tropical drink", "Con sombrillita y todo."),
            p("🥥", "Coco", "Coconut", "Agua fresca en tu propio envase."),
            p("🕶️", "Gafas de sol", "Sunglasses", "Hacen que cualquiera parezca estrella de cine."),
            p("👙", "Traje de baño", "Swimsuit", "El uniforme oficial de las vacaciones."),
            p("🏖️", "Playa", "Beach", "Arena tibia y olas que susurran."),
            p("🌴", "Palmera", "Palm tree", "Baila con la brisa marina."),
            p("🌞", "Sol sonriente", "Smiling sun", "Pone de buen humor hasta a las nubes."),
            p("🌇", "Atardecer en la costa", "Coastal sunset", "El mejor momento del día pinta el cielo de fuego."),
            p("🎣", "Pesca tranquila", "Calm fishing", "Esperar también es parte del plan."),
            p("🚤", "Lancha", "Speedboat", "Deja una estela de espuma y risas.")))
        addAll(series(Series.TEMPLE,
            p("🎋", "Árbol de deseos", "Wish tree", "Cada tira de papel lleva una esperanza."),
            p("🎍", "Bambú", "Bamboo", "Se dobla con el viento y nunca se rompe."),
            p("🎐", "Campanilla de viento", "Wind chime", "Su sonido calma hasta las tormentas."),
            p("🎎", "Muñecas tradicionales", "Traditional dolls", "Guardan costumbres de generación en generación."),
            p("🧘", "Meditación", "Meditation", "Respira hondo: el mundo puede esperar un minuto."),
            p("🎑", "Luna de otoño", "Autumn moon", "Se celebra con té, silencio y gratitud."),
            p("🗻", "Monte Fuji", "Mount Fuji", "Perfecto, nevado y eternamente sereno."),
            p("🎏", "Carpa koi dorada", "Golden koi", "Remonta la corriente y se convierte en dragón."),
            p("🥋", "Dojo", "Dojo", "Aquí se entrena el cuerpo y también la paciencia."),
            p("🎴", "Naipes antiguos", "Ancient cards", "Cada carta guarda una flor y una estación.")))
        addAll(series(Series.PETS,
            p("🐶", "Perrito", "Puppy", "Mueve la cola por cada pequeño motivo."),
            p("🐱", "Gatito", "Kitten", "Ronronea como si fuera un motor feliz."),
            p("🐹", "Hámster", "Hamster", "Guarda semillas en las mejillas y secretos en el corazón."),
            p("🐰", "Conejito", "Bunny", "Su nariz no deja de moverse de curiosidad."),
            p("🐢", "Tortuguita", "Little turtle", "Lenta, constante, y siempre llega."),
            p("🐟", "Pez dorado", "Goldfish", "Da vueltas en su pecera como una joya."),
            p("🐩", "Caniche", "Poodle", "Elegante, juguetón y lleno de estilo."),
            p("🐈", "Gato de la suerte", "Lucky cat", "Con su patita en alto atrae la buena fortuna."),
            p("🐇", "Conejo blanco", "White rabbit", "Siempre llega tarde… pero con una sonrisa."),
            p("🐕", "Perro leal", "Loyal dog", "Te espera en la puerta, pase lo que pase.")))
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
