package com.facefirst.applock.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.facefirst.applock.BuildConfig
import com.facefirst.applock.R
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.face.Challenge
import com.facefirst.applock.face.FaceConfig
import com.facefirst.applock.face.FaceOutcome
import com.facefirst.applock.face.FaceSession
import com.facefirst.applock.face.FaceStore
import com.facefirst.applock.face.FrameObs
import com.facefirst.applock.face.Hint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun hintRes(h: Hint): Int = when (h) {
    Hint.NO_FACE -> R.string.face_no_face
    Hint.MULTIPLE -> R.string.face_multiple
    Hint.CLOSER -> R.string.face_closer
    Hint.DARK -> R.string.face_dark
    Hint.LOOK_STRAIGHT -> R.string.face_straight
    Hint.OPEN_EYES -> R.string.face_open_eyes
    Hint.CHECKING -> R.string.face_checking
    Hint.BLINK -> R.string.face_blink
    Hint.TURN -> R.string.face_turn
    Hint.SMILE -> R.string.face_smile
}

fun debugLine(o: FrameObs): String =
    "benzerlik ${o.similarity?.let { "%.2f".format(it) } ?: "-"} · canlılık ${o.liveScore?.let { "%.2f".format(it) } ?: "-"} · göz ${"%.2f".format(o.eyeProb)} · yüz ${o.faces}"

/**
 * Doğrulama: önce uygulama içi yüz tanıma (kayıtlıysa ve PIN kuralı izin veriyorsa), olmazsa PIN.
 * Parmak izi ve sistem biyometrisi kullanılmaz.
 */
@Composable
fun AuthScreen(
    pinRepo: PinRepository,
    title: String,
    challengeOn: Boolean,
    onSuccess: () -> Unit,
    onCancel: (() -> Unit)?,
    onForgot: (() -> Unit)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinLen by pinRepo.pinLength.collectAsState(initial = 4)
    var ready by remember { mutableStateOf(false) }
    var templates by remember { mutableStateOf<List<FloatArray>?>(null) }
    var faceMode by remember { mutableStateOf(false) }
    var faceTry by remember { mutableIntStateOf(0) }
    var canRetry by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(Hint.NO_FACE) }
    var debug by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<Int?>(null) }
    var wait by remember { mutableLongStateOf(0L) }
    var resetKey by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cam) templates = withContext(Dispatchers.IO) { FaceStore(context).load() }
        faceMode = templates != null && pinRepo.beginFaceAttempt()
        ready = true
    }
    LaunchedEffect(wait > 0) {
        while (wait > 0) { delay(1000); wait -= 1000 }
        if (msg == R.string.pin_locked) msg = null
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (!ready) return@Column
        if (faceMode && templates != null) {
            val session = remember(faceTry) {
                FaceSession(FaceConfig(), if (challengeOn) Challenge.entries.random() else null, SystemClock.elapsedRealtime())
            }
            fun fail() {
                if (finished || !faceMode) return
                faceMode = false; msg = R.string.face_failed
                scope.launch { canRetry = pinRepo.faceAllowed() } // deneme zaten başlangıçta sayıldı
            }
            fun handle(o: FrameObs) {
                if (finished || !faceMode) return
                if (BuildConfig.DEBUG) debug = debugLine(o)
                when (val r = session.onFrame(o, SystemClock.elapsedRealtime())) {
                    is FaceOutcome.Pending -> hint = r.hint
                    FaceOutcome.Success -> { finished = true; scope.launch { pinRepo.recordFaceSuccess(); onSuccess() } }
                    is FaceOutcome.Failed -> fail()
                }
            }
            // Kamera hiç kare vermezse de zaman aşımı işlesin.
            LaunchedEffect(faceTry) {
                while (faceMode && !finished) { delay(500); if (session.onTick(SystemClock.elapsedRealtime()) is FaceOutcome.Failed) fail() }
            }
            FaceCamera(templates, Modifier.padding(vertical = 16.dp).size(240.dp).clip(CircleShape)) { o, _ -> handle(o) }
            Text(stringResource(hintRes(hint)), style = MaterialTheme.typography.bodyLarge)
            if (BuildConfig.DEBUG) Text(debug, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            TextButton(onClick = { faceMode = false; scope.launch { canRetry = pinRepo.faceAllowed() } }) { Text(stringResource(R.string.use_pin)) }
        } else {
            msg?.let {
                Text(
                    if (it == R.string.pin_locked) stringResource(it, wait / 1000 + 1) else stringResource(it),
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp),
                )
            }
            Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PinPad(pinLen, resetKey, enabled = wait <= 0L) { pin ->
                    scope.launch {
                        when (val r = pinRepo.verify(pin)) {
                            PinResult.Success -> if (!finished) { finished = true; onSuccess() }
                            is PinResult.Wrong -> { wait = r.retryAfterMs; msg = if (r.retryAfterMs > 0) R.string.pin_locked else R.string.pin_wrong }
                            is PinResult.Locked -> { wait = r.retryAfterMs; msg = R.string.pin_locked }
                            PinResult.NotSet -> onCancel?.invoke()
                        }
                        resetKey++
                    }
                }
            }
            if (templates != null && canRetry) {
                TextButton(onClick = {
                    scope.launch {
                        if (pinRepo.beginFaceAttempt()) { msg = null; hint = Hint.NO_FACE; faceTry++; faceMode = true } else canRetry = false
                    }
                }) { Text(stringResource(R.string.face_retry)) }
            }
            onForgot?.let { TextButton(onClick = it) { Text(stringResource(R.string.forgot_pin)) } }
        }
        onCancel?.let { TextButton(onClick = it) { Text(stringResource(R.string.cancel)) } }
    }
}

