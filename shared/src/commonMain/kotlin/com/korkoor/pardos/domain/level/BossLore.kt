package com.korkoor.pardos.domain.level

/** Cómo se dibuja y se anima cada jefe (la app dibuja un emblema propio por motivo). */
enum class BossMotif { WALL, STORM, CLOCK, GOLD, STONE, CRYSTAL, EYE, GEARS, MAZE, FIRE }

/** Motivo y frase de cada jefe: les da identidad (no son solo "un nivel difícil"). */
object BossLore {
    private data class Entry(val motif: BossMotif, val taunt: String)

    private val entries: Map<String, Entry> = mapOf(
        "El Muro" to Entry(BossMotif.WALL, "Nadie ha pasado de mis piedras."),
        "Tormenta de 4" to Entry(BossMotif.STORM, "Aquí solo llueven cuatros."),
        "Doble o nada" to Entry(BossMotif.GOLD, "Cada jugada vale el doble… o nada."),
        "La Cantera" to Entry(BossMotif.STONE, "Cada piedra tiene su sitio. El tuyo, no."),
        "Relojería" to Entry(BossMotif.CLOCK, "Tic, tac. Se te acaba el tiempo."),
        "Duelo de puntos" to Entry(BossMotif.GEARS, "Cuenta bien: yo no pierdo puntos."),
        "Rompecabezas" to Entry(BossMotif.MAZE, "Todas las piezas encajan, menos las tuyas."),
        "Escalera real" to Entry(BossMotif.CRYSTAL, "Sube escalón a escalón, si puedes."),
        "Pesadilla" to Entry(BossMotif.EYE, "Cierra los ojos. Te estoy mirando."),
        "Espejismo" to Entry(BossMotif.EYE, "¿Seguro que ese lado es el izquierdo?"),
        "Ojo del huracán" to Entry(BossMotif.STORM, "En el centro todo está en calma. Aún."),
        "Gran maratón" to Entry(BossMotif.CLOCK, "No hay meta cercana. Respira."),
        "Reacción en cadena" to Entry(BossMotif.FIRE, "Una chispa y se enciende todo."),
        "Cosecha de oro" to Entry(BossMotif.GOLD, "Recoge hasta la última ficha."),
        "Lluvia de meteoros" to Entry(BossMotif.FIRE, "Mira arriba… y esquiva."),
        "Callejón sin salida" to Entry(BossMotif.MAZE, "Una dirección menos. ¿Te alcanza?"),
        "El Laberinto" to Entry(BossMotif.MAZE, "Entrar es fácil."),
        "Fábrica de combos" to Entry(BossMotif.GEARS, "Aquí todo se hace en cadena."),
        "Cronista" to Entry(BossMotif.CLOCK, "Lo anoto todo, también tus errores."),
        "Dragón de piedra" to Entry(BossMotif.STONE, "Duermo sobre un montón de fichas."),
        "Doble tormenta" to Entry(BossMotif.STORM, "Una tormenta no bastaba."),
        "Gran cosecha" to Entry(BossMotif.GOLD, "El campo está lleno. El tiempo, no."),
        "Torre de cristal" to Entry(BossMotif.CRYSTAL, "Un paso en falso y se rompe."),
        "Ojo de la tormenta" to Entry(BossMotif.EYE, "Lo veo todo, incluso lo que giras."),
        "Coloso de hierro" to Entry(BossMotif.WALL, "Pesado, lento… imparable."),
        "Banquete de cuatros" to Entry(BossMotif.GOLD, "Hoy se sirve solo el cuatro."),
        "Reloj roto" to Entry(BossMotif.CLOCK, "Las agujas ya no saben hacia dónde."),
        "Maestro del eco" to Entry(BossMotif.CRYSTAL, "Todo lo que haces, dos veces."),
        "Fuego fatuo" to Entry(BossMotif.FIRE, "Sigue la luz. Siempre miente."),
        "Guardián del abismo" to Entry(BossMotif.EYE, "Abajo no hay fondo."),
        "Eclipse" to Entry(BossMotif.STORM, "Hasta la luz se pone al revés."),
        "El Último Jefe" to Entry(BossMotif.FIRE, "Has llegado lejos. Aquí termina."),
        "Segador" to Entry(BossMotif.GOLD, "Siembro fichas y cosecho jugadores."),
        "Coloso de cristal" to Entry(BossMotif.CRYSTAL, "Pesado como el hierro, frágil como el vidrio."),
        "Reina de espejos" to Entry(BossMotif.EYE, "Mírate: ya vas al revés."),
        "Relámpago negro" to Entry(BossMotif.STORM, "Primero el rayo. Después, el trueno."),
        "El Archivista" to Entry(BossMotif.GEARS, "Cada jugada queda archivada. Y juzgada."),
        "Mareas" to Entry(BossMotif.CLOCK, "El tablero sube y baja con la luna."),
        "Titán" to Entry(BossMotif.STONE, "Mi paciencia es de montaña."),
        "Rey de los jefes" to Entry(BossMotif.FIRE, "Todos los jefes me sirven. ¿Y tú?"),
        // Jefes de los primeros capítulos, hechos a mano
        "Rincón apurado" to Entry(BossMotif.CLOCK, "Cuatro esquinas y ni un segundo de sobra."),
        "Cuatro rincones" to Entry(BossMotif.WALL, "Cada esquina guarda una sorpresa.")
    )

