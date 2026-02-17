package com.petsafety.app.util

import java.util.Locale

/**
 * Maps device locale to Senra country code and builds country-prefixed web URLs.
 */
object WebUrlHelper {
    private val regionToCountry = mapOf(
        "GB" to "uk", "US" to "uk",
        "HU" to "hu", "SK" to "sk",
        "AT" to "at", "DE" to "de", "CH" to "de",
        "CZ" to "cz",
        "ES" to "es", "MX" to "es", "AR" to "es", "CO" to "es", "CL" to "es",
        "PT" to "pt", "BR" to "pt",
        "FR" to "fr",
        "IT" to "it",
        "PL" to "pl",
        "RO" to "ro", "MD" to "ro",
        "HR" to "hr",
    )

    val validCountryCodes = setOf(
        "uk", "hu", "sk", "at", "de", "cz", "es", "pt", "fr", "it", "pl", "ro", "hr"
    )

    val countryCode: String
        get() {
            val region = Locale.getDefault().country.uppercase()
            return regionToCountry[region] ?: "uk"
        }

    fun url(path: String): String {
        val clean = if (path.startsWith("/")) path else "/$path"
        return "https://senra.pet/$countryCode$clean"
    }

    val termsUrl: String get() = url("/terms-conditions")
    val privacyUrl: String get() = url("/privacy-policy")

    /**
     * Strips a valid country prefix from a URL path.
     * e.g. "/hu/qr/ABC123" -> "/qr/ABC123"
     */
    fun stripCountryPrefix(path: String): String {
        val segments = path.trimStart('/').split("/", limit = 2)
        if (segments.isNotEmpty() && validCountryCodes.contains(segments[0])) {
            return if (segments.size > 1) "/${segments[1]}" else "/"
        }
        return path
    }
}
