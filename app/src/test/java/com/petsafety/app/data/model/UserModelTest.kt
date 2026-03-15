package com.petsafety.app.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the User data model — serialization, fullName computed property,
 * and locale-aware name ordering.
 */
class UserModelTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    // MARK: - Deserialization

    @Test
    fun `deserializes full user from JSON`() {
        val jsonStr = """
            {
                "id": "user-1",
                "email": "test@example.com",
                "first_name": "Viktor",
                "last_name": "Szász",
                "phone": "+36301234567",
                "country": "HU",
                "preferred_language": "hu",
                "address": "Kossuth u. 10",
                "city": "Budapest",
                "postal_code": "1055",
                "role": "user",
                "created_at": "2026-01-01T00:00:00Z",
                "is_missing": false
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonStr)

        assertEquals("user-1", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("Viktor", user.firstName)
        assertEquals("Szász", user.lastName)
        assertEquals("+36301234567", user.phone)
        assertEquals("HU", user.country)
        assertEquals("hu", user.preferredLanguage)
        assertEquals("Kossuth u. 10", user.address)
        assertEquals("Budapest", user.city)
        assertEquals("1055", user.postalCode)
    }

    @Test
    fun `deserializes minimal user with defaults`() {
        val jsonStr = """
            {
                "id": "user-2",
                "email": "min@example.com"
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonStr)

        assertEquals("user-2", user.id)
        assertEquals("min@example.com", user.email)
        assertNull(user.firstName)
        assertNull(user.lastName)
        assertNull(user.phone)
        assertNull(user.country)
    }

    @Test
    fun `deserializes optional secondary contact fields`() {
        val jsonStr = """
            {
                "id": "user-3",
                "email": "test@example.com",
                "secondary_email": "backup@example.com",
                "secondary_phone": "+36309876543"
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonStr)

        assertEquals("backup@example.com", user.secondaryEmail)
        assertEquals("+36309876543", user.secondaryPhone)
    }

    @Test
    fun `deserializes is_gift field on Order in user context`() {
        // User model doesn't have is_gift but Order does — sanity check
        val orderJson = """
            {
                "id": "ord-1",
                "pet_name": "Gift",
                "total_amount": 0.0,
                "shipping_cost": 3.90,
                "payment_method": "card",
                "payment_status": "completed",
                "order_status": "processing",
                "is_gift": true,
                "created_at": "2026-03-15T00:00:00Z",
                "updated_at": "2026-03-15T00:00:00Z"
            }
        """.trimIndent()

        val order = json.decodeFromString<Order>(orderJson)
        assertTrue(order.isGift)
    }

    // MARK: - fullName Computed Property

    @Test
    fun `fullName with both first and last name`() {
        val user = createTestUser(firstName = "John", lastName = "Doe")
        // Default system locale (likely "en" in test) → "First Last"
        val name = user.fullName
        assertTrue(
            "Expected 'John Doe' or 'Doe John' but got '$name'",
            name == "John Doe" || name == "Doe John"
        )
    }

    @Test
    fun `fullName with first name only`() {
        val user = createTestUser(firstName = "John", lastName = null)
        assertEquals("John", user.fullName)
    }

    @Test
    fun `fullName with last name only`() {
        val user = createTestUser(firstName = null, lastName = "Doe")
        assertEquals("Doe", user.fullName)
    }

    @Test
    fun `fullName with both names null`() {
        val user = createTestUser(firstName = null, lastName = null)
        assertEquals("", user.fullName)
    }

    @Test
    fun `fullName with empty strings`() {
        val user = createTestUser(firstName = "", lastName = "")
        assertEquals("", user.fullName)
    }

    // MARK: - Privacy Settings

    @Test
    fun `privacy settings default to null when not provided`() {
        val jsonStr = """
            {
                "id": "user-4",
                "email": "privacy@example.com"
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonStr)

        assertNull(user.showPhonePublicly)
        assertNull(user.showEmailPublicly)
        assertNull(user.showAddressPublicly)
        assertNull(user.showSecondaryPhonePublicly)
        assertNull(user.showSecondaryEmailPublicly)
    }

    @Test
    fun `privacy settings deserialized correctly`() {
        val jsonStr = """
            {
                "id": "user-5",
                "email": "privacy@example.com",
                "show_phone_publicly": false,
                "show_email_publicly": true,
                "show_address_publicly": false,
                "show_secondary_phone_publicly": false,
                "show_secondary_email_publicly": true
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonStr)

        assertEquals(false, user.showPhonePublicly)
        assertEquals(true, user.showEmailPublicly)
        assertEquals(false, user.showAddressPublicly)
        assertEquals(false, user.showSecondaryPhonePublicly)
        assertEquals(true, user.showSecondaryEmailPublicly)
    }

    // MARK: - Helper

    private fun createTestUser(
        firstName: String? = null,
        lastName: String? = null
    ): User {
        val jsonStr = buildString {
            append("{\"id\": \"test\", \"email\": \"test@example.com\"")
            if (firstName != null) append(", \"first_name\": \"$firstName\"")
            if (lastName != null) append(", \"last_name\": \"$lastName\"")
            append("}")
        }
        return json.decodeFromString<User>(jsonStr)
    }
}
