package com.petsafety.app

import com.petsafety.app.data.model.TagScannedEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class SseEventTests {
    @Test
    fun decodesTagScannedEvent() {
        val json = """
        {
          "petId": "pet_123",
          "petName": "Max",
          "qrCode": "QR_123",
          "location": { "lat": 51.5074, "lng": -0.1278 },
          "address": "London, UK",
          "scannedAt": "2026-01-12T10:00:00Z"
        }
        """.trimIndent()

        val parser = Json { ignoreUnknownKeys = true }
        val event = parser.decodeFromString(TagScannedEvent.serializer(), json)
        assertEquals("pet_123", event.petId)
        assertEquals("Max", event.petName)
        assertEquals("QR_123", event.qrCode)
        assertEquals(51.5074, event.location.lat, 0.0001)
    }
}
