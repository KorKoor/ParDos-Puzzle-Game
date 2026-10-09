package com.korkoor.pardos.domain.achievements

import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.domain.model.TileModel

/** Lo que se mira para saber si un logro se cumple (el mismo contenido que el estado del tablero de Android). */
data class AchContext(
    val isLevelCompleted: Boolean,
    val currentLevel: Int,
    val moveCount: Int,
    val elapsedTime: Long,
    val score: Int,
    val combo: Int,
    val emptySpaces: Int,
    val hasMovesAvailable: Boolean,
    val boardSize: Int,
    val gameMode: GameMode,
    val tiles: List<TileModel>
)

/** Un logro. Los textos y condiciones son los mismos que en Android (`gameAchievements`), pasados a datos puros. */
class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val color: Long,
    val condition: (AchContext) -> Boolean
)

/** Catálogo de logros compartido con iPhone (generado desde la lista de Android; para añadir uno, una entrada más). */
object AchievementCatalog {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_win", "Primer Paso", "Completa tu primer nivel.", 0xFF81B29AL, { it.isLevelCompleted && it.currentLevel == 1 }),
        AchievementDef("getting_warmed_up", "Calentando Motores", "Alcanza el nivel 3.", 0xFFE07A5FL, { it.isLevelCompleted && it.currentLevel == 2 }),
        AchievementDef("level_5", "Mano Caliente", "Alcanza el nivel 5.", 0xFFF2CC8FL, { it.isLevelCompleted && it.currentLevel == 4 }),
        AchievementDef("level_10", "Doble Dígito", "Alcanza el nivel 10.", 0xFFC0C0C0L, { it.isLevelCompleted && it.currentLevel == 9 }),
        AchievementDef("level_15", "Persistente", "Alcanza el nivel 15.", 0xFFC0C0C0L, { it.isLevelCompleted && it.currentLevel == 14 }),
        AchievementDef("level_20", "Veterano", "Alcanza el nivel 20.", 0xFFFFD700L, { it.isLevelCompleted && it.currentLevel == 19 }),
        AchievementDef("level_25", "Cuarto de Siglo", "Alcanza el nivel 25.", 0xFFFFD700L, { it.isLevelCompleted && it.currentLevel == 24 }),
        AchievementDef("level_30", "Camino a la Gloria", "Alcanza el nivel 30.", 0xFFFFD700L, { it.isLevelCompleted && it.currentLevel == 29 }),
        AchievementDef("level_40", "Experto Confirmado", "Alcanza el nivel 40.", 0xFF9C27B0L, { it.isLevelCompleted && it.currentLevel == 39 }),
        AchievementDef("level_50", "Leyenda", "Alcanza el nivel 50.", 0xFF9C27B0L, { it.isLevelCompleted && it.currentLevel == 49 }),
        AchievementDef("level_75", "Trascendental", "Alcanza el nivel 75.", 0xFFFF6B6BL, { it.isLevelCompleted && it.currentLevel == 74 }),
        AchievementDef("level_100", "Centenario", "Alcanza el nivel 100.", 0xFFFF1744L, { it.isLevelCompleted && it.currentLevel == 99 }),
        AchievementDef("tile_16", "Dulces Dieciséis", "Crea una ficha de 16.", 0xFFF59563L, { it.tiles.any { tile -> tile.value >= 16 } }),
        AchievementDef("tile_32", "Treinta y Dos", "Crea una ficha de 32.", 0xFFF67C5FL, { it.tiles.any { tile -> tile.value >= 32 } }),
        AchievementDef("tile_64", "Sesenta y Cuatro", "Crea una ficha de 64.", 0xFFF65E3BL, { it.tiles.any { tile -> tile.value >= 64 } }),
        AchievementDef("tile_128", "Centenar Plus", "Crea una ficha de 128.", 0xFFEDCF72L, { it.tiles.any { tile -> tile.value >= 128 } }),
        AchievementDef("tile_256", "Doble Centenar", "Crea una ficha de 256.", 0xFFEDCC61L, { it.tiles.any { tile -> tile.value >= 256 } }),
        AchievementDef("tile_512", "Quinientos Doce", "Crea una ficha de 512.", 0xFFEDC850L, { it.tiles.any { tile -> tile.value >= 512 } }),
        AchievementDef("tile_1024", "Mil Veinticuatro", "Crea una ficha de 1024.", 0xFFEDC53FL, { it.tiles.any { tile -> tile.value >= 1024 } }),
        AchievementDef("tile_2048", "¡2048 Alcanzado!", "Crea la legendaria ficha de 2048.", 0xFFEDC22EL, { it.tiles.any { tile -> tile.value >= 2048 } }),
        AchievementDef("tile_4096", "Más Allá", "Crea una ficha de 4096.", 0xFF9C27B0L, { it.tiles.any { tile -> tile.value >= 4096 } }),
        AchievementDef("tile_8192", "Estratosférico", "Crea una ficha de 8192.", 0xFFFF1744L, { it.tiles.any { tile -> tile.value >= 8192 } }),
        AchievementDef("speedrun", "Relámpago", "Gana un nivel en menos de 50 movimientos.", 0xFFE07A5FL, { it.isLevelCompleted && it.moveCount < 50 }),
        AchievementDef("speed_40", "Veloz", "Gana un nivel en menos de 40 movimientos.", 0xFFE07A5FL, { it.isLevelCompleted && it.moveCount < 40 }),
        AchievementDef("speed_30", "Rápido Como el Viento", "Gana un nivel en menos de 30 movimientos.", 0xFFFF9800L, { it.isLevelCompleted && it.moveCount < 30 }),
        AchievementDef("speed_20", "Supersónico", "Gana un nivel en menos de 20 movimientos.", 0xFFFFD700L, { it.isLevelCompleted && it.moveCount < 20 }),
        AchievementDef("speed_15", "Eficiencia Máxima", "Gana un nivel en menos de 15 movimientos.", 0xFF9C27B0L, { it.isLevelCompleted && it.moveCount < 15 }),
        AchievementDef("speed_10", "Perfección Absoluta", "Gana un nivel en menos de 10 movimientos.", 0xFFFF6B6BL, { it.isLevelCompleted && it.moveCount < 10 }),
        AchievementDef("time_60", "Minuto de Oro", "Completa un nivel en menos de 60 segundos.", 0xFFFFD700L, { it.isLevelCompleted && it.elapsedTime < 60_000 }),
        AchievementDef("time_45", "Tres Cuartos", "Completa un nivel en menos de 45 segundos.", 0xFFFFD700L, { it.isLevelCompleted && it.elapsedTime < 45_000 }),
        AchievementDef("time_30", "Medio Minuto", "Completa un nivel en menos de 30 segundos.", 0xFFFF9800L, { it.isLevelCompleted && it.elapsedTime < 30_000 }),
        AchievementDef("time_15", "Flash", "Completa un nivel en menos de 15 segundos.", 0xFFFF6B6BL, { it.isLevelCompleted && it.elapsedTime < 15_000 }),
        AchievementDef("century", "Centurión", "Haz 100 movimientos en una sola partida.", 0xFF3D405BL, { it.moveCount >= 100 }),
        AchievementDef("moves_150", "Sesquicentenario", "Haz 150 movimientos en una sola partida.", 0xFF3D405BL, { it.moveCount >= 150 }),
        AchievementDef("moves_200", "Bicentenario", "Haz 200 movimientos en una sola partida.", 0xFF6C63FFL, { it.moveCount >= 200 }),
        AchievementDef("moves_300", "Tricentenario", "Haz 300 movimientos en una sola partida.", 0xFF9C27B0L, { it.moveCount >= 300 }),
        AchievementDef("moves_500", "Imparable", "Haz 500 movimientos en una sola partida.", 0xFFFF6B6BL, { it.moveCount >= 500 }),
        AchievementDef("time_5min", "Cinco Minutos", "Juega durante 5 minutos seguidos.", 0xFF81B29AL, { it.elapsedTime >= 300_000 }),
        AchievementDef("time_10min", "Decálogo Temporal", "Juega durante 10 minutos seguidos.", 0xFF6C63FFL, { it.elapsedTime >= 600_000 }),
        AchievementDef("time_20min", "Veinte Minutos", "Juega durante 20 minutos seguidos.", 0xFF9C27B0L, { it.elapsedTime >= 1200_000 }),
        AchievementDef("time_30min", "Media Hora", "Juega durante 30 minutos seguidos.", 0xFFFF6B6BL, { it.elapsedTime >= 1800_000 }),
        AchievementDef("score_500", "Medio Millar", "Alcanza 500 puntos.", 0xFF81B29AL, { it.score >= 500 }),
        AchievementDef("score_1k", "Primer Millar", "Alcanza 1,000 puntos.", 0xFF81B29AL, { it.score >= 1000 }),
        AchievementDef("score_2500", "Dos Mil Quinientos", "Alcanza 2,500 puntos.", 0xFFC0C0C0L, { it.score >= 2500 }),
        AchievementDef("score_5k", "Cinco Mil", "Alcanza 5,000 puntos.", 0xFFC0C0C0L, { it.score >= 5000 }),
        AchievementDef("score_7500", "Siete Mil Quinientos", "Alcanza 7,500 puntos.", 0xFFFFD700L, { it.score >= 7500 }),
        AchievementDef("score_10k", "Diez Mil Maestro", "Alcanza 10,000 puntos.", 0xFFFFD700L, { it.score >= 10000 }),
        AchievementDef("score_15k", "Quince Mil", "Alcanza 15,000 puntos.", 0xFF9C27B0L, { it.score >= 15000 }),
        AchievementDef("score_25k", "Veinticinco Mil", "Alcanza 25,000 puntos.", 0xFF9C27B0L, { it.score >= 25000 }),
        AchievementDef("score_50k", "Cincuenta Mil", "Alcanza 50,000 puntos.", 0xFFFF6B6BL, { it.score >= 50000 }),
        AchievementDef("score_100k", "Cien Mil Legendario", "Alcanza 100,000 puntos.", 0xFFFF1744L, { it.score >= 100000 }),
        AchievementDef("combo_3", "Triple Combo", "Consigue un combo de 3 fusiones.", 0xFFFF9800L, { it.combo >= 3 }),
        AchievementDef("combo_5", "Penta Combo", "Consigue un combo de 5 fusiones.", 0xFFFF5722L, { it.combo >= 5 }),
        AchievementDef("combo_7", "Hepta Combo", "Consigue un combo de 7 fusiones.", 0xFFE91E63L, { it.combo >= 7 }),
        AchievementDef("combo_10", "Deca Combo", "Consigue un combo de 10 fusiones.", 0xFFFFD700L, { it.combo >= 10 }),
        AchievementDef("combo_15", "Mega Combo", "Consigue un combo de 15 fusiones.", 0xFF9C27B0L, { it.combo >= 15 }),
        AchievementDef("combo_20", "Ultra Combo", "Consigue un combo de 20 fusiones.", 0xFFFF6B6BL, { it.combo >= 20 }),
        AchievementDef("combo_30", "Hyper Combo", "Consigue un combo de 30 fusiones.", 0xFFFF1744L, { it.combo >= 30 }),
        AchievementDef("full_board", "Tablero Lleno", "Llena todo el tablero sin perder.", 0xFF6C63FFL, { it.emptySpaces == 0 && it.hasMovesAvailable }),
        AchievementDef("minimal_tiles", "Minimalista", "Gana con menos de 5 fichas en el tablero.", 0xFF607D8BL, { it.isLevelCompleted && it.tiles.size < 5 }),
        AchievementDef("clutch_victory", "Victoria In Extremis", "Gana con solo 1 espacio vacío.", 0xFFFF5722L, { it.isLevelCompleted && it.emptySpaces == 1 }),
        AchievementDef("close_call", "Por los Pelos", "Gana con 2 espacios vacíos o menos.", 0xFFFF9800L, { it.isLevelCompleted && it.emptySpaces <= 2 }),
        AchievementDef("efficiency_king", "Rey de la Eficiencia", "Completa un nivel sin movimientos desperdiciados.", 0xFF9C27B0L, { it.isLevelCompleted && it.moveCount <= it.currentLevel * 3 }),
        AchievementDef("classic_beginner", "Clásico Nivel 5", "Alcanza nivel 5 en modo Clásico.", 0xFF81B29AL, { it.gameMode == GameMode.CLASICO && it.isLevelCompleted && it.currentLevel == 4 }),
        AchievementDef("classic_intermediate", "Clásico Nivel 10", "Alcanza nivel 10 en modo Clásico.", 0xFF81B29AL, { it.gameMode == GameMode.CLASICO && it.isLevelCompleted && it.currentLevel == 9 }),
        AchievementDef("classic_master", "Maestro Clásico", "Alcanza nivel 20 en modo Clásico.", 0xFF81B29AL, { it.gameMode == GameMode.CLASICO && it.isLevelCompleted && it.currentLevel == 19 }),
        AchievementDef("challenge_completed", "Desafío Superado", "Completa un nivel en modo Desafío.", 0xFFE07A5FL, { it.gameMode == GameMode.DESAFIO && it.isLevelCompleted }),
        AchievementDef("challenge_level_5", "Desafío Nivel 5", "Alcanza nivel 5 en modo Desafío.", 0xFFE07A5FL, { it.gameMode == GameMode.DESAFIO && it.isLevelCompleted && it.currentLevel == 4 }),
        AchievementDef("zen_master", "Maestro Zen", "Alcanza nivel 5 en modo Zen.", 0xFF6C63FFL, { it.gameMode == GameMode.ZEN && it.isLevelCompleted && it.currentLevel == 4 }),
        AchievementDef("zen_enlightened", "Iluminado", "Alcanza nivel 10 en modo Zen.", 0xFF6C63FFL, { it.gameMode == GameMode.ZEN && it.isLevelCompleted && it.currentLevel == 9 }),
        AchievementDef("rapid_fire", "Fuego Rápido", "Completa 3 niveles en modo Rápido.", 0xFFF4A261L, { it.gameMode == GameMode.RAPIDO && it.isLevelCompleted && it.currentLevel == 3 }),
        AchievementDef("speed_demon", "Demonio de Velocidad", "Alcanza nivel 10 en modo Rápido.", 0xFFF4A261L, { it.gameMode == GameMode.RAPIDO && it.isLevelCompleted && it.currentLevel == 9 }),
        AchievementDef("lucky_seven", "Siete de la Suerte", "Completa exactamente en el nivel 7.", 0xFF4CAF50L, { it.currentLevel == 7 && it.isLevelCompleted }),
        AchievementDef("thirteen", "Trece Escalofriante", "Alcanza el nivel 13.", 0xFF212121L, { it.isLevelCompleted && it.currentLevel == 12 }),
        AchievementDef("double_double", "Doble Doble", "Ten dos pares de fichas del mismo valor.", 0xFF3F51B5L, { it.tiles.groupBy { tile -> tile.value } .count { group -> group.value.size >= 2 } >= 2 }),
        AchievementDef("quad_squad", "Escuadrón Cuádruple", "Ten 4 fichas del mismo valor simultáneamente.", 0xFF9C27B0L, { it.tiles.groupBy { tile -> tile.value }.any { group -> group.value.size >= 4 } }),
        AchievementDef("corner_king", "Rey de las Esquinas", "Ten fichas en las 4 esquinas del tablero.", 0xFF795548L, { ctx -> listOf(0 to 0, 0 to (ctx.boardSize - 1), (ctx.boardSize - 1) to 0, (ctx.boardSize - 1) to (ctx.boardSize - 1)).all { corner -> ctx.tiles.any { tile -> tile.row == corner.first && tile.col == corner.second } } }),
        AchievementDef("power_of_two", "Potencia de Dos", "Ten solo fichas que sean potencias de 2 puras.", 0xFF00BCD4L, { ctx -> ctx.tiles.isNotEmpty() && ctx.tiles.all { tile -> tile.value > 0 && (tile.value and (tile.value - 1)) == 0 } }),
        AchievementDef("symmetry", "Simetría Perfecta", "Ten el tablero con simetría horizontal.", 0xFF009688L, { ctx -> ctx.tiles.all { tile -> ctx.tiles.find { t -> t.col == tile.col && t.row == (ctx.boardSize - 1 - tile.row) }?.value == tile.value } }),
        AchievementDef("center_piece", "Pieza Central", "Crea tu ficha más alta en el centro del tablero.", 0xFFFF6B6BL, { ctx -> val top = ctx.tiles.maxByOrNull { tile -> tile.value }; val center = ctx.boardSize / 2; top?.let { tile -> (tile.row == center || tile.row == center - 1) && (tile.col == center || tile.col == center - 1) } ?: false }),
        AchievementDef("edge_lord", "Señor de los Bordes", "Ten todas las fichas en los bordes del tablero.", 0xFF607D8BL, { it.tiles.isNotEmpty() && it.tiles.all { tile -> tile.row == 0 || tile.row == it.boardSize - 1 || tile.col == 0 || tile.col == it.boardSize - 1 } }),
        AchievementDef("fibonacci", "Fibonacci Fan", "Ten fichas con valores de la secuencia Fibonacci.", 0xFFFFEB3BL, { ctx -> val fib = listOf(2, 8, 32, 128, 512, 2048); ctx.tiles.any { tile -> tile.value in fib } })
    )

    fun byId(id: String): AchievementDef? = all.firstOrNull { it.id == id }

    /** Categoría de un logro: decide su rareza (y por tanto su premio) según su posición dentro de ella. */
    fun categoryOf(id: String): String = when {
        id == "first_win" || id == "getting_warmed_up" || id.startsWith("level_") ||
            id.startsWith("classic_") || id.startsWith("challenge_") || id.startsWith("zen_") -> "progress"
        id.startsWith("tile_") || id in setOf("lucky_seven", "thirteen", "double_double", "quad_squad", "power_of_two", "fibonacci") -> "tiles"
        id == "speedrun" || id.startsWith("speed_") || id in setOf("time_60", "time_45", "time_30", "time_15", "rapid_fire", "efficiency_king") -> "speed"
        id == "century" || id.startsWith("moves_") || id.endsWith("min") -> "endurance"
        id.startsWith("score_") -> "score"
        id.startsWith("combo_") -> "combo"
        else -> "special"
    }

    /** Rareza de cada logro (misma regla que Android): según su lugar dentro de su categoría. */
    val rarity: Map<String, com.korkoor.pardos.domain.collection.Rarity> by lazy {
        buildMap {
            all.groupBy { categoryOf(it.id) }.values.forEach { list ->
                list.forEachIndexed { i, a -> put(a.id, com.korkoor.pardos.domain.economy.Economy.achievementRarity(i, list.size)) }
            }
        }
    }
}
