package com.korkoor.pardos

import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelKind
import kotlin.test.Test

/** Informe del catálogo (no comprueba nada): se activa con CAL=report y sirve para escribir la documentación y buscar niveles de prueba. */
class CatalogReportTest {
    @Test fun report() {
        if ((System.getenv("CAL") ?: "") != "report") return
        val all = LevelCatalog.all()
        println("REP total=${all.size}")
        for (kind in LevelKind.entries) {
            val of = all.filter { it.kind == kind }
            println("REP kind=$kind count=${of.size} (${of.size * 100 / all.size}%) first=${of.firstOrNull()?.id} last=${of.lastOrNull()?.id}")
            for (s in of.take(3)) println("REP   #${s.id} ${s.title} ${s.boardSize}x${s.boardSize} ${s.goalText()} chips=${s.ruleChips()}")
        }
        // los giros y las tormentas por capítulo
        for (twist in com.korkoor.pardos.domain.level.Twist.entries.drop(1)) {
            val first = all.firstOrNull { it.twist == twist }
            println("REP twist=$twist first=${first?.id} (cap ${(first?.id ?: 1) / 20 + 1}) count=${all.count { it.twist == twist }}")
        }
        println("REP tamaños=" + all.groupBy { it.boardSize }.mapValues { it.value.size })
        println("REP metas máx=" + all.filter { it.goal == com.korkoor.pardos.domain.level.LevelGoal.REACH_TILE }.maxOf { it.goalValue })
        // tiempo estimado de juego: movimientos mínimos × 1,15 (los reales) a 0,9 s por movimiento, +25 % por reintentos, +12 s por nivel (menús, cofres, anuncios)
        fun minutes(specs: List<com.korkoor.pardos.domain.level.LevelSpec>) =
            specs.sumOf { com.korkoor.pardos.domain.level.LevelMath.minMoves(it) * 1.15 * 0.9 * 1.25 + 12 } / 60.0
        for (from in 1..all.size step 400) {
            val slice = all.filter { it.id in from until from + 400 }
            println("REP tiempo ${from}-${from + 399}: ${"%.1f".format(minutes(slice) / 60)} h (${"%.1f".format(minutes(slice) / slice.size)} min por nivel)")
        }
        println("REP tiempo total: ${"%.1f".format(minutes(all) / 60)} h → a 3,5 h/día: ${"%.1f".format(minutes(all) / 60 / 3.5)} días")
        // un resumen por tramos de 200 niveles
        for (from in 1..all.size step 200) {
            val slice = all.filter { it.id in from until from + 200 }
            val kinds = slice.groupBy { it.kind }.mapValues { it.value.size }.entries.sortedByDescending { it.value }.joinToString { "${it.key.label}:${it.value}" }
            println("REP ${from}-${from + 199}: $kinds")
        }
    }
}