/** Yüz kaydı: canlılık kontrolünden geçen [COUNT] kare alınır; fotoğraf saklanmaz, yalnızca şablonlar döner. */
@Composable
fun FaceEnroll(onDone: (List<FloatArray>) -> Unit, onCancel: () -> Unit) {
    val collected = remember { ArrayList<FloatArray>() }
    var count by remember { mutableIntStateOf(0) }
    var lastAt by remember { mutableLongStateOf(0L) }
    var hint by remember { mutableStateOf(Hint.NO_FACE) }
    var debug by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    val cfg = remember { FaceConfig() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.face_enroll_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.face_enroll_info), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
        FaceCamera(null, Modifier.padding(vertical = 16.dp).size(240.dp).clip(CircleShape)) { o, emb ->
            if (done) return@FaceCamera
            if (BuildConfig.DEBUG) debug = debugLine(o)
            hint = when {
                o.faces == 0 -> Hint.NO_FACE
                o.faces > 1 -> Hint.MULTIPLE
                !o.bigEnough -> Hint.CLOSER
                !o.bright -> Hint.DARK
                !o.frontal -> Hint.LOOK_STRAIGHT
                o.eyeProb < cfg.eyeThr -> Hint.OPEN_EYES
                else -> Hint.CHECKING
            }
            val now = SystemClock.elapsedRealtime()
            val live = o.liveScore
            if (emb != null && live != null && live >= cfg.liveThr && now - lastAt > 500 &&
                (collected.isEmpty() || com.facefirst.applock.face.FaceMath.cosine(emb, collected[0]) >= 0.5f)
            ) {
                collected.add(emb); lastAt = now; count = collected.size
                if (collected.size >= ENROLL_COUNT) { done = true; onDone(collected.toList()) }
            }
        }
        Text(stringResource(hintRes(hint)), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.face_enroll_progress, count, ENROLL_COUNT), Modifier.padding(8.dp))
        if (BuildConfig.DEBUG) Text(debug, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

const val ENROLL_COUNT = 5
