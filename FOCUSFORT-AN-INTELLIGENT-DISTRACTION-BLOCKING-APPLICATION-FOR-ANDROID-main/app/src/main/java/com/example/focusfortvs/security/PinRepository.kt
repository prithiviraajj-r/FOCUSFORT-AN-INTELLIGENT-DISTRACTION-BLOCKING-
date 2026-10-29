package com.example.focusfortvs.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores and verifies the user's 4-digit guardian PIN.
 * The PIN is never stored in plain text — only its SHA-256 hash is persisted.
 */
@Singleton
class PinRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME   = "focusfort_pin"
        private const val KEY_PIN_HASH = "pin_hash"
    }

    private val prefs get() =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns true if the user has set a PIN. */
    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    /**
     * Saves a new PIN. Call only after the old PIN has been verified
     * (or on first-time setup when no PIN exists yet).
     */
    fun savePin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    /** Returns true if [pin] matches the stored PIN. */
    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    /** Clears the stored PIN (used only for testing / account reset). */
    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
