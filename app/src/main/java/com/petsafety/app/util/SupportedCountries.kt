package com.petsafety.app.util

data class SupportedCountry(val code: String, val name: String)

object SupportedCountries {
    val all: List<SupportedCountry> = listOf(
        SupportedCountry("AT", "Austria"),
        SupportedCountry("BE", "Belgium"),
        SupportedCountry("BG", "Bulgaria"),
        SupportedCountry("HR", "Croatia"),
        SupportedCountry("CY", "Cyprus"),
        SupportedCountry("CZ", "Czech Republic"),
        SupportedCountry("DK", "Denmark"),
        SupportedCountry("EE", "Estonia"),
        SupportedCountry("FI", "Finland"),
        SupportedCountry("FR", "France"),
        SupportedCountry("DE", "Germany"),
        SupportedCountry("GR", "Greece"),
        SupportedCountry("HU", "Hungary"),
        SupportedCountry("IT", "Italy"),
        SupportedCountry("LV", "Latvia"),
        SupportedCountry("LT", "Lithuania"),
        SupportedCountry("LU", "Luxembourg"),
        SupportedCountry("MT", "Malta"),
        SupportedCountry("NL", "Netherlands"),
        SupportedCountry("NO", "Norway"),
        SupportedCountry("PL", "Poland"),
        SupportedCountry("PT", "Portugal"),
        SupportedCountry("RO", "Romania"),
        SupportedCountry("SK", "Slovakia"),
        SupportedCountry("SI", "Slovenia"),
        SupportedCountry("ES", "Spain"),
        SupportedCountry("SE", "Sweden"),
        SupportedCountry("CH", "Switzerland"),
    )

    fun find(value: String): SupportedCountry? {
        val v = value.trim()
        return all.firstOrNull { it.code.equals(v, ignoreCase = true) }
            ?: all.firstOrNull { it.name.equals(v, ignoreCase = true) }
    }

    fun findByCode(code: String): SupportedCountry? =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) }
}
