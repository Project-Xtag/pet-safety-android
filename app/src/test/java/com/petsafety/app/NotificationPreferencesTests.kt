package com.petsafety.app

import com.petsafety.app.data.model.NotificationPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferencesTests {
    @Test
    fun defaultPreferencesValid() {
        val prefs = NotificationPreferences.default
        assertTrue(prefs.isValid)
        assertEquals(3, prefs.enabledCount)
    }

    @Test
    fun invalidWhenAllDisabled() {
        val prefs = NotificationPreferences(false, false, false)
        assertFalse(prefs.isValid)
        assertEquals(0, prefs.enabledCount)
    }

    @Test
    fun enabledCountMatches() {
        val prefs = NotificationPreferences(true, false, true)
        assertTrue(prefs.isValid)
        assertEquals(2, prefs.enabledCount)
    }

    @Test
    fun `missingPetAlerts defaults to true`() {
        val prefs = NotificationPreferences.default
        assertTrue(prefs.missingPetAlerts)
    }

    @Test
    fun `missingPetAlerts can be set to false`() {
        val prefs = NotificationPreferences(true, true, true, missingPetAlerts = false)
        assertFalse(prefs.missingPetAlerts)
    }

    @Test
    fun `missingPetAlerts does not affect isValid or enabledCount`() {
        val prefs = NotificationPreferences(true, false, false, missingPetAlerts = false)
        assertTrue(prefs.isValid)
        assertEquals(1, prefs.enabledCount)
    }

    @Test
    fun `missingPetAlerts defaults to true with positional args`() {
        val prefs = NotificationPreferences(true, true, true)
        assertTrue(prefs.missingPetAlerts)
    }
}
