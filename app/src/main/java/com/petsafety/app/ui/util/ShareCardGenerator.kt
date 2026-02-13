package com.petsafety.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.petsafety.app.R

object ShareCardGenerator {
    private const val CARD_SIZE = 1080
    private val TEAL = Color.parseColor("#4DB8C4")

    suspend fun generate(
        context: Context,
        petName: String,
        petImageUrl: String?,
        petSpecies: String,
        city: String? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Teal background
        val bgPaint = Paint().apply { color = TEAL }
        canvas.drawRect(0f, 0f, CARD_SIZE.toFloat(), CARD_SIZE.toFloat(), bgPaint)

        // Logo (compact)
        try {
            val logoDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.logo_new, null)
            if (logoDrawable != null) {
                val logoBitmap = logoDrawable.toBitmap()
                val logoHeight = 60f
                val logoWidth = (logoBitmap.width.toFloat() / logoBitmap.height) * logoHeight
                val logoLeft = (CARD_SIZE - logoWidth) / 2
                canvas.drawBitmap(
                    logoBitmap,
                    Rect(0, 0, logoBitmap.width, logoBitmap.height),
                    RectF(logoLeft, 25f, logoLeft + logoWidth, 25f + logoHeight),
                    null
                )
            }
        } catch (_: Exception) {
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("TagMe Now", CARD_SIZE / 2f, 65f, textPaint)
        }

        // "Reunited!" text
        val reunitedPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(context.getString(R.string.share_card_reunited), CARD_SIZE / 2f, 130f, reunitedPaint)

        // Top divider
        val dividerPaint = Paint().apply {
            color = Color.argb(102, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawLine(140f, 155f, (CARD_SIZE - 140).toFloat(), 155f, dividerPaint)

        // Pet photo — maximized
        val photoRadius = 270f
        val photoCenterX = CARD_SIZE / 2f
        val photoCenterY = 460f

        // White border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(photoCenterX, photoCenterY, photoRadius + 6f, borderPaint)

        // Load pet photo
        var petBitmap: Bitmap? = null
        if (!petImageUrl.isNullOrBlank()) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(petImageUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    petBitmap = result.drawable.toBitmap()
                }
            } catch (_: Exception) { }
        }

        if (petBitmap != null) {
            val scaled = Bitmap.createScaledBitmap(
                petBitmap,
                (photoRadius * 2).toInt(),
                (photoRadius * 2).toInt(),
                true
            )
            val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val photoPaint = Paint().apply {
                isAntiAlias = true
                this.shader = shader
            }
            canvas.save()
            canvas.translate(photoCenterX - photoRadius, photoCenterY - photoRadius)
            canvas.drawCircle(photoRadius, photoRadius, photoRadius, photoPaint)
            canvas.restore()
        } else {
            val placeholderPaint = Paint().apply {
                color = Color.argb(51, 255, 255, 255)
                isAntiAlias = true
            }
            canvas.drawCircle(photoCenterX, photoCenterY, photoRadius, placeholderPaint)
            val pawPaint = Paint().apply {
                color = Color.WHITE
                textSize = 140f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("\uD83D\uDC3E", photoCenterX, photoCenterY + 50f, pawPaint)
        }

        // Pet name + city
        val nameText = if (!city.isNullOrBlank()) "$petName, $city" else petName
        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(nameText, CARD_SIZE / 2f, 800f, namePaint)

        // Bottom divider
        canvas.drawLine(140f, 845f, (CARD_SIZE - 140).toFloat(), 845f, dividerPaint)

        // Website
        val urlPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("senra.pet", CARD_SIZE / 2f, 900f, urlPaint)

        return bitmap
    }
}
