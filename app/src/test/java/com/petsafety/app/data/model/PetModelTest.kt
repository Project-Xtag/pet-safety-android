package com.petsafety.app.data.model

import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the Pet data model — localizedAge, age fallback, profileImage,
 * and JSON deserialization edge cases.
 */
class PetModelTest {

    private lateinit var resources: Resources
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Before
    fun setup() {
        resources = mockk(relaxed = true)

        // Mock plurals: getQuantityString(resId, count, count)
        every { resources.getQuantityString(any(), eq(1), eq(1)) } answers {
            val resId = firstArg<Int>()
            if (resId == com.petsafety.app.R.plurals.age_years) "1 year"
            else "1 month"
        }
        every { resources.getQuantityString(any(), eq(2), eq(2)) } answers {
            val resId = firstArg<Int>()
            if (resId == com.petsafety.app.R.plurals.age_years) "2 years"
            else "2 months"
        }
        every { resources.getQuantityString(any(), eq(3), eq(3)) } answers {
            val resId = firstArg<Int>()
            if (resId == com.petsafety.app.R.plurals.age_years) "3 years"
            else "3 months"
        }
        every { resources.getQuantityString(any(), eq(5), eq(5)) } answers {
            val resId = firstArg<Int>()
            if (resId == com.petsafety.app.R.plurals.age_years) "5 years"
            else "5 months"
        }
        every { resources.getQuantityString(any(), eq(6), eq(6)) } answers {
            val resId = firstArg<Int>()
            if (resId == com.petsafety.app.R.plurals.age_years) "6 years"
            else "6 months"
        }
        every { resources.getQuantityString(any(), eq(10), eq(10)) } answers {
            val resId = firstArg<Int>()
            if (resId == com.petsafety.app.R.plurals.age_years) "10 years"
            else "10 months"
        }
        every { resources.getQuantityString(any(), eq(11), eq(11)) } answers {
            val resId = firstArg<Int>()
            if (resId == com.petsafety.app.R.plurals.age_years) "11 years"
            else "11 months"
        }
    }

    private fun makePet(
        ageYears: Int? = null,
        ageMonths: Int? = null,
        ageIsApproximate: Boolean? = null
    ): Pet = Pet(
        id = "pet-1",
        name = "Test",
        ageYears = ageYears,
        ageMonths = ageMonths,
        ageIsApproximate = ageIsApproximate
    )

    // ==================== localizedAge tests ====================

