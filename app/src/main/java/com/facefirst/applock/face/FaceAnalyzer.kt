package com.facefirst.applock.face

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs

/**
 * Kamera karesi → [FrameObs]. Kare hiçbir yere kaydedilmez/gönderilmez; işlenip atılır.
 * [templates] null ise (kayıt modu) benzerlik hesaplanmaz, şablon geri verilir.
 */
class FaceAnalyzer(
    private val context: android.content.Context,
    private val templates: List<FloatArray>?,
    private val onResult: (FrameObs, FloatArray?) -> Unit,
) : ImageAnalysis.Analyzer {
    @Volatile var closed = false
    private var engineOrNull: FaceEngine? = null
    private val engine: FaceEngine get() = engineOrNull ?: FaceEngine(context).also { engineOrNull = it } // modeller analiz iş parçacığında yüklenir
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.2f)
            .enableTracking()
            .build()
    )

    override fun analyze(image: ImageProxy) {
        try {
            if (closed) return
            val raw = image.toBitmap()
            val rot = image.imageInfo.rotationDegrees
            val frame = if (rot == 0) raw else
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
            val faces = Tasks.await(detector.process(InputImage.fromBitmap(frame, 0)))
            if (closed) return
            if (faces.size != 1) { onResult(FrameObs(faces = faces.size), null); return }
            val f = faces[0]
            fun pt(type: Int) = f.getLandmark(type)?.position?.let { floatArrayOf(it.x, it.y) }
            val le = pt(FaceLandmark.LEFT_EYE); val re = pt(FaceLandmark.RIGHT_EYE); val nose = pt(FaceLandmark.NOSE_BASE)
            val lm = pt(FaceLandmark.MOUTH_LEFT); val rm = pt(FaceLandmark.MOUTH_RIGHT)
            val hasLm = le != null && re != null && nose != null && lm != null && rm != null
            val eye = minOf(f.leftEyeOpenProbability ?: -1f, f.rightEyeOpenProbability ?: -1f)
            var obs = FrameObs(
                faces = 1, trackingId = f.trackingId,
                bigEnough = f.boundingBox.width() >= MIN_FACE_FRACTION * frame.width,
                frontal = hasLm && abs(f.headEulerAngleY) < MAX_ANGLE && abs(f.headEulerAngleX) < MAX_ANGLE,
                eyeProb = eye, smileProb = f.smilingProbability ?: 0f, yaw = f.headEulerAngleY,
            )
            var emb: FloatArray? = null
            if (hasLm && obs.bigEnough) {
                val aligned = engine.align(frame, FaceMath.orderLandmarks(le!!, re!!, nose!!, lm!!, rm!!))
                obs = obs.copy(bright = engine.brightness(aligned) >= MIN_BRIGHTNESS)
                if (obs.bright && obs.frontal && eye >= 0.5f) {
                    emb = engine.embed(aligned)
                    obs = obs.copy(
                        liveScore = engine.liveScore(frame, f.boundingBox),
                        similarity = templates?.let { FaceMath.bestSimilarity(emb, it) },
                    )
                }
            }
            onResult(obs, emb)
        } catch (e: Exception) {
            if (!closed) onResult(FrameObs(error = true), null)
        } finally {
            image.close()
        }
    }

    /** Analiz iş parçacığından çağrılmalı. */
    fun close() { closed = true; detector.close(); engineOrNull?.close(); engineOrNull = null }

    private companion object {
        const val MIN_FACE_FRACTION = 0.28f
        const val MAX_ANGLE = 18f
        const val MIN_BRIGHTNESS = 45f
    }
}
