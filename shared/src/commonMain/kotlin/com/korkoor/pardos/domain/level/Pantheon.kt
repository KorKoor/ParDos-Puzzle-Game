package com.korkoor.pardos.domain.level

/**
 * El Panteón de jefes: 12 salas con 5 jefes cada una (60 en total), aparte de la campaña. Sus niveles viven en una zona de números
 * propia ([BASE] en adelante), así que no mueven ni un solo nivel ni jefe de la campaña. Cada jefe mezcla reglas que el motor ya
 * tiene (piedras, reloj, tormentas, giros de controles, segunda fase...) y se juega en orden: vencer a uno abre al siguiente.
 */
object Pantheon {
    /** Los niveles del Panteón empiezan aquí (la campaña llega a 2.400). */
    const val BASE = 100_000
    const val ROOMS = 12
    const val PER_ROOM = 5
    const val TOTAL = ROOMS * PER_ROOM

    /** Nivel de campaña que se pide para entrar al Panteón (ya conoce todas las reglas básicas). */
    const val MIN_CAMPAIGN_LEVEL = 30

    data class Boss(val index: Int, val name: String, val motif: BossMotif, val taunt: String) {
        val id: Int get() = BASE + index + 1
        val room: Int get() = index / PER_ROOM
        val position: Int get() = index % PER_ROOM
        val isRoomBoss: Boolean get() = position == PER_ROOM - 1
        val spec: LevelSpec get() = build(this)
    }

    class Room(val index: Int, val name: String, val bosses: List<Boss>)

    private val roomNames = listOf(
        "Sala de las Sombras", "Taller del Relojero", "Jardín Prohibido", "Mar de Cristal", "Biblioteca Infinita", "La Fundición",
        "Cumbre de Tormentas", "Circo Olvidado", "Catacumbas Doradas", "El Observatorio", "Bosque de Espejos", "Trono del Panteón"
    )

    private data class Def(val name: String, val motif: BossMotif, val taunt: String)

