package com.petsafety.app.ui.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.petsafety.app.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Tests for PetLocalizer — verifies localizeSpecies, localizeBreed, and localizeSex
 * correctly resolve string resources or fall back to raw values.
 */
class PetLocalizerTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        resources = mockk(relaxed = true)
        every { context.resources } returns resources
        every { context.packageName } returns "com.petsafety.app"

        // Mock locale chain for BreedData
        val localeList = mockk<LocaleList>()
        val configuration = mockk<Configuration>()
        every { resources.configuration } returns configuration
        every { configuration.locales } returns localeList
        every { localeList[0] } returns Locale.ENGLISH

        // Species string resources
        every { context.getString(R.string.species_dog) } returns "Dog"
        every { context.getString(R.string.species_cat) } returns "Cat"
        every { context.getString(R.string.species_bird) } returns "Bird"
        every { context.getString(R.string.species_rabbit) } returns "Rabbit"
        every { context.getString(R.string.species_other) } returns "Other"
        every { context.getString(R.string.sex_unknown) } returns "Unknown"

        // By default, getIdentifier returns 0 (no resource found)
        every { resources.getIdentifier(any(), any(), any()) } returns 0
    }

    // ==================== localizeSpecies tests ====================

    @Test
    fun `localizeSpecies - dog - returns localized string`() {
        val result = PetLocalizer.localizeSpecies(context, "dog")
        assertEquals("Dog", result)
    }

    @Test
    fun `localizeSpecies - Dog uppercase - returns localized string`() {
        val result = PetLocalizer.localizeSpecies(context, "Dog")
        assertEquals("Dog", result)
    }

    @Test
    fun `localizeSpecies - DOG all caps - returns localized string`() {
        val result = PetLocalizer.localizeSpecies(context, "DOG")
        assertEquals("Dog", result)
    }

    @Test
    fun `localizeSpecies - cat - returns localized string`() {
        val result = PetLocalizer.localizeSpecies(context, "cat")
        assertEquals("Cat", result)
    }

    @Test
    fun `localizeSpecies - bird - returns localized string`() {
        val result = PetLocalizer.localizeSpecies(context, "bird")
        assertEquals("Bird", result)
    }

    @Test
    fun `localizeSpecies - rabbit - returns localized string`() {
        val result = PetLocalizer.localizeSpecies(context, "rabbit")
        assertEquals("Rabbit", result)
    }

    @Test
    fun `localizeSpecies - other - returns localized string`() {
        val result = PetLocalizer.localizeSpecies(context, "other")
        assertEquals("Other", result)
    }

    @Test
    fun `localizeSpecies - unknown species - returns capitalized raw value`() {
        val result = PetLocalizer.localizeSpecies(context, "hamster")
        assertEquals("Hamster", result)
    }

    @Test
    fun `localizeSpecies - unknown species lowercase - capitalizes first char`() {
        val result = PetLocalizer.localizeSpecies(context, "ferret")
        assertEquals("Ferret", result)
    }

    @Test
    fun `localizeSpecies - null - returns empty string`() {
        val result = PetLocalizer.localizeSpecies(context, null)
        assertEquals("", result)
    }

    @Test
    fun `localizeSpecies - empty string - returns empty string`() {
        val result = PetLocalizer.localizeSpecies(context, "")
        assertEquals("", result)
    }

    @Test
    fun `localizeSpecies - blank string - returns empty string`() {
        val result = PetLocalizer.localizeSpecies(context, "   ")
        assertEquals("", result)
    }

    // ==================== localizeBreed tests ====================

    @Test
    fun `localizeBreed - known breed with species prefix - returns localized string`() {
        val resId = 999
        every { resources.getIdentifier("breed_dog_golden_retriever", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Golden Retriever"

        val result = PetLocalizer.localizeBreed(context, "Golden Retriever", "Dog")
        assertEquals("Golden Retriever", result)
    }

    @Test
    fun `localizeBreed - known breed without species - tries plain key`() {
        val resId = 998
        every { resources.getIdentifier("breed_persian", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Persian"

        val result = PetLocalizer.localizeBreed(context, "Persian")
        assertEquals("Persian", result)
    }

    @Test
    fun `localizeBreed - unknown breed - falls back to raw value`() {
        // No resource found (default mock returns 0)
        val result = PetLocalizer.localizeBreed(context, "Custom Breed XYZ", "Dog")
        assertEquals("Custom Breed XYZ", result)
    }

    @Test
    fun `localizeBreed - alias dsh - resolves to European Shorthair`() {
        val result = PetLocalizer.localizeBreed(context, "dsh", "Cat")
        assertEquals("European Shorthair", result)
    }

    @Test
    fun `localizeBreed - alias dlh - resolves to European Shorthair`() {
        val result = PetLocalizer.localizeBreed(context, "dlh")
        assertEquals("European Shorthair", result)
    }

    @Test
    fun `localizeBreed - alias mixed - resolves to Mixed Crossbreed`() {
        val result = PetLocalizer.localizeBreed(context, "mixed")
        assertEquals("Mixed / Crossbreed", result)
    }

    @Test
    fun `localizeBreed - alias crossbreed - resolves to Mixed Crossbreed`() {
        val result = PetLocalizer.localizeBreed(context, "crossbreed")
        assertEquals("Mixed / Crossbreed", result)
    }

    @Test
    fun `localizeBreed - alias mixed breed - resolves to Mixed Crossbreed`() {
        val result = PetLocalizer.localizeBreed(context, "mixed breed")
        assertEquals("Mixed / Crossbreed", result)
    }

    @Test
    fun `localizeBreed - null - returns empty string`() {
        val result = PetLocalizer.localizeBreed(context, null, "Dog")
        assertEquals("", result)
    }

    @Test
    fun `localizeBreed - empty string - returns empty string`() {
        val result = PetLocalizer.localizeBreed(context, "", "Dog")
        assertEquals("", result)
    }

    @Test
    fun `localizeBreed - breed with slash - unknown breed falls back to raw value`() {
        val result = PetLocalizer.localizeBreed(context, "Lab/Poodle", "Dog")
        assertEquals("Lab/Poodle", result)
    }

    @Test
    fun `localizeBreed - species prefix tried first then plain key`() {
        // Species-prefixed key not found, but plain key found
        val resId = 993
        every { resources.getIdentifier("breed_dog_beagle", "string", "com.petsafety.app") } returns 0
        every { resources.getIdentifier("breed_beagle", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Beagle"

        val result = PetLocalizer.localizeBreed(context, "Beagle", "Dog")
        assertEquals("Beagle", result)
    }

    // ==================== localizeSex tests ====================

    @Test
    fun `localizeSex - dog male - returns species-specific term`() {
        val resId = 900
        every { resources.getIdentifier("sex_dog_male", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Male (Dog)"

        val result = PetLocalizer.localizeSex(context, "male", "Dog")
        assertEquals("Male (Dog)", result)
    }

    @Test
    fun `localizeSex - cat female - returns species-specific term`() {
        val resId = 901
        every { resources.getIdentifier("sex_cat_female", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Female (Cat)"

        val result = PetLocalizer.localizeSex(context, "female", "Cat")
        assertEquals("Female (Cat)", result)
    }

    @Test
    fun `localizeSex - dog male with uppercase input - normalizes case`() {
        val resId = 900
        every { resources.getIdentifier("sex_dog_male", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Male (Dog)"

        val result = PetLocalizer.localizeSex(context, "Male", "Dog")
        assertEquals("Male (Dog)", result)
    }

    @Test
    fun `localizeSex - generic fallback when species not dog or cat`() {
        val resId = 902
        every { resources.getIdentifier("sex_male", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Male"

        val result = PetLocalizer.localizeSex(context, "male", "Bird")
        assertEquals("Male", result)
    }

    @Test
    fun `localizeSex - generic fallback when species null`() {
        val resId = 902
        every { resources.getIdentifier("sex_male", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Male"

        val result = PetLocalizer.localizeSex(context, "male", null)
        assertEquals("Male", result)
    }

    @Test
    fun `localizeSex - unknown - returns localized unknown string`() {
        val result = PetLocalizer.localizeSex(context, "unknown", "Dog")
        assertEquals("Unknown", result)
    }

    @Test
    fun `localizeSex - Unknown mixed case - returns localized unknown string`() {
        val result = PetLocalizer.localizeSex(context, "Unknown", "Cat")
        assertEquals("Unknown", result)
    }

    @Test
    fun `localizeSex - no resource found - returns capitalized raw value`() {
        // No species-specific or generic resource found (default 0)
        val result = PetLocalizer.localizeSex(context, "neutered_male", "Dog")
        assertEquals("Neutered_male", result)
    }

    @Test
    fun `localizeSex - null - returns empty string`() {
        val result = PetLocalizer.localizeSex(context, null, "Dog")
        assertEquals("", result)
    }

    @Test
    fun `localizeSex - empty string - returns empty string`() {
        val result = PetLocalizer.localizeSex(context, "", "Dog")
        assertEquals("", result)
    }

    @Test
    fun `localizeSex - blank string - returns empty string`() {
        val result = PetLocalizer.localizeSex(context, "   ", "Cat")
        assertEquals("", result)
    }

    @Test
    fun `localizeSex - species-specific not found falls back to generic`() {
        // Dog-specific key returns 0, generic key returns a resource
        every { resources.getIdentifier("sex_dog_female", "string", "com.petsafety.app") } returns 0
        val resId = 903
        every { resources.getIdentifier("sex_female", "string", "com.petsafety.app") } returns resId
        every { context.getString(resId) } returns "Female"

        val result = PetLocalizer.localizeSex(context, "female", "Dog")
        assertEquals("Female", result)
    }
}
