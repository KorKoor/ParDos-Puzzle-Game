package com.korkoor.pardos.domain.logic

/** Formato corto para compartir una configuración de partida personalizada. */
data class CustomLevelConfig(
    val size: Int,
    val target: Int,
    val allowPowerUps: Boolean,
    val difficulty: String
)

object CustomLevelCode {
    private const val PREFIX = "PDC1"
    private val validDifficulties = setOf("Zen", "Normal", "Pro")

    fun encode(config: CustomLevelConfig): String? {
        if (!isValid(config)) return null
        val payload = "$PREFIX-${config.size}-${config.target}-${if (config.allowPowerUps) 1 else 0}-${config.difficulty.uppercase()}"
        return "$payload-${checksum(payload)}"
    }

    /** Acepta el código solo o dentro de un mensaje compartido. */
    fun decode(text: String): CustomLevelConfig? {
        val match = Regex("PDC1-[0-9]{1,2}-[0-9]{1,4}-[01]-(ZEN|NORMAL|PRO)-[0-9A-Z]{2}")
            .find(text.uppercase()) ?: return null
        val parts = match.value.split('-')
        val payload = parts.dropLast(1).joinToString("-")
        if (checksum(payload) != parts.last()) return null
        val config = CustomLevelConfig(
            size = parts[1].toIntOrNull() ?: return null,
            target = parts[2].toIntOrNull() ?: return null,
            allowPowerUps = parts[3] == "1",
            difficulty = parts[4].lowercase().replaceFirstChar { it.uppercase() }
        )
        return config.takeIf(::isValid)
    }

    private fun isValid(config: CustomLevelConfig): Boolean =
        config.size in 3..6 && config.target in setOf(64, 128, 256, 512) && config.difficulty in validDifficulties

    private fun checksum(payload: String): String {
        var hash = 2166136261L
        payload.forEach { hash = ((hash xor it.code.toLong()) * 0x01000193L) and 0xFFFFFFFFL }
        val value = (hash % 1296).toInt()
        val digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return "${digits[value / 36]}${digits[value % 36]}"
    }
}