    private val defs = listOf(
        // Sala de las Sombras
        Def("Silueta", BossMotif.EYE, "Soy lo que queda cuando apagas la luz."),
        Def("Telón", BossMotif.WALL, "Todo espectáculo empieza cuando yo me abro."),
        Def("Penumbra", BossMotif.MAZE, "No estoy aquí. Ni allí. Pero te veo."),
        Def("Nocturno", BossMotif.STORM, "La noche tiene su propio reloj."),
        Def("Rey de sombras", BossMotif.EYE, "Mis súbditos no tienen nombre. Tú tampoco."),
        // Taller del Relojero
        Def("Engranajín", BossMotif.GEARS, "Un diente más y todo gira."),
        Def("Péndulo", BossMotif.CLOCK, "Voy, vengo, y el tiempo se va."),
        Def("Cuco loco", BossMotif.CLOCK, "¡Cuco! ¡Cuco! Ya es tarde."),
        Def("Maestro de agujas", BossMotif.GEARS, "Cada hora tiene su precio."),
        Def("Tic Tac Supremo", BossMotif.CLOCK, "Cuando yo paro, paran todos."),
        // Jardín Prohibido
        Def("Rosal espinoso", BossMotif.FIRE, "Bonito de lejos. De cerca, también."),
        Def("Hiedra viva", BossMotif.MAZE, "Te abrazo hasta que no puedas salir."),
        Def("Gran Girasol", BossMotif.GOLD, "Siempre miro al que brilla. Hoy, tú."),
        Def("Hongo sabio", BossMotif.STONE, "Crezco en la sombra y lo sé todo."),
        Def("Reina del jardín", BossMotif.FIRE, "Nadie arranca mis flores."),
        // Mar de Cristal
        Def("Medusa lunar", BossMotif.CRYSTAL, "Brillo para que te acerques."),
        Def("Coral rey", BossMotif.STONE, "Mil años construyendo este muro."),
        Def("Kraken de vidrio", BossMotif.CRYSTAL, "Ocho brazos y ninguna prisa."),
        Def("Sirena glacial", BossMotif.CRYSTAL, "Mi canción congela las jugadas."),
        Def("Leviatán", BossMotif.STORM, "El fondo no tiene fondo."),
        // Biblioteca Infinita
        Def("Escriba", BossMotif.GEARS, "Lo que anoto, ocurre."),
        Def("Tomo maldito", BossMotif.EYE, "No abras la página siete."),
        Def("Bibliotecario ciego", BossMotif.MAZE, "Conozco cada pasillo de memoria."),
        Def("Tinta viva", BossMotif.STORM, "Me derramo y lo mancho todo."),
        Def("Archimago", BossMotif.FIRE, "Un hechizo por cada error tuyo."),
        // La Fundición
        Def("Yunque", BossMotif.WALL, "Golpéame. Yo no me muevo."),
        Def("Horno rugiente", BossMotif.FIRE, "Aquí todo se derrite. Hasta la paciencia."),
        Def("Forjador", BossMotif.GEARS, "Te voy a dar forma, quieras o no."),
        Def("Coloso fundido", BossMotif.STONE, "Pesado como el plomo, caliente como el sol."),
        Def("Ifrit", BossMotif.FIRE, "Llamas que piensan. Y tú, ¿piensas rápido?"),
        // Cumbre de Tormentas
        Def("Nimbo", BossMotif.STORM, "Una nube pequeña con un gran mal humor."),
        Def("Rayo azul", BossMotif.STORM, "Caigo una vez. Basta."),
        Def("Águila de trueno", BossMotif.STORM, "Mis alas hacen llover fichas."),
        Def("Ojo del ciclón", BossMotif.EYE, "Calma en el centro. Caos en todo lo demás."),
        Def("Señor del trueno", BossMotif.STORM, "Cuando hablo, tiembla el tablero."),
        // Circo Olvidado
        Def("Payaso triste", BossMotif.MAZE, "Reír cuesta más cuando pierdes."),
        Def("Equilibrista", BossMotif.CRYSTAL, "Un paso a la vez. Un error y caes."),
        Def("Domador", BossMotif.GEARS, "Las fichas me obedecen. ¿A ti?"),
        Def("Ilusionista", BossMotif.EYE, "¿Seguro que esa ficha estaba ahí?"),
        Def("Director de pista", BossMotif.GOLD, "¡Damas y caballeros, su derrota!"),
        // Catacumbas Doradas
        Def("Escarabajo", BossMotif.GOLD, "Rodo el oro de otros."),
        Def("Faraón dormido", BossMotif.CLOCK, "Mil años de sueño y aún madrugo más que tú."),
        Def("Chacal dorado", BossMotif.GOLD, "Olfateo las fichas que escondes."),
        Def("Esfinge", BossMotif.WALL, "Una pregunta, un tablero, un destino."),
        Def("Rey eterno", BossMotif.GOLD, "Mi reino no tiene fin. Ni salida."),
        // El Observatorio
        Def("Astrónomo", BossMotif.EYE, "Calculé tu derrota hace tres lunas."),
        Def("Cometa", BossMotif.FIRE, "Paso cada cien años. Hoy me toca a mí."),
        Def("Eclipse total", BossMotif.STORM, "Cuando me interpongo, se apaga la luz."),
        Def("Constelación viva", BossMotif.CRYSTAL, "Mil estrellas, una sola voluntad."),
        Def("Guardián del cosmos", BossMotif.EYE, "Más allá de mí solo hay silencio."),
        // Bosque de Espejos
        Def("Reflejo", BossMotif.EYE, "Soy tú. Pero mejor."),
        Def("Doble", BossMotif.MAZE, "Dos tableros. Ningún descanso."),
        Def("Caleidoscopio", BossMotif.CRYSTAL, "Cada giro, una trampa distinta."),
        Def("Reina rota", BossMotif.CRYSTAL, "Mil pedazos y todos te miran."),
        Def("Mil caras", BossMotif.EYE, "¿Cuál soy yo? Adivina."),
        // Trono del Panteón
        Def("Heraldo", BossMotif.GOLD, "Anuncio tu llegada. Y tu final."),
        Def("Verdugo", BossMotif.WALL, "Rápido y sin rencor."),
        Def("Canciller", BossMotif.GEARS, "Tu caso está pendiente. Para siempre."),
        Def("Sombra del Rey", BossMotif.EYE, "El Rey no necesita pelear. Me tiene a mí."),
        Def("El Coleccionista", BossMotif.FIRE, "Tengo a todos los jefes. Falta uno: tú.")
    )

