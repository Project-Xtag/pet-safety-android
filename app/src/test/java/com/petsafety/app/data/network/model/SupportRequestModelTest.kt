package com.petsafety.app.data.network.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportRequestModelTest {

    private val json = Json { ignoreUnknownKeys = true }

    // MARK: - SupportRequest Tests

    @Test
    fun `SupportRequest serializes correctly`() {
        // Given
        val request = SupportRequest(
            category = "Technical Issue",
            subject = "App crashes on startup",
            message = "The app crashes every time I open it."
        )

        // When
        val jsonString = json.encodeToString(request)

        // Then
        assertTrue(jsonString.contains("\"category\":\"Technical Issue\""))
        assertTrue(jsonString.contains("\"subject\":\"App crashes on startup\""))
        assertTrue(jsonString.contains("\"message\":\"The app crashes every time I open it.\""))
    }

    @Test
    fun `SupportRequest deserializes correctly`() {
        // Given
        val jsonString = """
            {
                "category": "Billing",
                "subject": "Subscription issue",
                "message": "I was charged twice."
            }
        """.trimIndent()

        // When
        val request = json.decodeFromString<SupportRequest>(jsonString)

        // Then
        assertEquals("Billing", request.category)
        assertEquals("Subscription issue", request.subject)
        assertEquals("I was charged twice.", request.message)
    }

    // MARK: - SupportRequestResponse Tests

    @Test
    fun `SupportRequestResponse deserializes correctly`() {
        // Given
        val jsonString = """
            {
                "ticketId": "SUP-ABC123-XYZ9",
                "message": "Support request submitted successfully. We will get back to you within 24 hours."
            }
        """.trimIndent()

        // When
        val response = json.decodeFromString<SupportRequestResponse>(jsonString)

        // Then
        assertEquals("SUP-ABC123-XYZ9", response.ticketId)
        assertTrue(response.message.contains("24 hours"))
    }

    @Test
    fun `SupportRequestResponse serializes correctly`() {
        // Given
        val response = SupportRequestResponse(
            ticketId = "SUP-TEST-1234",
            message = "Request submitted successfully."
        )

        // When
        val jsonString = json.encodeToString(response)

        // Then
        assertTrue(jsonString.contains("\"ticketId\":\"SUP-TEST-1234\""))
        assertTrue(jsonString.contains("\"message\":\"Request submitted successfully.\""))
    }

    // MARK: - API Envelope Tests

    @Test
    fun `ApiEnvelope with SupportRequestResponse deserializes correctly`() {
        // Given
        val jsonString = """
            {
                "success": true,
                "data": {
                    "ticketId": "SUP-TEST-9999",
                    "message": "Success!"
                },
                "error": null
            }
        """.trimIndent()

        // When
        val envelope = json.decodeFromString<ApiEnvelope<SupportRequestResponse>>(jsonString)

        // Then
        assertTrue(envelope.success)
        assertNotNull(envelope.data)
        assertEquals("SUP-TEST-9999", envelope.data?.ticketId)
        assertEquals(null, envelope.error)
    }

    @Test
    fun `ApiEnvelope error response deserializes correctly`() {
        // Given
        val jsonString = """
            {
                "success": false,
                "data": null,
                "error": "Subject and message are required"
            }
        """.trimIndent()

        // When
        val envelope = json.decodeFromString<ApiEnvelope<SupportRequestResponse>>(jsonString)

        // Then
        assertTrue(!envelope.success)
        assertEquals(null, envelope.data)
        assertEquals("Subject and message are required", envelope.error)
    }

    // MARK: - Validation Tests

    @Test
    fun `SupportRequest handles special characters`() {
        // Given
        val request = SupportRequest(
            category = "General",
            subject = "Help! 🐕 My pet's tag isn't working",
            message = "Unicode: café, naïve, 日本語\nNewlines work\tTabs too"
        )

        // When
        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<SupportRequest>(jsonString)

        // Then
        assertEquals(request.subject, decoded.subject)
        assertEquals(request.message, decoded.message)
    }

    @Test
    fun `SupportRequest handles empty strings`() {
        // Given
        val request = SupportRequest(
            category = "",
            subject = "",
            message = ""
        )

        // When
        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<SupportRequest>(jsonString)

        // Then
        assertEquals("", decoded.category)
        assertEquals("", decoded.subject)
        assertEquals("", decoded.message)
    }

    @Test
    fun `SupportRequest handles long messages`() {
        // Given
        val longMessage = "A".repeat(5000)
        val request = SupportRequest(
            category = "General",
            subject = "Long message test",
            message = longMessage
        )

        // When
        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<SupportRequest>(jsonString)

        // Then
        assertEquals(5000, decoded.message.length)
    }

    // MARK: - Ticket ID Format Tests

    @Test
    fun `Ticket ID matches expected format`() {
        // Given
        val validTicketIds = listOf(
            "SUP-ABC123-XYZ9",
            "SUP-LQWERT-ABCD",
            "SUP-123ABC-9999"
        )

        val ticketIdPattern = Regex("^SUP-[A-Z0-9]+-[A-Z0-9]+$")

        // Then
        validTicketIds.forEach { ticketId ->
            assertTrue(
                "Ticket ID $ticketId should match pattern",
                ticketIdPattern.matches(ticketId)
            )
        }
    }

    // MARK: - Category Tests

    @Test
    fun `All valid categories are accepted`() {
        val validCategories = listOf(
            "General",
            "Technical Issue",
            "Account",
            "Billing",
            "Feature Request",
            "Other"
        )

        validCategories.forEach { category ->
            val request = SupportRequest(
                category = category,
                subject = "Test",
                message = "Test message"
            )

            val jsonString = json.encodeToString(request)
            val decoded = json.decodeFromString<SupportRequest>(jsonString)

            assertEquals(category, decoded.category)
        }
    }
}
