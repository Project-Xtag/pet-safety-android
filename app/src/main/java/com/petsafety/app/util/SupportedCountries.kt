package com.petsafety.app.util

data class SupportedCountry(val code: String) {
    /** Returns the country name localized to the user's current locale. */
    fun localizedName(locale: java.util.Locale = java.util.Locale.getDefault()): String =
        java.util.Locale("", code).getDisplayCountry(locale).ifBlank { code }
}

object SupportedCountries {
    /** ISO 3166-1 alpha-2 codes for supported shipping destinations. */
    private val codes: List<String> = listOf(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
        "DE", "GR", "HU", "IT", "LV", "LT", "LU", "MT", "NL", "NO",
        "PL", "PT", "RO", "SK", "SI", "ES", "SE", "CH",
    )

    val all: List<SupportedCountry> = codes.map { SupportedCountry(it) }

    /**
     * Countries sorted by localized name, with [priorityCode] placed first if provided.
     */
    fun sorted(priorityCode: String? = null): List<SupportedCountry> {
        val locale = java.util.Locale.getDefault()
        val sorted = all.sortedBy { it.localizedName(locale).lowercase(locale) }
        val priority = priorityCode?.uppercase()
        if (priority != null) {
            val idx = sorted.indexOfFirst { it.code.equals(priority, ignoreCase = true) }
            if (idx > 0) {
                return sorted.toMutableList().apply {
                    add(0, removeAt(idx))
                }
            }
        }
        return sorted
    }

    fun find(value: String): SupportedCountry? {
        val v = value.trim()
        return all.firstOrNull { it.code.equals(v, ignoreCase = true) }
            ?: all.firstOrNull { it.localizedName().equals(v, ignoreCase = true) }
    }

    fun findByCode(code: String): SupportedCountry? =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) }
}
