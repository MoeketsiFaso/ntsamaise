package com.moeketsifaso.ntsamaise

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.vision.common.InputImage
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrintedTextRecognizerTest {
    @Test
    fun currencyTextProducesOnlyAPossibleDenomination() {
        // Synthetic text verifies OCR-to-parser integration, not real-banknote accuracy.
        val bitmap = Bitmap.createBitmap(1400, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 64f
        }
        canvas.drawText("CENTRAL BANK OF LESOTHO", 60f, 130f, paint)
        canvas.drawText("50 MALOTI", 60f, 280f, paint)
        canvas.drawText("50", 60f, 430f, paint)
        val recognizer = PrintedTextRecognizer(TextCaptureRequests())
        try {
            val text = recognizer.recognize(InputImage.fromBitmap(bitmap, 0))
            val result = LotiCurrencyReader.read(text)
            assertEquals(CurrencyReadingStatus.POSSIBLE_MATCH, result.status)
            assertEquals(50, result.denomination)
            assertTrue(result.message.startsWith("Possible 50 maloti"))
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }

    @Test
    fun bundledRecognizerReadsPrintedText() {
        val bitmap = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 80f
        }
        canvas.drawText("READ TEXT 123", 60f, 150f, paint)
        val recognizer = PrintedTextRecognizer(TextCaptureRequests())
        try {
            val text = recognizer.recognize(InputImage.fromBitmap(bitmap, 0))
            assertTrue("Expected the printed test phrase", text.contains("READ TEXT 123"))
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }
}
