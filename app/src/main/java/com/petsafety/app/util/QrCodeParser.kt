package com.petsafety.app.util

import android.net.Uri

/**
 * Canonical QR code parsing for the Android app. This is the single place
 * all scanned / deep-linked strings should flow through before hitting the
 * API. Mirrors the backend's `extractTagCode` and web's `lib/extractTagCode.ts`
 * so all three layers agree on what counts as a valid scan.
 *
 * Tag codes are case-sensitive (nanoid alphabet includes both cases).
 * Only the URL scaffolding around a code is matched case-insensitively.
 *
 * Handles:
 *   - Bare code:             "T5NAJlr2"
 *   - Prod URL:              "https://senra.pet/t/T5NAJlr2"
 *   - Uppercase path:        "HTTPS://SENRA.PET/T/T5NAJlr2"
 *   - /qr/ legacy form:      "https://senra.pet/qr/PS-12345678"
 *   - Country-prefixed:      "https://senra.pet/hu/t/T5NAJlr2"
 *   - Custom scheme:         "senra://tag/T5NAJlr2"
 *   - Trailing slash:        "https://senra.pet/t/T5NAJlr2/"
 *   - Query / fragment:      "...T5NAJlr2?utm=1#found"
 *   - Whitespace padding
 *
 * Foreign hosts (not senra.pet) are passed through unchanged — we do not
 * make any claims about their structure. The downstream lookup will just
 * fail closed for anything that isn't a real tag.
 */
object QrCodeParser {

    private val TAG_CODE_REGEX = Regex("^[A-Za-z0-9_-]{6,32}$")
    private val KNOWN_HOSTS = setOf("senra.pet", "www.senra.pet")
    private val CODE_PATH_PREFIXES = setOf("t", "qr")

    /**
     * Extract a bare tag code from any scan input. Returns the trimmed
     * input unchanged if it doesn't match a recognized URL pattern — the
     * caller should then validate with [isValidTagCode] to fail closed.
     */
    fun extractTagCode(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return trimmed

        return try {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme?.lowercase()

            // Custom scheme: senra://tag/<CODE>
            if (scheme == "senra" && uri.host?.lowercase() == "tag") {
                return uri.lastPathSegment ?: trimmed
            }

            // HTTP/HTTPS: must be a known host
            val host = uri.host?.lowercase() ?: return trimmed
            if (host !in KNOWN_HOSTS) return trimmed

            val segments = uri.pathSegments?.filter { it.isNotEmpty() } ?: return trimmed
            if (segments.size < 2) return trimmed

            // /t/CODE or /qr/CODE
            val first = segments[0].lowercase()
            if (first in CODE_PATH_PREFIXES) return segments[1]

            // /:country/t/CODE or /:country/qr/CODE (country is always 2 chars)
            if (segments.size >= 3 && segments[0].length == 2) {
                val second = segments[1].lowercase()
                if (second in CODE_PATH_PREFIXES) return segments[2]
            }

            trimmed
        } catch (_: Exception) {
            trimmed
        }
    }

    /**
     * Validate a tag code against the canonical shape. Use at any boundary
     * (user input, deep-link payload) where an invalid code should fail
     * fast rather than hitting the network.
     */
    fun isValidTagCode(code: String): Boolean = TAG_CODE_REGEX.matches(code)
}
