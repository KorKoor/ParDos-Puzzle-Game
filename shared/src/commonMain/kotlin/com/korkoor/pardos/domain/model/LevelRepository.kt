package com.korkoor.pardos.domain.model

import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelSpec

object LevelRepository {
    /** Los niveles de la campaña, tal como los define [LevelCatalog] (solo el primero empieza desbloqueado). */
    fun getGeneratedLevels(): List<LevelInfo> = LevelCatalog.all().map(::infoFor)

    fun infoFor(spec: LevelSpec) = LevelInfo(
        id = spec.id,
        target = spec.goalValue,
        isLocked = spec.id > 1,
        difficultyName = spec.title,
        maxTime = spec.timeLimitMs?.let { it / 1000 },
        spec = spec
    )
}
