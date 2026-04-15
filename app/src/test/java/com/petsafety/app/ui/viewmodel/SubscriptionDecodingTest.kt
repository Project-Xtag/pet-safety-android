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
}