    @Test
    fun `localizedAge - years only - returns years string`() {
        val pet = makePet(ageYears = 3)
        assertEquals("3 years", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - 1 year singular - returns singular form`() {
        val pet = makePet(ageYears = 1)
        assertEquals("1 year", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - months only - returns months string`() {
        val pet = makePet(ageMonths = 6)
        assertEquals("6 months", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - 1 month singular - returns singular form`() {
        val pet = makePet(ageMonths = 1)
        assertEquals("1 month", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - years and months - returns combined string`() {
        val pet = makePet(ageYears = 3, ageMonths = 6)
        assertEquals("3 years 6 months", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - approximate flag - prepends tilde`() {
        val pet = makePet(ageYears = 2, ageIsApproximate = true)
        assertEquals("~2 years", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - approximate with years and months - prepends tilde`() {
        val pet = makePet(ageYears = 5, ageMonths = 3, ageIsApproximate = true)
        assertEquals("~5 years 3 months", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - approximate months only - prepends tilde`() {
        val pet = makePet(ageMonths = 6, ageIsApproximate = true)
        assertEquals("~6 months", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - no age data - returns null`() {
        val pet = makePet()
        assertNull(pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - zero years zero months - returns null`() {
        val pet = makePet(ageYears = 0, ageMonths = 0)
        assertNull(pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - years with zero months - returns years only`() {
        val pet = makePet(ageYears = 10, ageMonths = 0)
        assertEquals("10 years", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - approximate false - no tilde prefix`() {
        val pet = makePet(ageYears = 2, ageIsApproximate = false)
        assertEquals("2 years", pet.localizedAge(resources))
    }

    @Test
    fun `localizedAge - approximate null - no tilde prefix`() {
        val pet = makePet(ageYears = 2, ageIsApproximate = null)
        assertEquals("2 years", pet.localizedAge(resources))
    }

    // ==================== age (fallback) tests ====================

    @Test
    fun `age - years only - returns Ny format`() {
        val pet = makePet(ageYears = 3)
        assertEquals("3y", pet.age)
    }

    @Test
    fun `age - months only - returns Nm format`() {
        val pet = makePet(ageMonths = 6)
        assertEquals("6m", pet.age)
    }

    @Test
    fun `age - years and months - returns Ny Nm format`() {
        val pet = makePet(ageYears = 3, ageMonths = 6)
        assertEquals("3y 6m", pet.age)
    }

    @Test
    fun `age - approximate - prepends tilde`() {
        val pet = makePet(ageYears = 2, ageIsApproximate = true)
        assertEquals("~2y", pet.age)
    }

    @Test
    fun `age - approximate years and months - prepends tilde`() {
        val pet = makePet(ageYears = 1, ageMonths = 5, ageIsApproximate = true)
        assertEquals("~1y 5m", pet.age)
    }

    @Test
    fun `age - no age data - returns null`() {
        val pet = makePet()
        assertNull(pet.age)
    }

    @Test
    fun `age - zero values - returns null`() {
        val pet = makePet(ageYears = 0, ageMonths = 0)
        assertNull(pet.age)
    }

    // ==================== profileImage tests ====================

    @Test
    fun `profileImage - profile_image set - returns profile_image`() {
        val jsonStr = """
            {"id":"p1","name":"Buddy","profile_image":"https://example.com/profile.jpg"}
        """.trimIndent()
        val pet = json.decodeFromString<Pet>(jsonStr)
        assertEquals("https://example.com/profile.jpg", pet.profileImage)
    }

    @Test
    fun `profileImage - photo_url set - returns photo_url`() {
        val jsonStr = """
            {"id":"p1","name":"Buddy","photo_url":"https://example.com/photo.jpg"}
        """.trimIndent()
        val pet = json.decodeFromString<Pet>(jsonStr)
        assertEquals("https://example.com/photo.jpg", pet.profileImage)
    }

    @Test
    fun `profileImage - both set - prefers profile_image`() {
        val jsonStr = """
            {"id":"p1","name":"Buddy","profile_image":"https://example.com/profile.jpg","photo_url":"https://example.com/photo.jpg"}
        """.trimIndent()
        val pet = json.decodeFromString<Pet>(jsonStr)
        assertEquals("https://example.com/profile.jpg", pet.profileImage)
    }

    @Test
    fun `profileImage - neither set - returns null`() {
        val pet = Pet(id = "p1", name = "Buddy")
        assertNull(pet.profileImage)
    }

    // ==================== JSON deserialization tests ====================

    @Test
    fun `deserialization - full pet JSON - all fields populated`() {
        val jsonStr = """
            {
                "id": "pet-123",
                "owner_id": "user-456",
                "name": "Fluffy",
                "species": "Cat",
                "breed": "Persian",
                "color": "White",
                "weight": 4.5,
                "microchip_number": "1234567890",
                "is_missing": true,
                "sex": "female",
                "is_neutered": true,
                "age_years": 3,
                "age_months": 6,
                "age_is_approximate": true,
                "qr_code": "QR-ABC",
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-06-01T00:00:00Z"
            }
        """.trimIndent()

        val pet = json.decodeFromString<Pet>(jsonStr)

        assertEquals("pet-123", pet.id)
        assertEquals("user-456", pet.ownerId)
        assertEquals("Fluffy", pet.name)
        assertEquals("Cat", pet.species)
        assertEquals("Persian", pet.breed)
        assertEquals("White", pet.color)
        assertEquals(4.5, pet.weight)
        assertEquals("1234567890", pet.microchipNumber)
        assertTrue(pet.isMissing)
        assertEquals("female", pet.sex)
        assertEquals(true, pet.isNeutered)
        assertEquals(3, pet.ageYears)
        assertEquals(6, pet.ageMonths)
        assertEquals(true, pet.ageIsApproximate)
        assertEquals("QR-ABC", pet.qrCode)
    }

    @Test
    fun `deserialization - minimal pet JSON - defaults applied`() {
        val jsonStr = """{"id":"p1","name":"Rex"}"""
        val pet = json.decodeFromString<Pet>(jsonStr)

        assertEquals("p1", pet.id)
        assertEquals("Rex", pet.name)
        assertEquals("", pet.species)
        assertNull(pet.breed)
        assertNull(pet.color)
        assertNull(pet.weight)
        assertEquals(false, pet.isMissing)
        assertNull(pet.ageYears)
        assertNull(pet.ageMonths)
        assertNull(pet.profileImage)
    }

    @Test
    fun `deserialization - unknown keys ignored`() {
        val jsonStr = """{"id":"p1","name":"Rex","unknown_field":"value","extra":123}"""
        val pet = json.decodeFromString<Pet>(jsonStr)
        assertEquals("p1", pet.id)
        assertEquals("Rex", pet.name)
    }
}
