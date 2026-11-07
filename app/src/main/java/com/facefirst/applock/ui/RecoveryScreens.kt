package com.facefirst.applock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.facefirst.applock.R
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.data.RecoveryClient
import com.facefirst.applock.data.RecoveryError
import com.facefirst.applock.data.RecoveryResult
import kotlinx.coroutines.launch

fun errorRes(e: RecoveryError): Int = when (e) {
    RecoveryError.DISABLED -> R.string.rec_disabled
    RecoveryError.NETWORK -> R.string.rec_network
    RecoveryError.RATE_LIMITED -> R.string.rec_rate
    RecoveryError.WRONG_CODE -> R.string.rec_wrong_code
    RecoveryError.NO_CODE -> R.string.rec_no_code
    RecoveryError.TOO_MANY_ATTEMPTS -> R.string.rec_too_many
    RecoveryError.BAD_EMAIL -> R.string.rec_bad_email
    RecoveryError.NO_RECOVERY_EMAIL -> R.string.rec_none
    RecoveryError.OTHER -> R.string.rec_other
}

@Composable
private fun Page(content: @Composable () -> Unit) = Column(
    Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) { content() }

@Composable
fun CodeEntry(masked: String?, error: Int?, busy: Boolean, onSubmit: (String) -> Unit, onResend: (() -> Unit)?, onCancel: (() -> Unit)?) {
    var code by remember { mutableStateOf("") }
    Text(stringResource(R.string.rec_code_title), style = MaterialTheme.typography.titleLarge)
    masked?.let { Text(stringResource(R.string.rec_code_sent, it), Modifier.padding(8.dp)) }
    OutlinedTextField(
        value = code, onValueChange = { v -> code = v.filter(Char::isDigit).take(6) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        label = { Text(stringResource(R.string.rec_code_hint)) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    if (busy) CircularProgressIndicator() else Button(onClick = { onSubmit(code) }, enabled = code.length == 6) { Text(stringResource(R.string.rec_verify)) }
    onResend?.let { TextButton(onClick = it, enabled = !busy) { Text(stringResource(R.string.rec_resend)) } }
    onCancel?.let { TextButton(onClick = it) { Text(stringResource(R.string.cancel)) } }
}

/** Kurtarma e-postası kaydı: e-posta gir → kod gönder → kodu doğrula. Doğrulanmadan etkinleşmez. */
@Composable
fun EmailFlow(client: RecoveryClient, onDone: () -> Unit, onSkip: (() -> Unit)?) {
    var step by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var masked by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun send() = scope.launch {
        busy = true; err = null
        when (val r = client.startEmail(email.trim())) {
            is RecoveryResult.Ok -> { masked = r.masked; step = 1 }
            is RecoveryResult.Fail -> err = errorRes(r.error)
        }
        busy = false
    }

    Page {
        if (step == 0) {
            Text(stringResource(R.string.rec_email_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.rec_email_info), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = email, onValueChange = { email = it }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                label = { Text(stringResource(R.string.rec_email_hint)) }, modifier = Modifier.fillMaxWidth(),
            )
            err?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            if (busy) CircularProgressIndicator() else Button(onClick = { send() }, enabled = email.contains('@')) { Text(stringResource(R.string.rec_send_code)) }
            onSkip?.let { TextButton(onClick = it) { Text(stringResource(R.string.rec_skip)) } }
        } else {
            CodeEntry(
                masked, err, busy,
                onSubmit = { c -> scope.launch {
                    busy = true; err = null
                    when (val r = client.verifyEmail(c)) { is RecoveryResult.Ok -> onDone(); is RecoveryResult.Fail -> err = errorRes(r.error) }
                    busy = false
                } },
                onResend = { send() }, onCancel = { step = 0; err = null },
            )
        }
    }
}

/** Hassas işlem öncesi mevcut PIN doğrulaması (ör. kurtarma e-postasını değiştirmek). */
@Composable
fun PinGate(repo: PinRepository, length: Int, onSuccess: () -> Unit, onCancel: () -> Unit) {
    var resetKey by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<Int?>(null) }
    var wait by remember { mutableStateOf(0L) }
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

/** PIN'imi unuttum: e-postaya kod → sunucu doğrulaması → yeni PIN → bildirim e-postası. */
@Composable
fun ResetFlow(client: RecoveryClient, repo: PinRepository, onFinished: () -> Unit, onCancel: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }  // 0 gönderiliyor, 1 kod, 2 yeni PIN
    var masked by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(attempt) {
        busy = true; err = null
        when (val r = client.startReset()) {
            is RecoveryResult.Ok -> { masked = r.masked; step = 1 }
            is RecoveryResult.Fail -> err = errorRes(r.error)
        }
        busy = false
    }

    when (step) {
        2 -> PinFlow(
            requireOld = false, verifyOld = { PinResult.NotSet },
            save = { pin, len, _ -> repo.setPin(pin, len); client.resetDone(); onFinished() },
            onCancel = onCancel,
        )
        else -> Page {
            if (step == 0) {
                if (busy) CircularProgressIndicator() else {
                    err?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { attempt++ }) { Text(stringResource(R.string.rec_retry)) }
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                }
            } else CodeEntry(
                masked, err, busy,
                onSubmit = { c -> scope.launch {
                    busy = true; err = null
                    when (val r = client.verifyReset(c)) { is RecoveryResult.Ok -> step = 2; is RecoveryResult.Fail -> err = errorRes(r.error) }
                    busy = false
                } },
                onResend = { attempt++ }, onCancel = onCancel,
            )
        }
    }
}
