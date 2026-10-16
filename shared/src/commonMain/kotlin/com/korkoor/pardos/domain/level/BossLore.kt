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
        "El Último Jefe" to Entry(BossMotif.FIRE, "Has llegado lejos. Aquí termina.")
    )

    val names: Set<String> get() = entries.keys

    fun motifOf(name: String?): BossMotif = entries[name]?.motif ?: BossMotif.WALL

    fun tauntOf(name: String?): String? = entries[name]?.taunt
}
