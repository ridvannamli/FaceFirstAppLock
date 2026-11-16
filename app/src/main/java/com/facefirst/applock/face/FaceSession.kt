package com.facefirst.applock.face

import kotlin.math.abs

/** Tek kameranın tek karesinden çıkan gözlem. */
data class FrameObs(
    val faces: Int = 0,
    val trackingId: Int? = null,
    val bigEnough: Boolean = false,
    val bright: Boolean = false,
    val frontal: Boolean = false,
    val eyeProb: Float = -1f,      // iki gözün açık olma olasılığının küçüğü; bilinmiyorsa -1
    val smileProb: Float = 0f,
    val yaw: Float = 0f,
    val similarity: Float? = null, // kayıtlı şablonlara en yüksek benzerlik
    val liveScore: Float? = null,  // canlılık modeli "gerçek yüz" olasılığı
    val error: Boolean = false,
)

enum class Hint { NO_FACE, MULTIPLE, CLOSER, DARK, LOOK_STRAIGHT, OPEN_EYES, CHECKING, BLINK, TURN, SMILE }
enum class FailReason { TIMEOUT, NOT_RECOGNIZED, SPOOF, CAMERA }
enum class Challenge { BLINK, TURN, SMILE }

sealed interface FaceOutcome {
    data class Pending(val hint: Hint) : FaceOutcome
    data object Success : FaceOutcome
    data class Failed(val reason: FailReason) : FaceOutcome
}

data class FaceConfig(
    val matchThr: Float = 0.60f,
    val liveThr: Float = 0.70f,
    val eyeThr: Float = 0.5f,
    val needFrames: Int = 3,
    val confirmFrames: Int = 2,
    val timeoutMs: Long = 8_000,
    val challengeTimeoutMs: Long = 14_000,
    val maxMismatchFrames: Int = 12,
    val maxSpoofFrames: Int = 6,
)

/**
 * Yüz doğrulama oturumu. Başarı için art arda [FaceConfig.needFrames] karede:
 * tek yüz + yeterince yakın + aydınlık + düz bakış + gözler açık + canlı + kayıtlı yüzle eşleşme.
 * [challenge] verilirse ardından rastgele hareket (göz kırp / başı çevir / gülümse) ve yeniden eşleşme istenir.
 */
class FaceSession(private val cfg: FaceConfig, private val challenge: Challenge?, private val startMs: Long) {
    private enum class Phase { IDENTIFY, CHALLENGE, CONFIRM }
    private var phase = Phase.IDENTIFY
    private var streak = 0
    private var mismatch = 0
    private var spoof = 0
    private var trackId: Int? = null
    private var armed = false
    private var done: FaceOutcome? = null

    fun onFrame(o: FrameObs, now: Long): FaceOutcome {
        done?.let { return it }
        val limit = if (challenge == null) cfg.timeoutMs else cfg.challengeTimeoutMs
        val r = when {
            o.error -> FaceOutcome.Failed(FailReason.CAMERA)
            now - startMs > limit -> FaceOutcome.Failed(FailReason.TIMEOUT)
            else -> step(o)
        }
        if (r !is FaceOutcome.Pending) done = r
        return r
    }

    /** Kare gelmese de zaman aşımını işletir; başka durumu değiştirmez. */
    fun onTick(now: Long): FaceOutcome? {
        done?.let { return it }
        val limit = if (challenge == null) cfg.timeoutMs else cfg.challengeTimeoutMs
        if (now - startMs > limit) { done = FaceOutcome.Failed(FailReason.TIMEOUT); return done }
        return null
    }

