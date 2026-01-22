package com.korkoor.pardos.domain.model

import java.util.UUID

data class TileModel(
    val id: String,
    val value: Int,
    val row: Int,
    val col: Int,
    val isNew: Boolean = false,
    val isMerged: Boolean = false,
    val isBlock: Boolean = false
) {
    init {
        require(value > 0) { "Tile value must be positive: $value" }
        require(row >= 0) { "Row must be non-negative: $row" }
        require(col >= 0) { "Col must be non-negative: $col" }
    }

    // Borramos 'val color' y 'getTileColor' de aquí.
    // Eso va en la capa de UI (Tile.kt).

    companion object {
        fun generateId(): String = UUID.randomUUID().toString()

        fun create(row: Int, col: Int, value: Int = 2): TileModel =
            TileModel(generateId(), value, row, col, isNew = true)
    }
}