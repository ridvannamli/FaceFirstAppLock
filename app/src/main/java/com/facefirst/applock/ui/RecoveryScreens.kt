package com.facefirst.applock.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.facefirst.applock.R
import com.facefirst.applock.data.GoogleAuth
import com.facefirst.applock.data.GoogleLinkStore
import com.facefirst.applock.data.GoogleResult
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

/** PIN kurtarma için Google hesabı bağlama. Telefonun ekran kilidi yoksa bağlanamaz (sıfırlamada ikinci etken). */
@Composable
fun GoogleLinkFlow(auth: GoogleAuth, store: GoogleLinkStore, onDone: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val secure = remember { DeviceLock.isSecure(context) }
    Page {
        Text(stringResource(R.string.g_link_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.g_link_info), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
        if (!secure) Text(stringResource(R.string.g_need_lock), color = MaterialTheme.colorScheme.error)
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (busy) CircularProgressIndicator() else Button(enabled = secure, onClick = {
            busy = true; err = null
            scope.launch {
                when (val r = auth.signIn(context)) {
                    is GoogleResult.Ok -> { store.save(r.claims); onDone() }
                    GoogleResult.Cancelled -> {}
                    GoogleResult.Disabled -> err = context.getString(R.string.g_disabled)
                    is GoogleResult.Error -> err = context.getString(R.string.g_error, r.detail)
                }
                busy = false
            }
        }) { Text(stringResource(R.string.g_link_button)) }
        TextButton(onClick = onSkip) { Text(stringResource(R.string.g_skip)) }
    }
}

/**
 * PIN'imi unuttum: (1) telefonun ekran kilidi, (2) bağlı Google hesabıyla giriş, (3) yüz kayıtlıysa yüz doğrulama
 * (rastgele hareketli), sonra yeni PIN. Üçü de geçilmeden PIN değişmez.
 */
@Composable
fun GoogleResetFlow(auth: GoogleAuth, store: GoogleLinkStore, repo: PinRepository, onFinished: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val linkedSub by store.sub.collectAsState(initial = null)
    val masked by store.masked.collectAsState(initial = null)
    var step by remember { mutableIntStateOf(0) } // 0 başlangıç, 1 yüz, 2 yeni PIN
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var templates by remember { mutableStateOf<List<FloatArray>?>(null) }
    val challenge = remember { Challenge.entries.random() }

    when (step) {
        2 -> PinFlow(
            requireOld = false, verifyOld = { PinResult.NotSet },
            save = { pin, len, _ -> repo.setPin(pin, len); onFinished() }, onCancel = onCancel,
        )
        1 -> Page {
            Text(stringResource(R.string.g_face_step), style = MaterialTheme.typography.titleLarge)
            FaceCheck(templates.orEmpty(), challenge) { ok ->
                if (ok) step = 2 else { err = context.getString(R.string.g_face_failed); step = 0 }
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
        else -> Page {
            Text(stringResource(R.string.g_reset_title), style = MaterialTheme.typography.titleLarge)
            val sub = linkedSub
            if (!auth.enabled || sub == null) {
                Text(stringResource(R.string.g_not_linked), Modifier.padding(8.dp))
            } else {
                Text(stringResource(R.string.g_reset_info, masked ?: ""), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) CircularProgressIndicator() else Button(onClick = {
                    err = null
                    if (!DeviceLock.isSecure(context)) { err = context.getString(R.string.g_need_lock); return@Button }
                    busy = true
                    DeviceLock.confirm(context as Activity, context.getString(R.string.g_reset_title)) { ok ->
                        if (!ok) { busy = false; err = context.getString(R.string.g_lock_failed); return@confirm }
                        scope.launch {
                            when (val r = auth.signIn(context)) {
                                is GoogleResult.Ok -> if (r.claims.sub == sub) {
                                    val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                    templates = if (hasCam) withContext(Dispatchers.IO) { FaceStore(context).load() } else null
                                    step = if (templates != null) 1 else 2
                                } else err = context.getString(R.string.g_wrong_account)
                                GoogleResult.Cancelled -> {}
                                GoogleResult.Disabled -> err = context.getString(R.string.g_disabled)
                                is GoogleResult.Error -> err = context.getString(R.string.g_error, r.detail)
                            }
                            busy = false
                        }
                    }
                }) { Text(stringResource(R.string.g_reset_button)) }
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
}
