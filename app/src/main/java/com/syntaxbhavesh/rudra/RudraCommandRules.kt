package com.syntaxbhavesh.rudra

import java.util.Locale

/**
 * Small, deterministic command rules shared by the app and JVM unit tests.
 * These rules validate recognised phrases; they never execute actions.
 */
object RudraCommandRules {
    fun isWakePhrase(value: String): Boolean {
        val normalized = value.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.contains("hey rudra") ||
            normalized.contains("hey roodra") ||
            normalized.contains("hey rudrah")
    }

    /** Returns a valid delay in seconds, or null when the duration is out of range. */
    fun reminderDelaySeconds(amount: Long, unit: String): Long? {
        if (amount <= 0) return null
        val normalizedUnit = unit.lowercase(Locale.ROOT)
        val multiplier = when {
            normalizedUnit.startsWith("hour") || normalizedUnit.startsWith("hr") -> 3600L
            normalizedUnit.startsWith("min") -> 60L
            normalizedUnit.startsWith("second") || normalizedUnit.startsWith("sec") -> 1L
            else -> return null
        }
        if (amount > 604800L / multiplier) return null
        return (amount * multiplier).takeIf { it in 10L..604800L }
    }
}
