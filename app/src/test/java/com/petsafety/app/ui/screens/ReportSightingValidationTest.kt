package com.petsafety.app.ui.screens

import com.petsafety.app.util.InputValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M7 — pre-fix the ReportSightingDialog and MarkAsMissingScreen submit
 * paths trusted whatever the geocoder returned and never validated the
 * reporter phone format. Symptom: malformed phone numbers reached the
 * backend (rejected as 400) and (0,0)/null-island sightings polluted
 * the alert map. Mirror the iOS ReportSightingView guards.
 *
 * These tests duplicate the inline pre-submit check logic so a
 * regression in the composable bodies fails here without needing
 * a Compose-UI test stack. Source-regression block at the bottom
 * grep-asserts the call sites still invoke the validators.
 */
class ReportSightingValidationTest {

    /** Mirror of the ReportSightingDialog phone-validation gate. */
    private fun isPhoneSubmittable(shareContact: Boolean, reporterPhone: String): Boolean {
        if (!shareContact) return true
        if (reporterPhone.isBlank()) return true
        return InputValidators.isValidPhone(reporterPhone)
    }

    /** Mirror of the post-geocode coordinate sanitisation in MarkAsMissingScreen. */
    private fun sanitizeCoordinate(lat: Double?, lng: Double?): Pair<Double, Double>? {
        if (lat == null || lng == null) return null
        return if (InputValidators.isValidCoordinate(lat, lng)) lat to lng else null
    }

    // --- phone validation -----------------------------------------------

    @Test
    fun `phone — passes when share-contact is off, regardless of value`() {
        assertTrue(isPhoneSubmittable(shareContact = false, reporterPhone = "garbage"))
        assertTrue(isPhoneSubmittable(shareContact = false, reporterPhone = ""))
    }

    @Test
    fun `phone — passes when share-contact is on but field is blank`() {
        // User opted to share name + email but not phone.
        assertTrue(isPhoneSubmittable(shareContact = true, reporterPhone = ""))
        assertTrue(isPhoneSubmittable(shareContact = true, reporterPhone = "   "))
    }

    @Test
    fun `phone — accepts well-formed international numbers`() {
        assertTrue(isPhoneSubmittable(shareContact = true, reporterPhone = "+36301234567"))
        assertTrue(isPhoneSubmittable(shareContact = true, reporterPhone = "+1 415 555 0100"))
    }

    @Test
    fun `phone — rejects free-text non-phone garbage`() {
        assertFalse(isPhoneSubmittable(shareContact = true, reporterPhone = "call me later"))
        assertFalse(isPhoneSubmittable(shareContact = true, reporterPhone = "abc"))
    }

    // --- coordinate sanitisation ----------------------------------------

    @Test
    fun `coords — null lat or lng returns null (no sighting placed)`() {
        assertNull(sanitizeCoordinate(null, 19.0))
        assertNull(sanitizeCoordinate(47.5, null))
        assertNull(sanitizeCoordinate(null, null))
    }

    @Test
    fun `coords — null island (0,0) is dropped`() {
        // Geocoder failure mode for unparseable input.
        assertNull(sanitizeCoordinate(0.0, 0.0))
    }

    @Test
    fun `coords — out-of-range values are dropped`() {
        assertNull(sanitizeCoordinate(91.0, 0.0))
        assertNull(sanitizeCoordinate(-91.0, 0.0))
        assertNull(sanitizeCoordinate(0.0, 181.0))
        assertNull(sanitizeCoordinate(0.0, -181.0))
        assertNull(sanitizeCoordinate(Double.NaN, 19.0))
    }

    @Test
    fun `coords — well-formed point is returned unchanged`() {
        val result = sanitizeCoordinate(47.5, 19.0)
        assertEquals(47.5 to 19.0, result)
    }

    @Test
    fun `coords — boundary values (90, 180) are accepted`() {
        assertEquals(90.0 to 180.0, sanitizeCoordinate(90.0, 180.0))
        assertEquals((-90.0) to (-180.0), sanitizeCoordinate(-90.0, -180.0))
    }

    // --- source regression ---------------------------------------------

    @Test
    fun `source — ReportSightingDialog wires both validators on submit`() {
        val src = readSource("AlertsScreens.kt")
        // Phone gate exists with the InputValidators check.
        assertTrue(
            "ReportSightingDialog should validate reporterPhone via InputValidators.isValidPhone",
            Regex("""shareContact\s*&&\s*reporterPhone\.isNotBlank\(\)\s*&&\s*\n?\s*!InputValidators\.isValidPhone\(reporterPhone\)""")
                .containsMatchIn(src)
        )
        // Coordinate gate exists for both the GPS path and the post-geocode path.
        assertTrue(
            "ReportSightingDialog should validate coords via InputValidators.isValidCoordinate",
            src.contains("InputValidators.isValidCoordinate")
        )
    }

    @Test
    fun `source — MarkAsMissingScreen sanitises geocoded coords`() {
        val src = readSource("MarkAsMissingScreen.kt")
        assertTrue(
            "MarkAsMissingScreen should validate geocoded coords before submit",
            src.contains("InputValidators.isValidCoordinate")
        )
    }

    private fun readSource(filename: String): String {
        val candidates = listOf(
            "app/src/main/java/com/petsafety/app/ui/screens/$filename",
            "src/main/java/com/petsafety/app/ui/screens/$filename",
        )
        var dir: java.io.File? = java.io.File(".").canonicalFile
        repeat(6) {
            val current = dir ?: return@repeat
            for (rel in candidates) {
                val f = java.io.File(current, rel)
                if (f.exists()) return f.readText()
            }
            dir = current.parentFile
        }
        error("Could not locate $filename from working dir ${java.io.File(".").canonicalPath}")
    }
}