    /** Kimlik kontrolü: null = geçti, aksi hâlde bekleme ipucu ya da başarısızlık. */
    private fun identity(o: FrameObs): FaceOutcome? {
        if (o.faces == 0) return FaceOutcome.Pending(Hint.NO_FACE)
        if (o.faces > 1) return FaceOutcome.Pending(Hint.MULTIPLE)
        if (!o.bigEnough) return FaceOutcome.Pending(Hint.CLOSER)
        if (!o.bright) return FaceOutcome.Pending(Hint.DARK)
        if (!o.frontal) return FaceOutcome.Pending(Hint.LOOK_STRAIGHT)
        if (o.eyeProb < cfg.eyeThr) return FaceOutcome.Pending(Hint.OPEN_EYES)
        val live = o.liveScore ?: return FaceOutcome.Pending(Hint.CHECKING)
        val sim = o.similarity ?: return FaceOutcome.Pending(Hint.CHECKING)
        if (live < cfg.liveThr) {
            return if (++spoof >= cfg.maxSpoofFrames) FaceOutcome.Failed(FailReason.SPOOF) else FaceOutcome.Pending(Hint.CHECKING)
        }
        if (sim < cfg.matchThr) {
            return if (++mismatch >= cfg.maxMismatchFrames) FaceOutcome.Failed(FailReason.NOT_RECOGNIZED) else FaceOutcome.Pending(Hint.CHECKING)
        }
        return null
    }

    private fun restart() { phase = Phase.IDENTIFY; streak = 0; armed = false; trackId = null }

    private fun step(o: FrameObs): FaceOutcome = when (phase) {
        Phase.IDENTIFY -> {
            val bad = identity(o)
            if (bad != null) { streak = 0; bad }
            else if (++streak < cfg.needFrames) FaceOutcome.Pending(Hint.CHECKING)
            else if (challenge == null) FaceOutcome.Success
            else { phase = Phase.CHALLENGE; trackId = o.trackingId; armed = false; streak = 0; FaceOutcome.Pending(hintOf(challenge)) }
        }
        Phase.CHALLENGE -> {
            val c = challenge!!
            // Aynı yüz kadrajda kalmalı; takip koparsa (yüz değişimi/kaybı) baştan başlanır.
            if (o.faces != 1 || trackId == null || o.trackingId != trackId) { restart(); FaceOutcome.Pending(Hint.NO_FACE) }
            else {
                val finished = when (c) {
                    Challenge.BLINK -> { if (o.eyeProb in 0f..0.25f) armed = true; armed && o.eyeProb > 0.6f }
                    Challenge.TURN -> { if (abs(o.yaw) > 22f) armed = true; armed && abs(o.yaw) < 10f }
                    Challenge.SMILE -> { if (o.smileProb < 0.4f && !armed) armed = true; armed && o.smileProb > 0.75f }
                }
                if (finished) { phase = Phase.CONFIRM; streak = 0 }
                FaceOutcome.Pending(if (finished) Hint.CHECKING else hintOf(c))
            }
        }
        Phase.CONFIRM -> {
            if (o.faces != 1 || o.trackingId != trackId) { restart(); FaceOutcome.Pending(Hint.NO_FACE) }
            else {
                val bad = identity(o)
                if (bad != null) { streak = 0; if (bad is FaceOutcome.Pending && bad.hint == Hint.OPEN_EYES) FaceOutcome.Pending(Hint.LOOK_STRAIGHT) else bad }
                else if (++streak >= cfg.confirmFrames) FaceOutcome.Success
                else FaceOutcome.Pending(Hint.CHECKING)
            }
        }
    }

    private fun hintOf(c: Challenge) = when (c) { Challenge.BLINK -> Hint.BLINK; Challenge.TURN -> Hint.TURN; Challenge.SMILE -> Hint.SMILE }
}

/** Yüzle açmaya izin kuralı: PIN bu açılışta ve son 48 saatte girilmiş olmalı; art arda yüz hatası sınırı aşılmamalı. */
object FaceGate {
    const val MAX_FAILS = 3
    const val MAX_AGE_MS = 48L * 60 * 60 * 1000
    fun allowed(pinOkBoot: Int?, pinOkElapsed: Long?, fails: Int, nowBoot: Int, nowElapsed: Long): Boolean =
        pinOkBoot != null && pinOkElapsed != null && pinOkBoot == nowBoot &&
            nowElapsed - pinOkElapsed in 0 until MAX_AGE_MS && fails < MAX_FAILS
}
