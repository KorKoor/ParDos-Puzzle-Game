package com.korkoor.pardos

import com.korkoor.pardos.domain.model.MissionPool
import com.korkoor.pardos.domain.model.MissionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissionPoolTest {
    private val all = MissionPool.allMissions

    @Test fun idsAreUnique() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test fun everyMissionIsReachableAndPaysSomething() {
        all.forEach {
            assertTrue(it.targetValue > 0, "meta de ${it.id}")
            assertTrue(it.xpReward > 0, "premio de ${it.id}")
            assertTrue(it.description.isNotBlank(), "texto de ${it.id}")
            assertEquals(0, it.currentProgress, "el catálogo no arrastra progreso (${it.id})")
        }
    }

    @Test fun everyTypeHasAtLeastTwoMissions() {
        MissionType.entries.forEach { type ->
            assertTrue(all.count { it.type == type } >= 2, "pocas misiones de $type")
        }
    }

    @Test fun harderMissionsOfTheSameTypePayMore() {
        // Con el mismo tipo, más esfuerzo debe dar más premio (en tiempo, menos segundos = más esfuerzo)
        MissionType.entries.forEach { type ->
            val sorted = all.filter { it.type == type }.let { list ->
                if (type == MissionType.WIN_UNDER_TIME) list.sortedByDescending { it.targetValue } else list.sortedBy { it.targetValue }
            }
            sorted.zipWithNext().forEach { (a, b) -> assertTrue(b.xpReward >= a.xpReward, "$type: ${a.id}→${b.id}") }
        }
    }
}
