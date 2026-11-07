package com.facefirst.applock.lock

import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.facefirst.applock.R
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.ui.PinPad
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Önce biyometrik (BiometricPrompt; yüz/parmak izi, sistemin sunduğu), olmazsa uygulama PIN'i.
 */
class UnlockActivity : FragmentActivity() {
    private lateinit var target: String
    private var showFallback by mutableStateOf(false)
    private var prompting = false
    private var pinMsg by mutableStateOf<Int?>(null)
    private var wait by mutableStateOf(0L)
    private var resetKey by mutableStateOf(0)
    private var pinLen by mutableStateOf(4)
    private val repo by lazy { PinRepository.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra(EXTRA_PACKAGE) ?: run { goHome(); return }
        lifecycleScope.launch { pinLen = repo.pinLength.first() }
        setContent {
            BackHandler { goHome() }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.locked_title), style = MaterialTheme.typography.titleLarge)
                        if (showFallback) {
                            pinMsg?.let {
                                Text(
                                    if (it == R.string.pin_locked) stringResource(it, wait / 1000 + 1) else stringResource(it),
                                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp),
                                )
                            }
                            Column(Modifier.padding(vertical = 16.dp)) {
                                PinPad(pinLen, resetKey, enabled = wait <= 0L) { pin -> checkPin(pin) }
                            }
                            if (BiometricStatus.check(this@UnlockActivity).usable) {
                                Button(onClick = { showFallback = false; prompt() }) { Text(stringResource(R.string.retry_biometric)) }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!showFallback) prompt()
    }

    private fun prompt() {
        if (prompting) return
        if (!BiometricStatus.check(this).usable) { showFallback = true; return }
        prompting = true
        val bp = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                prompting = false; unlock()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                prompting = false
                if (errorAction(errorCode) == ErrorAction.GO_HOME) goHome() else showFallback = true
            }
        })
        bp.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.locked_title))
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .setNegativeButtonText(getString(R.string.use_pin))
                .build()
        )
    }

    private fun checkPin(pin: String) {
        lifecycleScope.launch {
            when (val r = repo.verify(pin)) {
                is PinResult.Success -> unlock()
                is PinResult.Wrong -> { pinMsg = if (r.retryAfterMs > 0) R.string.pin_locked else R.string.pin_wrong; startWait(r.retryAfterMs) }
                is PinResult.Locked -> { pinMsg = R.string.pin_locked; startWait(r.retryAfterMs) }
                PinResult.NotSet -> goHome()
            }
            resetKey++
        }
    }

    private fun startWait(ms: Long) {
        wait = ms
        if (ms <= 0) return
        lifecycleScope.launch {
            while (wait > 0) { delay(1000); wait -= 1000 }
            wait = 0; pinMsg = null
        }
    }

    private fun unlock() {
        LockRuntime.machine?.onUnlocked(target)
        packageManager.getLaunchIntentForPackage(target)?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object { const val EXTRA_PACKAGE = "target_package" }
}
