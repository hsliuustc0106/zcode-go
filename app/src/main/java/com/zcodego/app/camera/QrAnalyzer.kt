package com.zcodego.app.camera

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Reads QR payloads from the camera Y plane. Frames that are not QR codes are ignored.
 * The callback is throttled so the same payload does not flood the UI.
 */
internal class QrAnalyzer(
    private val onQr: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            ),
        )
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastPosted = ""
    private var lastPostedAt = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val frame = yPlane(image) ?: return
            val text = decode(frame)?.takeIf { it.isNotBlank() } ?: return
            publish(text)
        } catch (_: Exception) {
            // A bad frame should not tear down the camera.
        } finally {
            image.close()
        }
    }

    private fun publish(text: String) {
        val now = SystemClock.elapsedRealtime()
        if (text == lastPosted && now - lastPostedAt < 1_200L) return
        lastPosted = text
        lastPostedAt = now
        mainHandler.post { onQr(text) }
    }

    private fun decode(frame: RotatedLuminance): String? {
        val source = PlanarYUVLuminanceSource(
            frame.bytes,
            frame.width,
            frame.height,
            0,
            0,
            frame.width,
            frame.height,
            false,
        )
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun yPlane(image: ImageProxy): RotatedLuminance? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        if (rowStride <= 0 || pixelStride <= 0) return null
        val buffer = plane.buffer.duplicate()
        buffer.rewind()
        val packed = ByteArray(width * height)
        val row = ByteArray(rowStride)
        for (y in 0 until height) {
            val toRead = minOf(rowStride, buffer.remaining())
            if (toRead <= 0) return null
            buffer.get(row, 0, toRead)
            var x = 0
            while (x < width) {
                val sourceIndex = x * pixelStride
                if (sourceIndex >= toRead) return null
                packed[y * width + x] = row[sourceIndex]
                x++
            }
        }
        return LuminanceRotation.rotate(packed, width, height, image.imageInfo.rotationDegrees)
    }
}
