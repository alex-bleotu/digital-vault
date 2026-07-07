package com.digitalvault.core.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val encoded = factory.generateSecret(spec).encoded
            return Base64.encodeToString(encoded, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        return constantTimeEquals(hash(password, salt), expectedHash)
    }

    private fun constantTimeEquals(first: String, second: String): Boolean {
        val firstBytes = first.toByteArray()
        val secondBytes = second.toByteArray()
        if (firstBytes.size != secondBytes.size) {
            return false
        }
        var difference = 0
        for (index in firstBytes.indices) {
            difference = difference or (firstBytes[index].toInt() xor secondBytes[index].toInt())
        }
        return difference == 0
    }
}
