package com.korkoor.pardos.domain.logic

/** Motor determinista del torneo. El mismo protocolo está implementado en Cloud Functions. */
class CompetitiveRound(seed: Long) {
    val boardSize: Int = 4
    private var randomState = seed.toUInt().let { if (it == 0u) 1u else it }
    private val cells = IntArray(boardSize * boardSize)
    private val moveLog = mutableListOf<String>()
    var score: Int = 0
        private set
    val actions: List<String> get() = moveLog.toList()
    val board: List<Int> get() = cells.toList()
    val isOver: Boolean get() = !hasMove()

    init { spawn(); spawn() }

    fun move(direction: Direction): Boolean {
        if (isOver || moveLog.size >= MAX_ACTIONS) return false
        val next = cells.copyOf()
        var gained = 0
        for (line in 0 until boardSize) {
            val indexes = lineIndexes(line, direction)
            val values = indexes.map { cells[it] }.filter { it != 0 }
            val merged = mutableListOf<Int>()
            var i = 0
            while (i < values.size) {
                if (i + 1 < values.size && values[i] == values[i + 1]) {
                    val value = values[i] * 2
                    merged.add(value)
                    gained += value
                    i += 2
                } else {
                    merged.add(values[i])
                    i++
                }
            }
            indexes.forEachIndexed { index, cell -> next[cell] = merged.getOrElse(index) { 0 } }
        }
        if (next.contentEquals(cells)) return false
        next.copyInto(cells)
        score += gained
        moveLog.add(direction.name)
        spawn()
        return true
    }

    private fun lineIndexes(line: Int, direction: Direction): List<Int> = (0 until boardSize).map { step ->
        when (direction) {
            Direction.LEFT -> line * boardSize + step
            Direction.RIGHT -> line * boardSize + boardSize - 1 - step
            Direction.UP -> step * boardSize + line
            Direction.DOWN -> (boardSize - 1 - step) * boardSize + line
        }
    }

    private fun nextRandom(): UInt {
        randomState = randomState xor (randomState shl 13)
        randomState = randomState xor (randomState shr 17)
        randomState = randomState xor (randomState shl 5)
        return randomState
    }

    private fun spawn() {
        val empty = cells.indices.filter { cells[it] == 0 }
        if (empty.isEmpty()) return
        val position = empty[(nextRandom() % empty.size.toUInt()).toInt()]
        cells[position] = if (nextRandom() % 10u == 0u) 4 else 2
    }

    private fun hasMove(): Boolean {
        if (cells.any { it == 0 }) return true
        for (row in 0 until boardSize) for (col in 0 until boardSize) {
            val i = row * boardSize + col
            if ((col + 1 < boardSize && cells[i] == cells[i + 1]) ||
                (row + 1 < boardSize && cells[i] == cells[i + boardSize])) return true
        }
        return false
    }

    companion object { const val MAX_ACTIONS = 200 }
}
