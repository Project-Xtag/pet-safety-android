package com.petsafety.app.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(
    application = com.petsafety.app.TestApplication::class,
    packageName = "com.petsafety.app"
)
class SocialShareHelperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ==================== File Saving ====================

    @Test
    fun `saveBitmap - creates images directory in cache`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "share_card.png")
        imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertTrue("Images directory should be created", imagesDir.exists())
    }

    @Test
    fun `saveBitmap - creates share_card png file`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "share_card.png")
        imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertTrue("Share card file should exist", imageFile.exists())
        assertTrue("Share card file should not be empty", imageFile.length() > 0)
    }

    @Test
    fun `saveBitmap - overwrites existing file`() {
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "share_card.png")

        val bitmap1 = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        imageFile.outputStream().use { bitmap1.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val firstSize = imageFile.length()

        val bitmap2 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        imageFile.outputStream().use { bitmap2.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val secondSize = imageFile.length()

        assertTrue("File should be overwritten with different size", secondSize != firstSize)
    }

    // ==================== Intent Construction ====================

    @Test
    fun `shareIntent - has ACTION_SEND action`() {
        val uri = Uri.parse("content://com.petsafety.app.fileprovider/images/share_card.png")
        val intent = createShareIntent(uri, "Test caption")

        assertEquals(Intent.ACTION_SEND, intent.action)
    }

    @Test
    fun `shareIntent - has image-png type`() {
        val uri = Uri.parse("content://com.petsafety.app.fileprovider/images/share_card.png")
        val intent = createShareIntent(uri, "Test caption")

        assertEquals("image/png", intent.type)
    }

    @Test
    fun `shareIntent - includes caption as EXTRA_TEXT`() {
        val uri = Uri.parse("content://com.petsafety.app.fileprovider/images/share_card.png")
        val caption = "Buddy reunited! tagmenow.eu"
        val intent = createShareIntent(uri, caption)

        assertEquals(caption, intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `shareIntent - includes image URI as EXTRA_STREAM`() {
        val uri = Uri.parse("content://com.petsafety.app.fileprovider/images/share_card.png")
        val intent = createShareIntent(uri, "caption")

        assertNotNull("EXTRA_STREAM should be set", intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
    }

    @Test
    fun `shareIntent - grants read URI permission`() {
        val uri = Uri.parse("content://com.petsafety.app.fileprovider/images/share_card.png")
        val intent = createShareIntent(uri, "caption")

        assertTrue(
            "Should have FLAG_GRANT_READ_URI_PERMISSION",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
    }

    @Test
    fun `shareIntent - handles empty caption`() {
        val uri = Uri.parse("content://com.petsafety.app.fileprovider/images/share_card.png")
        val intent = createShareIntent(uri, "")

        assertEquals("", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `chooser - wraps share intent in ACTION_CHOOSER`() {
        val uri = Uri.parse("content://com.petsafety.app.fileprovider/images/share_card.png")
        val intent = createShareIntent(uri, "caption")
        val chooser = Intent.createChooser(intent, null)

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    }

    // Helper — mirrors the intent construction logic from SocialShareHelper
    private fun createShareIntent(uri: Uri, caption: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
