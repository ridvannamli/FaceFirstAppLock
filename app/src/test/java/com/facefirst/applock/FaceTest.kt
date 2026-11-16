package com.facefirst.applock

import com.facefirst.applock.face.Challenge
import com.facefirst.applock.face.FaceConfig
import com.facefirst.applock.face.FaceGate
import com.facefirst.applock.face.FaceMath
import com.facefirst.applock.face.FaceOutcome
import com.facefirst.applock.face.FaceSession
import com.facefirst.applock.face.FailReason
import com.facefirst.applock.face.FrameObs
import com.facefirst.applock.face.Hint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class FaceMathTest {
    @Test fun cosineAndNormalize() {
        assertEquals(1f, FaceMath.cosine(floatArrayOf(1f, 2f), floatArrayOf(2f, 4f)), 1e-5f)
        assertEquals(0f, FaceMath.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 1e-5f)
        assertEquals(-1f, FaceMath.cosine(floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f)), 1e-5f)
        assertArrayEquals(floatArrayOf(0.6f, 0.8f), FaceMath.l2(floatArrayOf(3f, 4f)), 1e-5f)
    }
    @Test fun bestSimilarityPicksClosestTemplate() {
        val t = listOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f))
        assertEquals(1f, FaceMath.bestSimilarity(floatArrayOf(0f, 2f), t), 1e-5f)
        assertEquals(-1f, FaceMath.bestSimilarity(floatArrayOf(1f, 0f), emptyList()), 0f)
    }
    @Test fun similarityTransformRecoversScaleRotationTranslation() {
        val ang = 0.3; val sc = 1.7f; val a = (sc * cos(ang)).toFloat(); val b = (sc * sin(ang)).toFloat()
        val src = FaceMath.REF
        val dst = FloatArray(10)
        for (i in 0 until 5) { val x = src[2 * i]; val y = src[2 * i + 1]; dst[2 * i] = a * x - b * y + 12f; dst[2 * i + 1] = b * x + a * y - 5f }
        assertArrayEquals(floatArrayOf(a, b, 12f, -5f), FaceMath.similarityTransform(src, dst), 1e-2f)
        // ters yön: dst -> src, noktaları referansa geri taşımalı
        val t = FaceMath.similarityTransform(dst, src)
        for (i in 0 until 5) {
            val x = dst[2 * i]; val y = dst[2 * i + 1]
            assertEquals(src[2 * i], t[0] * x - t[1] * y + t[2], 0.05f); assertEquals(src[2 * i + 1], t[1] * x + t[0] * y + t[3], 0.05f)
        }
    }
    @Test fun landmarksOrderedByImageX() {
        val o = FaceMath.orderLandmarks(floatArrayOf(9f, 1f), floatArrayOf(3f, 1f), floatArrayOf(6f, 5f), floatArrayOf(8f, 9f), floatArrayOf(4f, 9f))
        assertArrayEquals(floatArrayOf(3f, 1f, 9f, 1f, 6f, 5f, 4f, 9f, 8f, 9f), o, 0f)
    }
    @Test fun softmaxSumsToOne() {
        val s = FaceMath.softmax(floatArrayOf(1f, 3f, 2f))
        assertEquals(1f, s.sum(), 1e-5f); assertTrue(s[1] > s[2] && s[2] > s[0])
    }
    @Test fun scaledBoxStaysInsideImage() {
        val b = FaceMath.scaledBox(640, 480, 10, 20, 100, 120, 2.7f)
        assertTrue(b[0] >= 0 && b[1] >= 0 && b[2] <= 639 && b[3] <= 479 && b[2] > b[0] && b[3] > b[1])
        val c = FaceMath.scaledBox(640, 480, 200, 150, 100, 100, 2.7f)
        assertEquals(270, c[2] - c[0]); assertEquals(270, c[3] - c[1])
    }
    @Test fun templatesRoundTripAndRejectCorrupt() {
        val l = listOf(floatArrayOf(0.1f, -2f, 3f), floatArrayOf(4f, 5f, 6f))
        val d = FaceMath.decodeTemplates(FaceMath.encodeTemplates(l))
        assertEquals(2, d.size); assertArrayEquals(l[1], d[1], 0f)
        assertTrue(runCatching { FaceMath.decodeTemplates(ByteArray(11)) }.isFailure)
    }
}

