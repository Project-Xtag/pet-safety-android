package com.petsafety.app.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * JVM coverage for the parts of [VaccinationCertificateEncoder] that don't need
 * the Android framework: the magic-byte sniff, the passthrough returns, and the
 * sub-28 null path (via an injected decode). The actual HEIF→JPEG decode runs
 * in the instrumented test on an API≥28 device.
 */
class VaccinationCertificateEncoderTest {

    // A genuine 8×8 HEIC (ftypheic / HEVC), produced via `sips -s format heic`.
    // Reused verbatim from the iOS encoder test so the sniff is exercised
    // against REAL HEIC bytes — a renamed .heic JPEG would carry JPEG magic
    // bytes and sniff as JPEG (false green); this catches that.
    private val heicBase64 =
        "AAAAJGZ0eXBoZWljAAAAAG1pZjFNaVBybWlhZk1pSEJoZWljAAABw21ldGEAAAAAAAAAIWhk" +
        "bHIAAAAAAAAAAHBpY3QAAAAAAAAAAAAAAAAAAAAAJGRpbmYAAAAcZHJlZgAAAAAAAAABAAAA" +
        "DHVybCAAAAABAAAADnBpdG0AAAAAAAEAAAA4aWluZgAAAAAAAgAAABVpbmZlAgAAAAABAABo" +
        "dmMxAAAAABVpbmZlAgAAAQACAABFeGlmAAAAABppcmVmAAAAAAAAAA5jZHNjAAIAAQABAAAA" +
        "5mlwcnAAAADFaXBjbwAAABNjb2xybmNseAACAAIABoAAAAAMY2xsaQDLAEAAAAAUaXNwZQAA" +
        "AAAAAAAIAAAACAAAAAlpcm90AAAAABBwaXhpAAAAAAMICAgAAABxaHZjQwEDcAAAALAAAAAA" +
        "AB7wAPz9+PgAAAsDoAABABdAAQwB//8DcAAAAwCwAAADAAADAB5wJKEAAQAjQgEBA3AAAAMA" +
        "sAAAAwAAAwAeoBQgQcCbDuIe5FlU3AgIGAKiAAEACUQBwGFyyERTZAAAABlpcG1hAAAAAAAA" +
        "AAEAAQaBAgMFhoQAAAAsaWxvYwAAAABEAAACAAEAAAABAAACRQAAAGIAAgAAAAEAAAH3AAAA" +
        "TgAAAAFtZGF0AAAAAAAAAMAAAAAGRXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAA" +
        "A6ABAAMAAAABAAEAAKACAAQAAAABAAAAtKADAAQAAAABAAAAtAAAAAAAAABeKAGvo8eAD8pA" +
        "fAApft2rv1oPTd1EHQd79rQ3jN8bxRX4LOQIPGz1u3Nm9BRuEiYfEWLdTg6KGdhdG/JjLq6X" +
        "2sAZw3//w0v8QlK/oySgHDTff+nuOzVDhcKWXuT52A=="
    private fun heicBytes(): ByteArray = Base64.getDecoder().decode(heicBase64)

    // ---- sniff ----

    @Test
    fun `sniff - genuine HEIC is OTHER, not a passthrough format`() {
        assertEquals(VaccinationCertificateEncoder.Format.OTHER, VaccinationCertificateEncoder.sniff(heicBytes()))
    }

    @Test
    fun `sniff - JPEG magic`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x00, 0x10)
        assertEquals(VaccinationCertificateEncoder.Format.JPEG, VaccinationCertificateEncoder.sniff(jpeg))
    }

    @Test
    fun `sniff - PNG magic`() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)
        assertEquals(VaccinationCertificateEncoder.Format.PNG, VaccinationCertificateEncoder.sniff(png))
    }

    @Test
    fun `sniff - WebP requires WEBP at offset 8, not just RIFF`() {
        val webp = "RIFF".toByteArray() + byteArrayOf(0, 0, 0, 0) + "WEBP".toByteArray()
        assertEquals(VaccinationCertificateEncoder.Format.WEBP, VaccinationCertificateEncoder.sniff(webp))

        // RIFF container that is NOT WebP (e.g. WAV) must NOT passthrough as an image.
        val wav = "RIFF".toByteArray() + byteArrayOf(0, 0, 0, 0) + "WAVE".toByteArray()
        assertEquals(VaccinationCertificateEncoder.Format.OTHER, VaccinationCertificateEncoder.sniff(wav))
    }

    // ---- encode: passthrough (no framework needed) ----

    @Test
    fun `encode - JPEG passes through untouched with concrete mime`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
        val out = VaccinationCertificateEncoder.encode(jpeg)!!
        assertEquals("image/jpeg", out.mimeType)
        assertArrayEquals(jpeg, out.data)
    }

    @Test
    fun `encode - PNG and WebP pass through with concrete mime`() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals("image/png", VaccinationCertificateEncoder.encode(png)!!.mimeType)

        val webp = "RIFF".toByteArray() + byteArrayOf(0, 0, 0, 0) + "WEBP".toByteArray()
        assertEquals("image/webp", VaccinationCertificateEncoder.encode(webp)!!.mimeType)
    }

    // ---- encode: sub-28 null path (injected decode) ----

    @Test
    fun `encode - HEIC with undecodable decode returns null (the API 26-27 path)`() {
        var decodeCalled = false
        val out = VaccinationCertificateEncoder.encode(heicBytes(), decode = {
            decodeCalled = true
            null // simulates BitmapFactory failing to decode HEIF on API 26/27
        })
        assertTrue("OTHER must route through the decoder", decodeCalled)
        assertNull("undecodable HEIC must yield null so the form can surface it", out)
    }
}
