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
}
