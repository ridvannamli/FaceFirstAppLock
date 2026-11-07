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
import com.facefirst.applock.R

/**
 * Önce biyometrik (BiometricPrompt; yüz/parmak izi, sistemin sunduğu). Yedek ekran (FAZ 5'e kadar)
 * geçici bir test düğmesidir ve PIN gelince kaldırılacak; o zamana dek gerçek koruma SAĞLAMAZ.
 */
class UnlockActivity : FragmentActivity() {
    private lateinit var target: String
    private var showFallback by mutableStateOf(false)
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra(EXTRA_PACKAGE) ?: run { goHome(); return }
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
                            Text(stringResource(R.string.stub_note), Modifier.padding(vertical = 16.dp))
                            Button(onClick = { unlock() }) { Text(stringResource(R.string.stub_unlock)) }
                            Button(onClick = { showFallback = false; prompt() }) { Text(stringResource(R.string.retry_biometric)) }
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
