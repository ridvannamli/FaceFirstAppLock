package com.facefirst.applock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.facefirst.applock.R
import com.facefirst.applock.data.PinResult
import kotlinx.coroutines.launch

private enum class Step { OLD, LENGTH, NEW, CONFIRM }

/**
 * PIN oluşturma (requireOld=false) veya değiştirme (requireOld=true) akışı.
 * verifyOld: mevcut PIN denemesi; save: yeni PIN'i kaydeder. PIN hiçbir yerde loglanmaz.
 */
@Composable
fun PinFlow(
    requireOld: Boolean,
    verifyOld: suspend (String) -> PinResult,
    save: suspend (pin: String, length: Int, old: String?) -> Unit,
    onCancel: (() -> Unit)?,
    oldLength: Int = 4,
) {
    var step by remember { mutableStateOf(if (requireOld) Step.OLD else Step.LENGTH) }
    var length by remember { mutableIntStateOf(4) }
    var first by remember { mutableStateOf("") }
    var old by remember { mutableStateOf<String?>(null) }
    var resetKey by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<Int?>(null) }
    var wait by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(
                when (step) {
                    Step.OLD -> R.string.pin_enter_current
                    Step.LENGTH -> R.string.pin_choose_length
                    Step.NEW -> R.string.pin_enter_new
                    Step.CONFIRM -> R.string.pin_confirm
                }
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        msg?.let {
            Text(
                if (it == R.string.pin_locked) stringResource(it, wait / 1000 + 1) else stringResource(it),
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp),
            )
        }
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                Step.LENGTH -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { length = 4; step = Step.NEW }) { Text(stringResource(R.string.pin_4)) }
                    OutlinedButton(onClick = { length = 6; step = Step.NEW }) { Text(stringResource(R.string.pin_6)) }
                }
                Step.OLD -> PinPad(oldLength, resetKey, true) { pin ->
                    scope.launch {
                        when (val r = verifyOld(pin)) {
                            is PinResult.Success -> { old = pin; msg = null; step = Step.LENGTH }
                            is PinResult.Wrong -> { wait = r.retryAfterMs; msg = if (r.retryAfterMs > 0) R.string.pin_locked else R.string.pin_wrong; resetKey++ }
                            is PinResult.Locked -> { wait = r.retryAfterMs; msg = R.string.pin_locked; resetKey++ }
                            PinResult.NotSet -> step = Step.LENGTH
                        }
                    }
                }
                Step.NEW -> PinPad(length, resetKey, true) { pin -> first = pin; msg = null; resetKey++; step = Step.CONFIRM }
                Step.CONFIRM -> PinPad(length, resetKey, true) { pin ->
                    if (pin == first) scope.launch { save(pin, length, old) }
                    else { msg = R.string.pin_mismatch; first = ""; resetKey++; step = Step.NEW }
                }
            }
        }
        onCancel?.let { TextButton(onClick = it) { Text(stringResource(R.string.cancel)) } }
    }
}
