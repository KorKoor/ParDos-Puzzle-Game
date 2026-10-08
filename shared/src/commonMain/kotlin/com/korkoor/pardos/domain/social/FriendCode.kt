package com.korkoor.pardos.domain.social

import kotlin.random.Random

/** Código corto para agregar amigos. Sin caracteres que se confunden (0/O, 1/I/L). */
object FriendCode {
    const val LENGTH = 8
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = Random.Default): String =
        buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** Limpia lo que escribe el jugador: mayúsculas, sin espacios ni guiones. */
    fun normalize(input: String): String = input.uppercase().filter { it.isLetterOrDigit() }

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }
}
