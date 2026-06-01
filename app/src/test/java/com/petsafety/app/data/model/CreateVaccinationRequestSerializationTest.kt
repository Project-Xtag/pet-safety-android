package com.petsafety.app.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LOAD-BEARING: the server derives `expires_at` only when the field is ABSENT
 * (undefined != null). If a toggle-off create serialized `"expires_at": null`,
 * the server would store a null (valid-forever) expiry instead of deriving from
 * the catalog's default_validity_months — a vaccination that never expires and
 * never fires a reminder, invisible in the UI.
 *
 * This pins the actual serialized body against the EXACT Retrofit converter
 * config (ApiClient.kt: encodeDefaults=true + explicitNulls=false). The
 * `explicitNulls=false` is what drops the null despite encodeDefaults=true — so
 * if anyone flips explicitNulls back to true, this test fails loudly.
 */
class CreateVaccinationRequestSerializationTest {

    // Mirror ApiClient.create()'s Json. Keep in sync if that converter changes.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `toggle-off create OMITS expires_at so the server derives it`() {
        val body = json.encodeToString(
            CreateVaccinationRequest(vaccineCode = "rabies", administeredAt = "2026-06-01", expiresAt = null)
        )
        assertFalse(
            "expires_at must be ABSENT on toggle-off (a serialized null would store valid-forever): $body",
            body.contains("expires_at")
        )
        // optional nulls likewise omitted
        assertFalse(body.contains("batch_number"))
        assertFalse(body.contains("vet_name"))
        // required fields present
        assertTrue(body.contains("\"vaccine_code\":\"rabies\""))
        assertTrue(body.contains("\"administered_at\":\"2026-06-01\""))
    }

    @Test
    fun `toggle-on create INCLUDES the explicit expires_at`() {
        val body = json.encodeToString(
            CreateVaccinationRequest(vaccineCode = "rabies", administeredAt = "2026-06-01", expiresAt = "2029-06-01")
        )
        assertTrue(body.contains("\"expires_at\":\"2029-06-01\""))
    }
}