    val names: Set<String> get() = entries.keys

    fun motifOf(name: String?): BossMotif = entries[name]?.motif ?: Guardians.motifOf(name)

    fun tauntOf(name: String?): String? = entries[name]?.taunt ?: Guardians.tauntOf(name)
}

/**
 * Guardianes: mini jefes que vigilan los niveles 5 y 15 de cada capítulo (desde el capítulo 3). No cambian las reglas del nivel:
 * le ponen cara, nombre y una frase, y cuentan en el bestiario.
 */
object Guardians {
    private data class G(val name: String, val motif: BossMotif, val taunt: String)

    private val all = listOf(
        G("Vigía", BossMotif.EYE, "Te vi llegar hace tres niveles."),
        G("Centinela", BossMotif.WALL, "Por aquí no se pasa sin permiso."),
        G("Aprendiz de brujo", BossMotif.FIRE, "Esto lo practiqué ayer. ¡Casi sale!"),
        G("Gárgola", BossMotif.STONE, "Llevo siglos aquí. Puedo esperar."),
        G("Duende del reloj", BossMotif.CLOCK, "Te robé un segundo. Y otro."),
        G("Topo de piedra", BossMotif.STONE, "Cavo más rápido de lo que juegas."),
        G("Cuervo", BossMotif.EYE, "Tengo ojo para las fichas brillantes."),
        G("Medusa de vidrio", BossMotif.CRYSTAL, "No me mires fijamente."),
        G("Salamandra", BossMotif.FIRE, "Cuidado: hoy estoy de mal humor."),
        G("Nube gruñona", BossMotif.STORM, "Si llueve, es culpa tuya."),
        G("Autómata", BossMotif.GEARS, "Mis movimientos están calculados."),
        G("Espectro", BossMotif.MAZE, "Ya has estado aquí. ¿O no?"),
        G("Bruja del té", BossMotif.CRYSTAL, "Una gota de té y todo cambia de color."),
        G("Murciélago bibliotecario", BossMotif.EYE, "Shhh. Aquí se juega en silencio."),
        G("Momia perezosa", BossMotif.CLOCK, "Mañana te envuelvo. O pasado."),
        G("Fantasma tímido", BossMotif.MAZE, "¿Me ves? Qué vergüenza."),
        G("Gato de ceniza", BossMotif.FIRE, "Siete vidas. Ninguna para ti."),
        G("Golem de musgo", BossMotif.STONE, "Crezco despacio. ¿Y tú, cuánto aguantas?"),
        G("Hada del hielo", BossMotif.CRYSTAL, "Un soplido y se congela la jugada."),
        G("Marioneta", BossMotif.GEARS, "Hilos por todas partes. ¿Quién mueve a quién?"),
        G("Faro errante", BossMotif.STORM, "Te guío… hacia las rocas."),
        G("Mapache ladrón", BossMotif.GOLD, "Esa ficha brillante ya es mía."),
        G("Cangrejo ermitaño", BossMotif.WALL, "Mi casa es mi tablero."),
        G("Sombra de la luna", BossMotif.EYE, "Solo existo cuando miras hacia otro lado.")
    )

    private fun find(name: String?) = all.firstOrNull { it.name == name }

    /** Guardián de un nivel de campaña, o null si ese nivel no tiene. */
    fun forLevel(level: Int): String? {
        val idx = (level - 1) % LevelCatalog.LEVELS_PER_CHAPTER
        val chapter = (level - 1) / LevelCatalog.LEVELS_PER_CHAPTER
        if (chapter < 2 || (idx != 4 && idx != 14)) return null
        return all[(chapter * 7 + idx) % all.size].name
    }

    val names: List<String> get() = all.map { it.name }
    fun motifOf(name: String?): BossMotif = find(name)?.motif ?: BossMotif.WALL
    fun tauntOf(name: String?): String? = find(name)?.taunt
}

/** Bestiario: todos los jefes y guardianes, con los niveles donde se les enfrenta. Se vencen al ganar cualquiera de esos niveles. */
object Bestiary {
    data class Entry(val name: String, val motif: BossMotif, val taunt: String, val levels: List<Int>, val guardian: Boolean) {
        val firstLevel: Int get() = levels.first()
        fun defeated(stars: (Int) -> Int): Boolean = levels.any { stars(it) > 0 }
    }

    val bosses: List<Entry> by lazy {
        (10..LevelCatalog.TOTAL_LEVELS step 10).map { it to LevelCatalog.spec(it) }.filter { it.second.isBoss }
            .groupBy({ it.second.title }, { it.first })
            .map { (name, levels) -> Entry(name, BossLore.motifOf(name), BossLore.tauntOf(name).orEmpty(), levels.sorted(), false) }
            .sortedBy { it.firstLevel }
    }

    val guardians: List<Entry> by lazy {
        (1..LevelCatalog.TOTAL_LEVELS).mapNotNull { lv -> Guardians.forLevel(lv)?.let { it to lv } }
            .groupBy({ it.first }, { it.second })
            .map { (name, levels) -> Entry(name, Guardians.motifOf(name), Guardians.tauntOf(name).orEmpty(), levels.sorted(), true) }
            .sortedBy { it.firstLevel }
    }
}
