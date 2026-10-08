package com.korkoor.pardos.domain.level

import com.korkoor.pardos.domain.model.TileModel
import kotlin.random.Random

/** Piedra temporal de una tormenta: nació tras el movimiento número [born]. */
data class StormStone(val cell: Cell, val born: Int)

/**
 * Lógica pura de las tormentas (la usan la partida y el bot de pruebas, así que lo que se mide es lo que se juega).
 * Tras cada movimiento se llama a [step] con el número de movimientos hechos: se van las piedras que ya cumplieron su
 * vida y, cada `everyMoves` movimientos, cae una nueva en una casilla libre (nunca sobre una ficha ni si queda poco sitio).
 */
object StormRules {
    /** Con menos casillas libres que estas no cae ninguna piedra: la tormenta estorba, no mata. */
    const val MIN_FREE_TO_DROP = 4

    fun step(storm: Storm, moves: Int, current: List<StormStone>, tiles: List<TileModel>, base: Set<Cell>, size: Int, random: Random): List<StormStone> {
        val alive = current.filter { moves - it.born < storm.lifeMoves }
        if (moves <= 0 || moves % storm.everyMoves != 0 || alive.size >= storm.maxStones) return alive
        val occupied = HashSet<Cell>()
        tiles.forEach { occupied.add(it.row to it.col) }
        occupied.addAll(base)
        alive.forEach { occupied.add(it.cell) }
        val empty = ArrayList<Cell>()
        for (r in 0 until size) for (c in 0 until size) if ((r to c) !in occupied) empty.add(r to c)
        if (empty.size < MIN_FREE_TO_DROP) return alive
        return alive + StormStone(empty[random.nextInt(empty.size)], moves)
    }

    /** Casillas bloqueadas ahora: las piedras fijas del nivel más las de la tormenta. */
    fun blockedNow(base: Collection<Cell>, stones: List<StormStone>): Set<Cell> = base.toSet() + stones.map { it.cell }
}
