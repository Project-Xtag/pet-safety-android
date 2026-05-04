package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.model.SubscriptionStatus
import com.petsafety.app.data.model.UserSubscription
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SubscriptionDecodingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `UserSubscription deserializes trialEndsAt`() {
        val jsonStr = """
        {
            "id": "sub-1",
            "user_id": "user-1",
            "plan_name": "standard",
            "status": "trialing",
            "trial_ends_at": "2026-06-18T00:00:00.000Z"
        }
        """.trimIndent()

        val sub = json.decodeFromString<UserSubscription>(jsonStr)
        assertEquals("2026-06-18T00:00:00.000Z", sub.trialEndsAt)
        assertEquals(SubscriptionStatus.TRIALING, sub.status)
        assertTrue(sub.isTrialing)
        assertTrue(sub.isActive)
    }

    @Test
    fun `isTrialing returns false for active status`() {
        val jsonStr = """
        {
            "id": "sub-1",
            "user_id": "user-1",
            "plan_name": "standard",
            "status": "active"
        }
        """.trimIndent()

        val sub = json.decodeFromString<UserSubscription>(jsonStr)
        assertFalse(sub.isTrialing)
        assertTrue(sub.isActive)
    }

    // M17 — trial formatting helpers (parity with iOS Subscription.swift).

    @Test
    fun `trialEndsAtInstant parses standard ISO-8601 with millis and Z`() {
        val sub = subscriptionWithTrialEnd("2026-06-18T00:00:00.000Z")
        assertNotNull(sub.trialEndsAtInstant)
        assertEquals(java.time.Instant.parse("2026-06-18T00:00:00.000Z"), sub.trialEndsAtInstant)
    }

    @Test
    fun `trialEndsAtInstant tolerates backend variant without trailing Z`() {
        // Backend occasionally emits "2026-06-18T00:00:00" (no Z). The
        // parse helper retries with Z appended so the field renders.
        val sub = subscriptionWithTrialEnd("2026-06-18T00:00:00")
        assertNotNull(sub.trialEndsAtInstant)
    }

    @Test
    fun `trialEndsAtInstant returns null for absent trial date`() {
        val sub = subscriptionWithTrialEnd(null)
        assertNull(sub.trialEndsAtInstant)
        assertNull(sub.trialEndFormatted)
        assertNull(sub.trialDaysLeft)
    }

    @Test
    fun `trialEndFormatted returns a non-empty locale-aware string`() {
        val sub = subscriptionWithTrialEnd("2026-06-18T00:00:00.000Z")
        val formatted = sub.trialEndFormatted
        assertNotNull(formatted)
        assertTrue("formatted should not equal raw ISO", formatted != "2026-06-18T00:00:00.000Z")
        assertTrue("expected year in formatted output", formatted!!.contains("2026"))
    }

    @Test
    fun `trialDaysLeft is positive for a future date`() {
        val future = java.time.Instant.now().plus(5, java.time.temporal.ChronoUnit.DAYS)
        val sub = subscriptionWithTrialEnd(future.toString())
        val days = sub.trialDaysLeft
        assertNotNull(days)
        // Allow off-by-one for clock drift across the test's tick.
        assertTrue("expected ~5 days, got $days", days in 4..5)
    }

    @Test
    fun `trialDaysLeft is negative for an expired trial`() {
        val past = java.time.Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS)
        val sub = subscriptionWithTrialEnd(past.toString())
        val days = sub.trialDaysLeft
        assertNotNull(days)
        assertTrue("expected negative, got $days", days!! < 0)
    }

    private fun subscriptionWithTrialEnd(trialEndsAt: String?): UserSubscription {
        val isoField = trialEndsAt?.let { """"trial_ends_at": "$it",""" } ?: ""
        val jsonStr = """
        {
            "id": "sub-1",
            "user_id": "user-1",
            "plan_name": "standard",
            "status": "trialing",
            $isoField
            "billing_period": "monthly"
        }
        """.trimIndent()
        return json.decodeFromString(jsonStr)
    }
}
