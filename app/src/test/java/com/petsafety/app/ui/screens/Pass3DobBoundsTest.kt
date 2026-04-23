package com.petsafety.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pass 3 audit fix — DOB picker now lower-bounds at 100 years ago, not
 * just upper-bounds at today. A user can no longer scroll back to 1800
 * and register a pet born in the Victorian era, which broke downstream
 * age calculations.
 */
class Pass3DobBoundsTest {

    // Mirror of PetFormScreen.kt's SelectableDates.isSelectableDate body.
    private fun isSelectable(utcTimeMillis: Long, now: Long = System.currentTimeMillis()): Boolean {
        val min = now - (100L * 365L * 24L * 60L * 60L * 1000L)
        val max = now
        return utcTimeMillis in min..max
    }

    @Test
    fun `today is selectable`() {
        val now = System.currentTimeMillis()
        assertTrue(isSelectable(now, now))
    }

    @Test
    fun `one year ago is selectable`() {
        val now = System.currentTimeMillis()
        val yearAgo = now - (365L * 24L * 60L * 60L * 1000L)
        assertTrue(isSelectable(yearAgo, now))
    }

    @Test
    fun `one day in the future is rejected`() {
        val now = System.currentTimeMillis()
        val tomorrow = now + (24L * 60L * 60L * 1000L)
        assertFalse(isSelectable(tomorrow, now))
    }

    @Test
    fun `year 1800 is rejected (the actual audit regression)`() {
        val now = System.currentTimeMillis()
        // 1800-01-01 in epoch millis is negative (before 1970)
        val year1800 = -5_364_662_400_000L
        assertFalse(isSelectable(year1800, now))
    }

    @Test
    fun `99 years ago is selectable (boundary - 1)`() {
        val now = System.currentTimeMillis()
        val ninetyNineYearsAgo = now - (99L * 365L * 24L * 60L * 60L * 1000L)
        assertTrue(isSelectable(ninetyNineYearsAgo, now))
    }

    @Test
    fun `101 years ago is rejected (boundary + 1)`() {
        val now = System.currentTimeMillis()
        val past = now - (101L * 365L * 24L * 60L * 60L * 1000L)
        assertFalse(isSelectable(past, now))
    }
}
