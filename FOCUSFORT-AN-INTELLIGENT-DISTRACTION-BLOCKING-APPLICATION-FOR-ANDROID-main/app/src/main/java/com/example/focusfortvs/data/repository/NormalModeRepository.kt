package com.example.focusfortvs.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks usage counts and hard-locks for Normal Mode.
 * Persists data to SharedPreferences so it survives app kills (anti-cheat).
 */
@Singleton
class NormalModeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("normal_mode_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_USAGE_COUNT = 5
        private const val KEY_USAGE_PREFIX = "usage_count_"
        private const val KEY_HARD_LOCK_PREFIX = "hard_lock_"
        private const val KEY_COOLDOWN_PREFIX = "cooldown_"
    }

    /**
     * Starts a 60-second cooldown for the given package name.
     */
    fun startCooldown(packageName: String) {
        val expiryMillis = System.currentTimeMillis() + 60_000L
        prefs.edit().putLong(KEY_COOLDOWN_PREFIX + packageName, expiryMillis).apply()
    }

    /**
     * Returns remaining cooldown seconds, or 0 if expired/not active.
     */
    fun getCooldownRemainingSeconds(packageName: String): Long {
        val expiry = prefs.getLong(KEY_COOLDOWN_PREFIX + packageName, 0L)
        val remaining = (expiry - System.currentTimeMillis()) / 1000L
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Increments the usage count for the given package name.
     * Returns the new usage count.
     */
    fun incrementUsage(packageName: String): Int {
        val current = getUsageCount(packageName)
        val newCount = current + 1
        prefs.edit().putInt(KEY_USAGE_PREFIX + packageName, newCount).apply()
        return newCount
    }

    /**
     * Returns the current usage count for the given package name.
     */
    fun getUsageCount(packageName: String): Int {
        return prefs.getInt(KEY_USAGE_PREFIX + packageName, 0)
    }

    /**
     * Applies a hard lock for the given package name until the specified epoch time.
     */
    fun applyHardLock(packageName: String, expiryTimeMillis: Long) {
        prefs.edit().putLong(KEY_HARD_LOCK_PREFIX + packageName, expiryTimeMillis).apply()
    }

    /**
     * Returns the epoch time when the hard lock expires, or 0 if not hard locked.
     */
    fun getHardLockExpiry(packageName: String): Long {
        return prefs.getLong(KEY_HARD_LOCK_PREFIX + packageName, 0L)
    }

    /**
     * Checks if the app is currently hard locked.
     */
    fun isHardLocked(packageName: String): Boolean {
        val expiry = getHardLockExpiry(packageName)
        return System.currentTimeMillis() < expiry
    }

    /**
     * Returns remaining hard lock seconds.
     */
    fun getRemainingHardLockSeconds(packageName: String): Long {
        val expiry = getHardLockExpiry(packageName)
        val remaining = (expiry - System.currentTimeMillis()) / 1000L
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Clears all normal mode session data (called when a session officially ends/starts).
     */
    fun clearSessionData() {
        prefs.edit().clear().apply()
    }
}
