package com.petsafety.app.util

import com.petsafety.app.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [QrCodeParser.extractTagCode] and [QrCodeParser.isValidTagCode].
 * Uses Robolectric because extractTagCode calls android.net.Uri.parse(), which
 * is only available on-device or under Robolectric.
 *
 * This matrix must stay in sync with the backend (extractTagCode.test.ts) and
 * the web (lib/extractTagCode.ts tests). If you add a case here, add it there
 * and vice versa — the wire format is the contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class QrCodeParserTest {

    // ---------- Bare codes (pass-through) ----------

    @Test fun `bare nanoid is returned unchanged`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("T5NAJlr2"))
    }

    @Test fun `TEST-prefixed 13 char code unchanged`() {
        assertEquals("TEST_EKulb71M", QrCodeParser.extractTagCode("TEST_EKulb71M"))
    }

    @Test fun `PS- prefixed code unchanged`() {
        assertEquals("PS-12345678", QrCodeParser.extractTagCode("PS-12345678"))
    }

    @Test fun `case preserved on bare code`() {
        assertEquals("aBcDeFgH", QrCodeParser.extractTagCode("aBcDeFgH"))
    }

    // ---------- Prod URL format ----------

    @Test fun `extracts from canonical prod URL`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/t/T5NAJlr2"))
    }

    @Test fun `extracts from www-prefixed host`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://www.senra.pet/t/T5NAJlr2"))
    }

    @Test fun `extracts from http (non-TLS)`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("http://senra.pet/t/T5NAJlr2"))
    }

    // ---------- Case variants on URL scaffolding ----------

    @Test fun `accepts uppercase scheme and host, preserves code case`() {
        // Real legacy import example
        assertEquals("TEST_OVYYMTPA", QrCodeParser.extractTagCode("HTTPS://SENRA.PET/T/TEST_OVYYMTPA"))
    }

    @Test fun `accepts uppercase path segment`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/T/T5NAJlr2"))
    }

    @Test fun `accepts mixed case hostname`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://Senra.Pet/t/T5NAJlr2"))
    }

    // ---------- /qr/ path variant ----------

    @Test fun `extracts from qr path`() {
        assertEquals("PS-12345678", QrCodeParser.extractTagCode("https://senra.pet/qr/PS-12345678"))
    }

    @Test fun `extracts from uppercase QR path`() {
        assertEquals("PS-12345678", QrCodeParser.extractTagCode("https://senra.pet/QR/PS-12345678"))
    }

    // ---------- Country-prefixed paths ----------

    @Test fun `extracts from country-prefixed t path`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/hu/t/T5NAJlr2"))
    }

    @Test fun `extracts from country-prefixed qr path`() {
        assertEquals("PS-12345678", QrCodeParser.extractTagCode("https://senra.pet/de/qr/PS-12345678"))
    }

    // ---------- URL suffixes ----------

    @Test fun `ignores trailing slash`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/t/T5NAJlr2/"))
    }

    @Test fun `ignores query string`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/t/T5NAJlr2?utm=1"))
    }

    @Test fun `ignores hash fragment`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/t/T5NAJlr2#found"))
    }

    // ---------- Custom scheme ----------

    @Test fun `extracts from senra scheme`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("senra://tag/T5NAJlr2"))
    }

    // ---------- Whitespace ----------

    @Test fun `trims leading whitespace`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("   T5NAJlr2"))
    }

    @Test fun `trims trailing newline from handheld scanners`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("T5NAJlr2\r\n"))
    }

    // ---------- Foreign hosts (pass-through) ----------

    @Test fun `foreign host URL passed through unchanged`() {
        val url = "https://evil.example.com/t/T5NAJlr2"
        assertEquals(url, QrCodeParser.extractTagCode(url))
    }

    @Test fun `subdomain attack URL passed through`() {
        val url = "https://senra.pet.evil.example.com/t/T5NAJlr2"
        assertEquals(url, QrCodeParser.extractTagCode(url))
    }

    // ---------- Malformed input ----------

    @Test fun `empty string returns empty`() {
        assertEquals("", QrCodeParser.extractTagCode(""))
    }

    @Test fun `whitespace only returns empty`() {
        assertEquals("", QrCodeParser.extractTagCode("   "))
    }

    @Test fun `non-URL garbage passes through`() {
        assertEquals("<script>alert(1)</script>", QrCodeParser.extractTagCode("<script>alert(1)</script>"))
    }

    @Test fun `URL with empty code path passes through`() {
        val url = "https://senra.pet/t/"
        assertEquals(url, QrCodeParser.extractTagCode(url))
    }

    // ---------- Prod contract (regression) ----------

    @Test fun `prod URL form produces the bare stored form`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/t/T5NAJlr2"))
    }

    @Test fun `prod URL form with country prefix produces bare stored form`() {
        assertEquals("T5NAJlr2", QrCodeParser.extractTagCode("https://senra.pet/hu/t/T5NAJlr2"))
    }

    // ---------- isValidTagCode ----------

    @Test fun `isValid accepts 8-char nanoid`() {
        assertTrue(QrCodeParser.isValidTagCode("T5NAJlr2"))
    }

    @Test fun `isValid accepts 13-char TEST_ code`() {
        assertTrue(QrCodeParser.isValidTagCode("TEST_EKulb71M"))
    }

    @Test fun `isValid rejects empty`() {
        assertFalse(QrCodeParser.isValidTagCode(""))
    }

    @Test fun `isValid rejects too short`() {
        assertFalse(QrCodeParser.isValidTagCode("ABCDE"))
    }

    @Test fun `isValid rejects too long`() {
        assertFalse(QrCodeParser.isValidTagCode("A".repeat(33)))
    }

    @Test fun `isValid rejects URL residue`() {
        assertFalse(QrCodeParser.isValidTagCode("senra.pet/t/T5NAJlr2"))
    }

    @Test fun `isValid rejects full URL`() {
        assertFalse(QrCodeParser.isValidTagCode("https://senra.pet/t/T5NAJlr2"))
    }

    @Test fun `isValid rejects spaces`() {
        assertFalse(QrCodeParser.isValidTagCode("T5NAJlr2 "))
    }

    @Test fun `isValid rejects unicode`() {
        assertFalse(QrCodeParser.isValidTagCode("café1234"))
    }

    @Test fun `isValid rejects special chars`() {
        assertFalse(QrCodeParser.isValidTagCode("T5NAJlr@"))
    }
}
