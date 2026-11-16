package com.facefirst.applock.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.facefirst.applock.R
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.face.Challenge
import com.facefirst.applock.face.FaceStore
import com.facefirst.applock.lock.DeviceLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun Page(content: @Composable () -> Unit) = Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) { content() }

/** Hassas işlem öncesi mevcut PIN doğrulaması (ör. yüz kaydı, Google hesabını değiştirmek). */
@Composable
fun PinGate(repo: PinRepository, length: Int, onSuccess: () -> Unit, onCancel: () -> Unit) {
    var resetKey by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<Int?>(null) }
    var wait by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    Page {
        Text(stringResource(R.string.pin_enter), style = MaterialTheme.typography.titleLarge)
        msg?.let { Text(if (it == R.string.pin_locked) stringResource(it, wait / 1000 + 1) else stringResource(it), color = MaterialTheme.colorScheme.error) }
        Column(Modifier.padding(vertical = 16.dp)) {
            PinPad(length, resetKey, true) { pin ->
                scope.launch {
                    when (val r = repo.verify(pin)) {
                        PinResult.Success -> onSuccess()
                        is PinResult.Wrong -> { wait = r.retryAfterMs; msg = if (r.retryAfterMs > 0) R.string.pin_locked else R.string.pin_wrong }
                        is PinResult.Locked -> { wait = r.retryAfterMs; msg = R.string.pin_locked }
                        PinResult.NotSet -> onSuccess()
                    }
                    resetKey++
                }
            }
        }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

/** Kurtarma kodunu bir kez gösterir. Kod saklanmaz; kullanıcı not etmelidir. */
@Composable
fun RecoveryCodeScreen(code: String, onDone: () -> Unit) = Page {
    Text(stringResource(R.string.rc_title), style = MaterialTheme.typography.titleLarge)
    Text(code, Modifier.padding(vertical = 20.dp), style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
    Text(stringResource(R.string.rc_info), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
    Button(onClick = onDone) { Text(stringResource(R.string.rc_done)) }
}

/**
 * PIN'imi unuttum. İki yol:
 *  (A) telefonun ekran kilidi + canlılık ve rastgele hareketli yüz doğrulaması,
 *  (B) kurulumda verilen kurtarma kodu.
 * Hatalı denemeler ortak, artan beklemeyle sınırlanır. Sunucu/internet gerekmez.
 */
@Composable
fun ResetFlow(repo: PinRepository, onFinished: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasCode by repo.hasRecovery.collectAsState(initial = false)
    var step by remember { mutableIntStateOf(0) } // 0 seçim, 1 yüz, 2 kod, 3 yeni PIN, 4 yeni kod
    var err by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var viaCode by remember { mutableStateOf(false) }
    var newCode by remember { mutableStateOf("") }
    var templates by remember { mutableStateOf<List<FloatArray>?>(null) }
    val challenge = remember { Challenge.entries.random() }

    LaunchedEffect(Unit) {
        val cam = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (cam) templates = withContext(Dispatchers.IO) { FaceStore(context).load() }
    }
    fun lockedMsg(ms: Long) = context.getString(R.string.pin_locked, ms / 1000 + 1)

    when (step) {
        4 -> RecoveryCodeScreen(newCode) { onFinished() }
        3 -> PinFlow(
            requireOld = false, verifyOld = { PinResult.NotSet },
            save = { pin, len, _ ->
                repo.setPin(pin, len)
                if (viaCode) { newCode = repo.newRecoveryCode(); step = 4 } else onFinished() // kullanılan kod yenilenir
            },
            onCancel = onCancel,
        )
        2 -> Page {
            Text(stringResource(R.string.rc_enter_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = input, onValueChange = { input = it.take(20) }, singleLine = true,
                label = { Text(stringResource(R.string.rc_hint)) }, modifier = Modifier.padding(vertical = 12.dp),
            )
            err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                scope.launch {
                    when (val r = repo.verifyRecovery(input)) {
                        PinResult.Success -> { err = null; viaCode = true; step = 3 }
                        is PinResult.Wrong -> err = if (r.retryAfterMs > 0) lockedMsg(r.retryAfterMs) else context.getString(R.string.rc_wrong)
                        is PinResult.Locked -> err = lockedMsg(r.retryAfterMs)
                        PinResult.NotSet -> err = context.getString(R.string.rc_none)
                    }
                }
            }) { Text(stringResource(R.string.rc_verify)) }
            TextButton(onClick = { err = null; step = 0 }) { Text(stringResource(R.string.cancel)) }
        }
        1 -> Page {
            Text(stringResource(R.string.rs_face_step), style = MaterialTheme.typography.titleLarge)
            FaceCheck(templates.orEmpty(), challenge) { ok ->
                scope.launch {
                    if (ok) { repo.clearResetFails(); viaCode = false; err = null; step = 3 }
                    else { repo.recordResetFail(); err = context.getString(R.string.rs_face_failed); step = 0 }
                }
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
        else -> Page {
            Text(stringResource(R.string.rs_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.rs_info), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            err?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
            if (templates != null) {
                Button(onClick = {
                    err = null
                    scope.launch {
                        val rem = repo.resetRemaining()
                        if (rem > 0) { err = lockedMsg(rem); return@launch }
                        if (!DeviceLock.isSecure(context)) { err = context.getString(R.string.rs_need_lock); return@launch }
                        DeviceLock.confirm(context as Activity, context.getString(R.string.rs_title)) { ok ->
                            if (ok) step = 1 else err = context.getString(R.string.rs_lock_failed)
                        }
                    }
                }) { Text(stringResource(R.string.rs_face_button)) }
            } else Text(stringResource(R.string.rs_no_face), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            if (hasCode) Button(onClick = { err = null; input = ""; step = 2 }, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.rc_button)) }
            if (templates == null && !hasCode) Text(stringResource(R.string.rs_none), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
}