    val bosses: List<Boss> by lazy { defs.mapIndexed { i, d -> Boss(i, d.name, d.motif, d.taunt) } }

    val rooms: List<Room> by lazy {
        (0 until ROOMS).map { r -> Room(r, roomNames[r], bosses.subList(r * PER_ROOM, (r + 1) * PER_ROOM)) }
    }

    fun isPantheon(level: Int): Boolean = level > BASE && level <= BASE + TOTAL

    fun bossOf(level: Int): Boss? = if (isPantheon(level)) bosses[level - BASE - 1] else null

    fun byName(name: String?): Boss? = bosses.firstOrNull { it.name == name }

    /** Monedas y gemas de la primera victoria sobre un jefe (más en las salas altas y en el jefe de sala). */
    fun coinsFor(boss: Boss): Int = 120 + 40 * boss.room + if (boss.isRoomBoss) 150 else 0
    fun gemsFor(boss: Boss): Int = 2 + boss.room / 2 + if (boss.isRoomBoss) 8 else 0

    // ---------------------------------------------------------------- construcción de los niveles

    private val twists = listOf(Twist.NO_UP, Twist.MIRROR_H, Twist.MIRROR_V, Twist.NO_LEFT, Twist.NO_DOWN)

    private fun build(b: Boss): LevelSpec {
        val B = LevelBuilders
        val id = b.id
        val room = b.room
        val tile = when {
            room < 3 -> 256
            room < 8 -> 512
            else -> 1024
        }
        val pillars = StonePatterns.all.filter { it.size == 4 && it.cells.size == 1 && it.severity == 2 }
        val pillar = pillars[(b.index * 7) % pillars.size]
        val pillarTile = 1 shl StonePatterns.exponentFor(tile.countTrailingZeroBits(), pillar)
        val twist = twists[(room + b.position) % twists.size]
        val base = when ((b.position + room) % 8) {
            0 -> B.sprint(id, pillarTile, 4, slack = 1.75, stones = pillar)
            1 -> B.clock(id, tile, 4, secondsPerMove = 1.9)
            2 -> B.storm(id, minOf(tile, 512), 4)
            3 -> B.twins(id, minOf(tile, 512) / (if (tile >= 1024) 2 else 1), 4)
            4 -> B.fours(id, minOf(tile, 512))
            5 -> B.headStart(id, minOf(tile, 512))
            6 -> B.twisted(B.zen(id, minOf(tile, 512), 4), twist)
            else -> B.doubleDrop(id, maxOf(256, tile / 2), 4)
        }
        val boss = B.boss(base, b.name, tip = base.tip)
        // Desde la sala 2, cada jefe cambia las reglas a media partida; los jefes de sala lo hacen siempre
        if (room == 0 && !b.isRoomBoss) return boss
        val phase = when ((b.index + room) % 4) {
            0 -> B.withPhase(boss.copy(moveLimit = boss.moveLimit?.let { (it * 1.12).toInt() }), "¡Cae una tormenta!", storm = Storm(everyMoves = 9, lifeMoves = 5, maxStones = 2))
            1 -> B.withPhase(boss.copy(moveLimit = boss.moveLimit?.let { (it * 1.12).toInt() }), "¡Llueven los 4!", spawn = SpawnStyle.FOURS)
            2 -> B.withPhase(boss.copy(moveLimit = boss.moveLimit?.let { (it * 1.12).toInt() }), "¡Controles cambiados!", twist = Twist.MIRROR_H)
            else -> B.withPhase(boss.copy(moveLimit = boss.moveLimit?.let { (it * 1.12).toInt() }), "¡Una dirección se bloquea!", twist = Twist.NO_UP)
        }
        // El choque de reglas con un giro ya puesto sería injusto: ahí se deja el jefe de una sola fase
        return if (phase.twist != Twist.NONE && phase.phase2?.twist != null) boss else phase
    }
}
