package com.petsafety.app.ui.util

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for [resolveCameraTarget] and [CountryCenters].
 *
 * These guard the priority order pin → user → country → null and the
 * country-code lookup so we never regress to hardcoded London.
 */
class AdaptiveMapCameraTest {

    private val pin = LatLng(47.5, 19.0)
    private val user = LatLng(48.0, 19.5)

    @Test
    fun `pin wins over user location and country`() {
        val target = resolveCameraTarget(
            pinLatLng = pin,
            userLocation = user,
            userCountryCode = "hu",
        )
        assertNotNull(target)
        assertEquals(pin, target!!.latLng)
        assertEquals(11f, target.zoom)
    }

    @Test
    fun `user location wins when no pin`() {
        val target = resolveCameraTarget(
            pinLatLng = null,
            userLocation = user,
            userCountryCode = "hu",
        )
        assertNotNull(target)
        assertEquals(user, target!!.latLng)
        assertEquals(11f, target.zoom)
    }

    @Test
    fun `country center used when no pin and no user location`() {
        val target = resolveCameraTarget(
            pinLatLng = null,
            userLocation = null,
            userCountryCode = "hu",
        )
        assertNotNull(target)
        assertEquals(LatLng(47.1625, 19.5033), target!!.latLng)
        assertEquals(6f, target.zoom)
    }

    @Test
    fun `null when nothing known — never falls back to a hardcoded city`() {
        val target = resolveCameraTarget(
            pinLatLng = null,
            userLocation = null,
            userCountryCode = null,
        )
        assertNull(target)
    }

    @Test
    fun `unknown country code returns null target — no London fallback`() {
        val target = resolveCameraTarget(
            pinLatLng = null,
            userLocation = null,
            userCountryCode = "zz",
        )
        assertNull(target)
    }

    @Test
    fun `country code lookup is case-insensitive`() {
        val lower = CountryCenters.centerFor("hu")
        val upper = CountryCenters.centerFor("HU")
        val mixed = CountryCenters.centerFor("Hu")
        assertEquals(lower, upper)
        assertEquals(lower, mixed)
    }

    @Test
    fun `country code lookup trims whitespace`() {
        assertEquals(
            CountryCenters.centerFor("hu"),
            CountryCenters.centerFor("  hu  "),
        )
    }

    @Test
    fun `country code lookup returns null for blank input`() {
        assertNull(CountryCenters.centerFor(""))
        assertNull(CountryCenters.centerFor("   "))
        assertNull(CountryCenters.centerFor(null))
    }

    @Test
    fun `all 12 supported product locales have country centers`() {
        // Glossary covers EN/SK/HU/CZ/DE/HR/RO/IT/ES/PT/FR/NO/PL.
        // Add a separate row for AT (uses DE app) and GB (English fallback).
        val expected = listOf("hu", "sk", "cz", "de", "at", "es", "pt", "ro", "it", "fr", "pl", "no", "hr", "gb", "us")
        expected.forEach { code ->
            assertNotNull("missing country center for '$code'", CountryCenters.centerFor(code))
        }
    }

    @Test
    fun `custom zoom levels are respected`() {
        val target = resolveCameraTarget(
            pinLatLng = pin,
            userLocation = null,
            userCountryCode = null,
            pinZoom = 15f,
        )
        assertEquals(15f, target!!.zoom)
    }
}
