package com.petsafety.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.petsafety.app.TestApplication::class)
class ShareCardGeneratorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ==================== Bitmap Dimensions ====================

    @Test
    fun `generate - produces 1080x1080 bitmap`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "Buddy",
            petImageUrl = null,
            petSpecies = "Dog"
        )

        assertEquals(1080, bitmap.width)
        assertEquals(1080, bitmap.height)
    }

    @Test
    fun `generate - produces ARGB_8888 bitmap`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "Buddy",
            petImageUrl = null,
            petSpecies = "Dog"
        )

        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    // ==================== Content Verification ====================
    // Note: Pixel-based tests (non-blank, teal background) are skipped because
    // Robolectric's Canvas implementation doesn't render real pixels.

    // ==================== Edge Cases ====================

    @Test
    fun `generate - handles empty pet name`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "",
            petImageUrl = null,
            petSpecies = "Dog"
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
    }

    @Test
    fun `generate - handles long pet name`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "Sir Barksalot The Third Of Canterbury",
            petImageUrl = null,
            petSpecies = "Dog"
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
    }

    @Test
    fun `generate - handles special characters in name`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "Müller's Kätze",
            petImageUrl = null,
            petSpecies = "Cat"
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
    }

    @Test
    fun `generate - handles null pet image URL`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "Buddy",
            petImageUrl = null,
            petSpecies = "Dog"
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
    }

    @Test
    fun `generate - handles empty pet image URL`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "Buddy",
            petImageUrl = "",
            petSpecies = "Dog"
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
    }

    @Test
    fun `generate - handles blank pet image URL`() = runTest {
        val bitmap = ShareCardGenerator.generate(
            context = context,
            petName = "Buddy",
            petImageUrl = "   ",
            petSpecies = "Dog"
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
    }

    // ==================== Consistency ====================

    @Test
    fun `generate - produces consistent dimensions across calls`() = runTest {
        val bitmap1 = ShareCardGenerator.generate(context, "A", null, "Dog")
        val bitmap2 = ShareCardGenerator.generate(context, "B", null, "Cat")
        val bitmap3 = ShareCardGenerator.generate(context, "C", null, "Dog")

        assertEquals(bitmap1.width, bitmap2.width)
        assertEquals(bitmap2.width, bitmap3.width)
        assertEquals(bitmap1.height, bitmap2.height)
    }
}
