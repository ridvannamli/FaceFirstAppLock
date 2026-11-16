package com.facefirst.applock.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Cihaz üstü modeller (ağ kullanılmaz):
 * - mobile_facenet.tflite: 112x112 hizalı yüz → 128 boyutlu şablon (Qualcomm AI Hub dışa aktarımı, Apache-2.0).
 * - spoof_model_scale_*.tflite: MiniFASNet canlılık (ekran/kâğıt ayrımı), iki ölçek (Apache-2.0).
 * Tek iş parçacığından kullanılmalıdır.
 */
class FaceEngine(context: Context) : AutoCloseable {
    private val rec = Interpreter(load(context, "mobile_facenet.tflite"), Interpreter.Options().setNumThreads(2))
    private val spoofA = Interpreter(load(context, "spoof_model_scale_2_7.tflite"), Interpreter.Options().setNumThreads(2))
    private val spoofB = Interpreter(load(context, "spoof_model_scale_4_0.tflite"), Interpreter.Options().setNumThreads(2))

    private val recIn1 = direct(3 * SIZE * SIZE * 4)
    private val recIn2 = direct(3 * SIZE * SIZE * 4)
    private val spoofIn = direct(SPOOF * SPOOF * 3 * 4)
    private val px = IntArray(SIZE * SIZE)
    private val spx = IntArray(SPOOF * SPOOF)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    /** 5 nokta (FaceMath.REF sırası) ile yüzü 112x112'ye hizalar. */
    fun align(frame: Bitmap, landmarks: FloatArray): Bitmap {
        val t = FaceMath.similarityTransform(landmarks, FaceMath.REF)
        val m = Matrix().apply { setValues(floatArrayOf(t[0], -t[1], t[2], t[1], t[0], t[3], 0f, 0f, 1f)) }
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(frame, m, paint)
        return out
    }

    /** Hizalı kırpımın ortalama parlaklığı (0-255). */
    fun brightness(aligned: Bitmap): Float {
        aligned.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        var sum = 0L
        for (p in px) sum += (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
        return sum.toFloat() / px.size
    }

    /** L2 normalize 128 boyutlu şablon. Model girişi: NCHW, RGB, [0,1]. */
    fun embed(aligned: Bitmap): FloatArray {
        aligned.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        recIn1.rewind(); recIn2.rewind()
        for (shift in intArrayOf(16, 8, 0)) for (p in px) {
            val v = ((p shr shift) and 0xFF) / 255f
            recIn1.putFloat(v); recIn2.putFloat(v)
        }
        recIn1.rewind(); recIn2.rewind()
        val out = Array(2) { FloatArray(128) }
        rec.runForMultipleInputsOutputs(arrayOf<Any>(recIn1, recIn2), mapOf<Int, Any>(0 to out))
        return FaceMath.l2(out[0])
    }

    /** "Gerçek yüz" olasılığı (iki ölçeğin ortalaması, 0-1). */
    fun liveScore(frame: Bitmap, box: Rect): Float =
        (spoof(spoofA, frame, box, 2.7f) + spoof(spoofB, frame, box, 4.0f)) / 2f

    private fun spoof(model: Interpreter, frame: Bitmap, box: Rect, scale: Float): Float {
        val b = FaceMath.scaledBox(frame.width, frame.height, box.left, box.top, box.width().coerceAtLeast(1), box.height().coerceAtLeast(1), scale)
        val w = (b[2] - b[0]).coerceAtLeast(1); val h = (b[3] - b[1]).coerceAtLeast(1)
        val crop = Bitmap.createBitmap(frame, b[0], b[1], w.coerceAtMost(frame.width - b[0]), h.coerceAtMost(frame.height - b[1]))
        val small = Bitmap.createScaledBitmap(crop, SPOOF, SPOOF, true)
        small.getPixels(spx, 0, SPOOF, 0, 0, SPOOF, SPOOF)
        spoofIn.rewind()
        for (p in spx) { // model BGR, 0-255 bekler
            spoofIn.putFloat((p and 0xFF).toFloat()); spoofIn.putFloat(((p shr 8) and 0xFF).toFloat()); spoofIn.putFloat(((p shr 16) and 0xFF).toFloat())
        }
        spoofIn.rewind()
        val out = Array(1) { FloatArray(3) }
        model.run(spoofIn, out)
        return FaceMath.softmax(out[0])[1]
    }

    override fun close() { rec.close(); spoofA.close(); spoofB.close() }

    private companion object {
        const val SIZE = 112
        const val SPOOF = 80
        fun direct(n: Int): ByteBuffer = ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder())
        fun load(context: Context, name: String): ByteBuffer {
            val bytes = context.assets.open(name).use { it.readBytes() }
            return direct(bytes.size).apply { put(bytes); rewind() }
        }
    }
}
