package com.korkoor.pardos.domain.rewards

/** Cofre que se abre al terminar el último nivel de cada capítulo del mapa. */
object ChapterRewards {
    const val LEVELS_PER_CHAPTER = 20

    /** Capítulo (0-based) al que pertenece un nivel (1-based). */
    fun chapterOf(level: Int): Int = (level.coerceAtLeast(1) - 1) / LEVELS_PER_CHAPTER

    /** Último nivel del capítulo, el que abre su cofre. */
    fun lastLevelOf(chapter: Int): Int = (chapter + 1) * LEVELS_PER_CHAPTER

    /** Premio del cofre: crece con el capítulo y siempre da gemas. */
    fun forChapter(chapter: Int): Reward {
        val c = chapter.coerceAtLeast(0)
        return Reward(coins = 100 + 50 * c, gems = 2 + c / 3, isChest = true)
    }
}