class FaceSessionTest {
    private val cfg = FaceConfig()
    private val good = FrameObs(faces = 1, trackingId = 7, bigEnough = true, bright = true, frontal = true, eyeProb = 0.9f, similarity = 0.8f, liveScore = 0.95f)
    private fun run(s: FaceSession, frames: List<FrameObs>, t0: Long = 0, dt: Long = 100): FaceOutcome {
        var r: FaceOutcome = FaceOutcome.Pending(Hint.NO_FACE); var t = t0
        for (f in frames) { t += dt; r = s.onFrame(f, t) }
        return r
    }
    @Test fun successNeedsConsecutiveGoodFrames() {
        val s = FaceSession(cfg, null, 0)
        assertTrue(run(s, listOf(good, good)) is FaceOutcome.Pending)
        assertEquals(FaceOutcome.Success, s.onFrame(good, 300))
    }
    @Test fun badFrameResetsStreak() {
        val s = FaceSession(cfg, null, 0)
        assertTrue(run(s, listOf(good, good, good.copy(eyeProb = 0.1f), good, good)) is FaceOutcome.Pending)
    }
    @Test fun closedEyesNeverUnlock() {
        val s = FaceSession(cfg, null, 0)
        assertEquals(FaceOutcome.Failed(FailReason.TIMEOUT), run(s, List(100) { good.copy(eyeProb = 0.2f) }))
    }
    @Test fun wrongPersonFailsNotRecognized() {
        val s = FaceSession(cfg, null, 0)
        assertEquals(FaceOutcome.Failed(FailReason.NOT_RECOGNIZED), run(s, List(12) { good.copy(similarity = 0.3f) }))
    }
    @Test fun spoofFailsEvenIfIdentityMatches() {
        val s = FaceSession(cfg, null, 0)
        assertEquals(FaceOutcome.Failed(FailReason.SPOOF), run(s, List(6) { good.copy(liveScore = 0.2f) }))
    }
    @Test fun twoFacesOrDarkOrFarOrTurnedDoNotUnlock() {
        for (bad in listOf(good.copy(faces = 2), good.copy(bright = false), good.copy(bigEnough = false), good.copy(frontal = false), good.copy(faces = 0))) {
            assertTrue(run(FaceSession(cfg, null, 0), List(20) { bad }) is FaceOutcome.Pending)
        }
    }
    @Test fun timeoutFails() {
        val s = FaceSession(cfg, null, 0)
        assertEquals(FaceOutcome.Failed(FailReason.TIMEOUT), s.onFrame(good.copy(faces = 0), 9_000))
    }
    @Test fun tickOnlyTimesOutAndDoesNotResetStreak() {
        val s = FaceSession(cfg, null, 0)
        run(s, listOf(good, good))
        assertEquals(null, s.onTick(250))
        assertEquals(FaceOutcome.Success, s.onFrame(good, 300))
        val t = FaceSession(cfg, null, 0)
        assertEquals(FaceOutcome.Failed(FailReason.TIMEOUT), t.onTick(8_001))
    }
    @Test fun outcomeIsFinal() {
        val s = FaceSession(cfg, null, 0)
        run(s, List(12) { good.copy(similarity = 0.1f) })
        assertEquals(FaceOutcome.Failed(FailReason.NOT_RECOGNIZED), run(s, List(5) { good }))
    }
    @Test fun cameraErrorFails() = assertEquals(FaceOutcome.Failed(FailReason.CAMERA), FaceSession(cfg, null, 0).onFrame(FrameObs(error = true), 10))
    @Test fun blinkChallengeRequiresBlinkThenReconfirm() {
        val s = FaceSession(cfg, Challenge.BLINK, 0)
        assertEquals(FaceOutcome.Pending(Hint.BLINK), run(s, listOf(good, good, good)))
        assertEquals(FaceOutcome.Pending(Hint.BLINK), run(s, List(10) { good }, 300)) // kırpmadan geçmez
        run(s, listOf(good.copy(eyeProb = 0.1f), good), 1_300)
        assertEquals(FaceOutcome.Success, run(s, listOf(good, good), 1_500))
    }
    @Test fun turnChallenge() {
        val s = FaceSession(cfg, Challenge.TURN, 0)
        run(s, listOf(good, good, good))
        run(s, listOf(good.copy(frontal = false, yaw = 30f), good.copy(yaw = 3f)), 300)
        assertEquals(FaceOutcome.Success, run(s, listOf(good, good), 500))
    }
    @Test fun smileChallenge() {
        val s = FaceSession(cfg, Challenge.SMILE, 0)
        run(s, listOf(good, good, good))
        assertTrue(run(s, listOf(good.copy(smileProb = 0.1f)), 300) is FaceOutcome.Pending)
        run(s, listOf(good.copy(smileProb = 0.9f)), 400)
        assertEquals(FaceOutcome.Success, run(s, listOf(good, good), 500))
    }
    @Test fun faceSwapDuringChallengeRestarts() {
        val s = FaceSession(cfg, Challenge.BLINK, 0)
        run(s, listOf(good, good, good))
        run(s, listOf(good.copy(trackingId = 8, eyeProb = 0.1f), good.copy(trackingId = 8)), 300) // başka yüz kırptı
        assertTrue(run(s, listOf(good.copy(trackingId = 8), good.copy(trackingId = 8)), 500) is FaceOutcome.Pending)
    }
    @Test fun challengeConfirmStillChecksIdentityAndLiveness() {
        val s = FaceSession(cfg, Challenge.BLINK, 0)
        run(s, listOf(good, good, good)); run(s, listOf(good.copy(eyeProb = 0.1f), good), 300)
        assertTrue(run(s, List(5) { good.copy(liveScore = 0.1f) }, 500) is FaceOutcome.Pending)
    }
}

class FaceGateTest {
    private val h = 60L * 60 * 1000
    @Test fun allowedAfterPinThisBoot() = assertTrue(FaceGate.allowed(3, 1_000, 0, 3, 5_000))
    @Test fun notAllowedBeforeAnyPin() = assertFalse(FaceGate.allowed(null, null, 0, 3, 5_000))
    @Test fun notAllowedAfterReboot() = assertFalse(FaceGate.allowed(3, 1_000, 0, 4, 500))
    @Test fun notAllowedAfter48h() { assertTrue(FaceGate.allowed(3, 0, 0, 3, 47 * h)); assertFalse(FaceGate.allowed(3, 0, 0, 3, 48 * h)) }
    @Test fun notAllowedAfterThreeFails() { assertTrue(FaceGate.allowed(3, 0, 2, 3, h)); assertFalse(FaceGate.allowed(3, 0, 3, 3, h)) }
}
