package com.petsafety.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive tests for SupportedCountries utility.
 * Verifies the country list, lookup methods, and that only
 * EU27 (minus Ireland) + Norway + Switzerland are included.
 */
class SupportedCountriesTest {

    // MARK: - Country List Composition

    @Test
    fun `country list has exactly 28 entries`() {
        assertEquals(28, SupportedCountries.all.size)
    }

    @Test
    fun `all EU27 members except Ireland are present`() {
        val euCodes = setOf(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IT", "LV", "LT", "LU", "MT", "NL", "PL",
            "PT", "RO", "SK", "SI", "ES", "SE"
        )
        val actualCodes = SupportedCountries.all.map { it.code }.toSet()
        for (code in euCodes) {
            assertTrue("Missing EU country: $code", actualCodes.contains(code))
        }
    }

    @Test
    fun `Norway and Switzerland are included`() {
        assertNotNull(SupportedCountries.findByCode("NO"))
        assertNotNull(SupportedCountries.findByCode("CH"))
    }

    @Test
    fun `Ireland is not included`() {
        assertNull(SupportedCountries.findByCode("IE"))
    }

    @Test
    fun `United Kingdom is not included`() {
        assertNull(SupportedCountries.findByCode("GB"))
        assertNull(SupportedCountries.findByCode("UK"))
    }

    @Test
    fun `non-European countries are not included`() {
        assertNull(SupportedCountries.findByCode("US"))
        assertNull(SupportedCountries.findByCode("JP"))
        assertNull(SupportedCountries.findByCode("CN"))
        assertNull(SupportedCountries.findByCode("AU"))
        assertNull(SupportedCountries.findByCode("BR"))
        assertNull(SupportedCountries.findByCode("IN"))
    }

    @Test
    fun `no duplicate country codes`() {
        val codes = SupportedCountries.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `all country codes are 2-letter ISO format`() {
        for (country in SupportedCountries.all) {
            assertEquals("Code '${country.code}' should be 2 chars", 2, country.code.length)
            assertTrue("Code '${country.code}' should be uppercase", country.code == country.code.uppercase())
        }
    }

    @Test
    fun `all country names are non-empty`() {
        for (country in SupportedCountries.all) {
            assertTrue("Country '${country.code}' has empty name", country.name.isNotBlank())
        }
    }

    // MARK: - find() Lookup

    @Test
    fun `find by code - case insensitive`() {
        assertNotNull(SupportedCountries.find("HU"))
        assertNotNull(SupportedCountries.find("hu"))
        assertNotNull(SupportedCountries.find("Hu"))
    }

    @Test
    fun `find by name - case insensitive`() {
        val result = SupportedCountries.find("Hungary")
        assertNotNull(result)
        assertEquals("HU", result?.code)

        val resultLower = SupportedCountries.find("hungary")
        assertNotNull(resultLower)
        assertEquals("HU", resultLower?.code)
    }

    @Test
    fun `find returns null for unknown values`() {
        assertNull(SupportedCountries.find("XX"))
        assertNull(SupportedCountries.find("Atlantis"))
        assertNull(SupportedCountries.find(""))
    }

    @Test
    fun `find trims whitespace`() {
        assertNotNull(SupportedCountries.find("  HU  "))
        assertNotNull(SupportedCountries.find("  Hungary  "))
    }

    // MARK: - findByCode() Lookup

    @Test
    fun `findByCode returns correct country`() {
        val hungary = SupportedCountries.findByCode("HU")
        assertNotNull(hungary)
        assertEquals("Hungary", hungary?.name)

        val germany = SupportedCountries.findByCode("DE")
        assertNotNull(germany)
        assertEquals("Germany", germany?.name)
    }

    @Test
    fun `findByCode is case insensitive`() {
        assertEquals(
            SupportedCountries.findByCode("HU"),
            SupportedCountries.findByCode("hu")
        )
    }

    @Test
    fun `findByCode returns null for unknown codes`() {
        assertNull(SupportedCountries.findByCode("XX"))
        assertNull(SupportedCountries.findByCode(""))
        assertNull(SupportedCountries.findByCode("USA"))
    }

    // MARK: - Known Countries Spot Checks

    @Test
    fun `spot check known countries exist with correct names`() {
        assertEquals("Hungary", SupportedCountries.findByCode("HU")?.name)
        assertEquals("Germany", SupportedCountries.findByCode("DE")?.name)
        assertEquals("France", SupportedCountries.findByCode("FR")?.name)
        assertEquals("Spain", SupportedCountries.findByCode("ES")?.name)
        assertEquals("Norway", SupportedCountries.findByCode("NO")?.name)
        assertEquals("Switzerland", SupportedCountries.findByCode("CH")?.name)
        assertEquals("Poland", SupportedCountries.findByCode("PL")?.name)
        assertEquals("Romania", SupportedCountries.findByCode("RO")?.name)
    }
}
