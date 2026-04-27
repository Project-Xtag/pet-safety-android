package com.petsafety.app.ui.components

import android.content.Intent
import com.petsafety.app.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [chooseWazeIntent] — the pure helper extracted from
 * [openWaze] so we can verify each branch without a real Context (audit H46).
 *
 * The original implementation called startActivity directly with an https://
 * URL and crashed with ActivityNotFoundException on the rare device with no
 * browser at all. The fix prefers the waze:// deep link, falls back to the
 * universal link, and reports NoneAvailable when nothing resolves so the
 * caller can show a toast instead of crashing.
 *
 * Robolectric is required because Intent + Uri are Android framework classes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class WazeIntentTest {

    private val lat = 47.4979
    private val lng = 19.0402

    @Test
    fun `prefers waze deep-link when Waze is installed`() {
        val choice = chooseWazeIntent(lat, lng) { intent ->
            // Simulate Waze installed: waze:// resolves, https:// does not.
            intent.data?.scheme == "waze"
        }
        assertTrue("should resolve", choice is WazeIntent.Resolved)
        val intent = (choice as WazeIntent.Resolved).intent
        assertEquals("waze", intent.data?.scheme)
        assertEquals("ll=$lat,$lng&navigate=yes", intent.data?.query)
        assertEquals(Intent.ACTION_VIEW, intent.action)
    }

    @Test
    fun `falls back to universal link when Waze is not installed but a browser is`() {
        val choice = chooseWazeIntent(lat, lng) { intent ->
            // Simulate no Waze, browser only.
            intent.data?.scheme == "https"
        }
        assertTrue("should resolve", choice is WazeIntent.Resolved)
        val intent = (choice as WazeIntent.Resolved).intent
        assertEquals("https", intent.data?.scheme)
        assertEquals("waze.com", intent.data?.host)
        assertTrue(
            "universal link should still carry coords",
            intent.data?.query?.contains("ll=$lat,$lng") == true,
        )
    }

    @Test
    fun `reports NoneAvailable when nothing resolves so caller shows a toast`() {
        // Pre-fix this path threw ActivityNotFoundException and crashed the
        // hosting Composable. The test pins the regression: caller now gets
        // a structured signal instead of an exception.
        val choice = chooseWazeIntent(lat, lng) { false }
        assertEquals(WazeIntent.NoneAvailable, choice)
    }

    @Test
    fun `does not silently fall back when Waze deep link is preferred`() {
        // Both schemes resolve — verify Waze wins (otherwise a Waze user is
        // unexpectedly bounced into Chrome instead of the navigation app).
        val choice = chooseWazeIntent(lat, lng) { true }
        val intent = (choice as WazeIntent.Resolved).intent
        assertEquals("waze", intent.data?.scheme)
    }
}
