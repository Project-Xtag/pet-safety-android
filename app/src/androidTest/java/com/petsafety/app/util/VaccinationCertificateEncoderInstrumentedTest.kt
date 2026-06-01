package com.petsafety.app.util

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Base64

/**
 * On-device proof of the HEIF -> JPEG transcode — the branch the JVM test can't
 * exercise (BitmapFactory needs the Android framework). HEIF decode is API 28+,
 * so this is skipped (not failed) on API 26/27, where [VaccinationCertificateEncoder]
 * is contractually expected to return null instead (the form surfaces that).
 */
class VaccinationCertificateEncoderInstrumentedTest {

    // Same genuine 8x8 HEIC fixture as the JVM sniff test (verbatim from iOS).
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

    @Test
    fun heicTranscodesToJpegOnApi28Plus() {
        assumeTrue("HEIF decode requires API 28+", Build.VERSION.SDK_INT >= 28)
        val heic = heicBytes()
        val out = VaccinationCertificateEncoder.encode(heic)
        requireNotNull(out) { "a genuine HEIC must transcode (not return null) on API >= 28" }
        assertEquals("image/jpeg", out.mimeType)
        assertTrue(
            "output must carry JPEG magic bytes",
            out.data.size >= 2 && out.data[0] == 0xFF.toByte() && out.data[1] == 0xD8.toByte()
        )
        assertFalse("must be transcoded, not passed through", out.data.contentEquals(heic))
    }
}
