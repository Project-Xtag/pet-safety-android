package com.petsafety.app.util

/**
 * Canonical email normalization for the Android client.
 *
 * Mirrors the backend's `normalizeEmail` helper. Trim + lowercase. Apply at
 * every call site that sends an email to the server (login, verify-otp,
 * register) so the same human address always hits the same backend row,
 * regardless of how the user typed it.
 *
 * Pre-fix, an iOS user logging in on Android with the same address but
 * different casing got a freshly auto-provisioned empty user — the bug
 * behind "I logged in on Android and my pets are gone."
 */
object EmailNormalizer {

    fun normalize(input: String?): String {
        if (input == null) return ""
        return input.trim().lowercase()
    }
}
