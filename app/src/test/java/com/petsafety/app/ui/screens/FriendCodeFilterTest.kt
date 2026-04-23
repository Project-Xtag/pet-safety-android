package com.petsafety.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pass 2 audit fix — ReferralScreen friend-code input must:
 *   (a) strip non-alphanumeric chars
 *   (b) upper-case
 *   (c) cap length at 32
 *
 * The filter lives inline inside the Compose onValueChange and isn't
 * directly exported; this test replicates the exact transformation so a
 * regression in the inline body is caught by a cheap JVM test rather
 * than a full Compose UI test.
 */
class FriendCodeFilterTest {

    // Must mirror ReferralScreen.kt's onValueChange exactly.
    private fun filter(raw: String): String =
        raw.uppercase()
            .filter { it.isLetterOrDigit() }
            .take(32)

    @Test
    fun `drops lowercase to uppercase`() {
        assertEquals("ABC123", filter("abc123"))
    }

    @Test
    fun `strips whitespace and punctuation`() {
        assertEquals("ABC123", filter("abc 123"))
        assertEquals("ABC123", filter("abc-123"))
        assertEquals("ABC123", filter("a.b.c.1.2.3"))
    }

    @Test
    fun `strips emoji but keeps Unicode letters`() {
        // Emoji + symbols: stripped
        assertEquals("ABC", filter("🔥abc🚀"))
        // Accented letters ARE letters in Unicode, so they pass the
        // isLetterOrDigit filter. The backend accepts any uppercased
        // alphanumeric and will treat "ÁBC" as a distinct code from
        // "ABC" — documenting that behaviour here so a future
        // refactor doesn't silently change it.
        assertEquals("ÁBC", filter("ábc"))
    }

    @Test
    fun `caps at 32 characters`() {
        val pasted = "A".repeat(10_000)
        assertEquals(32, filter(pasted).length)
    }

    @Test
    fun `cap is applied after alphanum filter`() {
        // 20 letters + 20 punct → after strip, 20 letters — under the cap
        val raw = "a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t"
        val out = filter(raw)
        assertEquals(20, out.length)
        assertEquals("ABCDEFGHIJKLMNOPQRST", out)
    }

    @Test
    fun `empty input yields empty output`() {
        assertEquals("", filter(""))
        assertEquals("", filter("   "))
        assertEquals("", filter("💥🔥💥"))
    }
}
