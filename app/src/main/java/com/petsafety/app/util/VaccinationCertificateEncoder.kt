package com.petsafety.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Prepares a picked image for upload to the vaccination-certificate endpoint
 * (A.5), which accepts **JPEG / PNG / WebP only** and rejects HEIC and the
 * wildcard `image` MIME (Sentry PET-SAFETY-BACKEND-4, the profile-image bug).
 *
 * Mirrors the iOS `VaccinationCertificateEncoder` decision:
 *  - The three accepted formats **pass through untouched** (sniffed by magic
 *    bytes) — no re-encode, no downscale, so a photographed booklet page keeps
 *    its resolution and the server does the 2048px WebP step. (This is why we
 *    do NOT reuse PhotoCaptureSheet's 1200px/q80 compress for certs.)
 *  - HEIC / HEIF / anything else → transcoded to JPEG. iPhone/Android cameras
 *    can shoot HEIC, and an un-transcoded HEIC is a silent backend 415.
 *
 * minSdk is 26, but HEIF decode via BitmapFactory is API 28+. On API 26/27 a
 * library-picked HEIC decodes to `null` → [encode] returns `null`, and the
 * caller (the add form) must surface that gracefully. The camera path is JPEG
 * regardless, so camera certs are unaffected.
 *
 * [decode] is injectable so the null-guard is unit-testable on the JVM; the
 * actual HEIF→JPEG decode (production default) needs the Android framework and
 * is covered by an instrumented test on an API≥28 device.
 */
object VaccinationCertificateEncoder {

    data class Encoded(val data: ByteArray, val mimeType: String) {
        // Value-style equality on the byte content (data class default compares
        // the array by reference).
        override fun equals(other: Any?): Boolean =
            other is Encoded && mimeType == other.mimeType && data.contentEquals(other.data)
        override fun hashCode(): Int = 31 * data.contentHashCode() + mimeType.hashCode()
    }

    enum class Format { JPEG, PNG, WEBP, OTHER }

    fun encode(
        bytes: ByteArray,
        jpegQuality: Int = 90,
        decode: (ByteArray) -> Bitmap? = { BitmapFactory.decodeByteArray(it, 0, it.size) }
    ): Encoded? {
        when (sniff(bytes)) {
            Format.JPEG -> return Encoded(bytes, "image/jpeg")
            Format.PNG -> return Encoded(bytes, "image/png")
            Format.WEBP -> return Encoded(bytes, "image/webp")
            Format.OTHER -> Unit // fall through to transcode
        }
        // HEIC/HEIF/other → JPEG. Null = undecodable (e.g. HEIF on API 26/27).
        val bitmap = decode(bytes) ?: return null
        return try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            Encoded(out.toByteArray(), "image/jpeg")
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Magic-byte sniff. WebP must match `RIFF` at 0 **and** `WEBP` at offset 8 —
     * `RIFF` alone also fronts WAV/AVI, which would false-passthrough as an
     * image. Pure byte inspection (no decode) → JVM-testable.
     */
    fun sniff(b: ByteArray): Format = when {
        b.size >= 2 &&
            b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() -> Format.JPEG

        b.size >= 8 &&
            b[0] == 0x89.toByte() && b[1] == 0x50.toByte() && b[2] == 0x4E.toByte() &&
            b[3] == 0x47.toByte() && b[4] == 0x0D.toByte() && b[5] == 0x0A.toByte() &&
            b[6] == 0x1A.toByte() && b[7] == 0x0A.toByte() -> Format.PNG

        b.size >= 12 &&
            b[0] == 'R'.code.toByte() && b[1] == 'I'.code.toByte() &&
            b[2] == 'F'.code.toByte() && b[3] == 'F'.code.toByte() &&
            b[8] == 'W'.code.toByte() && b[9] == 'E'.code.toByte() &&
            b[10] == 'B'.code.toByte() && b[11] == 'P'.code.toByte() -> Format.WEBP

        else -> Format.OTHER
    }
}
