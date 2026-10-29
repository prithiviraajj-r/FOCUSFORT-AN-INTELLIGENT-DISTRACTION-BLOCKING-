package com.example.focusfortvs.blocking

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages timed per-app whitelisting during Focus Sessions.
 * Grants a specific app a time-boxed window (e.g., 3 mins for Normal, 1-10 mins for Office) to bypass the block overlay.
 * All state is in-memory; expires automatically when FocusSessionService polls the clock.
 */
@Singleton
class AppWhitelistManager @Inject constructor() {

    /** packageName -> expiry epoch millis */
    private val whitelistExpiry = mutableMapOf<String, Long>()

    /**
     * Grants [packageName] access for [minutes] minutes starting now.
     */
    fun grant(packageName: String, minutes: Int) {
        val expiryMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
        whitelistExpiry[packageName] = expiryMillis
    }

    /**
     * Returns true if [packageName] currently has an active whitelist entry.
     */
    fun isWhitelisted(packageName: String): Boolean {
        val expiry = whitelistExpiry[packageName] ?: return false
        return System.currentTimeMillis() < expiry
    }

    /**
     * Returns remaining whitelist seconds for [packageName], or 0 if expired / not present.
     */
    fun remainingSeconds(packageName: String): Long {
        val expiry = whitelistExpiry[packageName] ?: return 0L
        val remaining = (expiry - System.currentTimeMillis()) / 1000L
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Explicitly revokes whitelist for [packageName] (e.g., session ended).
     */
    fun revoke(packageName: String) {
        whitelistExpiry.remove(packageName)
    }

    /** Revokes all whitelist entries (called on session stop). */
    fun revokeAll() {
        whitelistExpiry.clear()
    }
}
