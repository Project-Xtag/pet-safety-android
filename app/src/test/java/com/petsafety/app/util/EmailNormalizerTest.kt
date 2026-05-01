package com.petsafety.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mirrors the backend `normalizeEmail` test expectations. Pins the
 * contract that the Android client always sends the canonical lowercased
 * email — fixing the cross-platform sync bug where Android auto-provisioned
 * a fresh empty user when casing differed from iOS/web.
 */
class EmailNormalizerTest {

    @Test
    fun lowercasesMixedCase() {
        assertEquals("foo@bar.com", EmailNormalizer.normalize("Foo@Bar.COM"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("foo@bar.com", EmailNormalizer.normalize("  foo@bar.com  "))
    }

    @Test
    fun handlesTabAndNewline() {
        assertEquals("foo@bar.com", EmailNormalizer.normalize("\tfoo@bar.com\n"))
    }

    @Test
    fun returnsEmptyStringForNull() {
        assertEquals("", EmailNormalizer.normalize(null))
    }

    @Test
    fun returnsEmptyStringForBlank() {
        assertEquals("", EmailNormalizer.normalize("   "))
    }

    @Test
    fun isIdempotent() {
        val first = EmailNormalizer.normalize("Foo@Bar.COM ")
        val second = EmailNormalizer.normalize(first)
        assertEquals(first, second)
    }

    @Test
    fun preservesPlusTagInLocalPart() {
        assertEquals(
            "foo+android@example.com",
            EmailNormalizer.normalize("Foo+Android@Example.com"),
        )
    }
}
