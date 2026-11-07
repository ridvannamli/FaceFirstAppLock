package com.facefirst.applock

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.data.PolicyRepository
import com.facefirst.applock.data.RecoveryClient
import com.facefirst.applock.lock.BiometricStatus
import com.facefirst.applock.lock.LockAccessibilityService
import com.facefirst.applock.lock.LockRuntime
import com.facefirst.applock.ui.AppsScreen
import com.facefirst.applock.ui.EmailFlow
import com.facefirst.applock.ui.PinFlow
import com.facefirst.applock.ui.PinGate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val repo by lazy { PinRepository.create(this) }
    private val policies by lazy { PolicyRepository(this) }
    private val recovery by lazy { RecoveryClient(this) }
    private var protectionOn by mutableStateOf(false)
    private var changing by mutableStateOf(false)
    private var emailStep by mutableStateOf(0)     // 0 yok, 1 PIN kapısı, 2 e-posta akışı
    private var disableStep by mutableStateOf(false) // genel korumayı kapatmadan önce PIN kapısı
    private var skipRecovery by mutableStateOf(false)
    private var authed by mutableStateOf(false)    // uygulamanın kendi ayarları da korunur
    private var prompting = false
    private var bio by mutableStateOf(BiometricStatus.UNAVAILABLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) // ekran görüntüsü/son uygulamalar önizlemesi engellenir
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.safeDrawingPadding()) {
                    val hasPin by repo.hasPin.collectAsState(initial = null)
                    val len by repo.pinLength.collectAsState(initial = 4)
                    val masked by recovery.maskedEmail.collectAsState(initial = "…")
                    when {
                        hasPin == null || masked == "…" -> {}
                        hasPin == false -> PinFlow(
                            requireOld = false, verifyOld = { PinResult.NotSet },
                            save = { pin, l, _ -> repo.setPin(pin, l); authed = true }, onCancel = null,
                        )
                        !authed -> PinGate(repo, len, onSuccess = { authed = true }, onCancel = { finish() })
                        changing -> PinFlow(
                            requireOld = true, oldLength = len, verifyOld = { repo.verify(it) },
                            save = { pin, l, old -> repo.changePin(old.orEmpty(), pin, l); changing = false },
                            onCancel = { changing = false },
                        )
                        disableStep -> PinGate(repo, len, onSuccess = {
                            disableStep = false
                            lifecycleScope.launch { policies.setEnabled(false); LockRuntime.machine?.resetSessions() }
                        }, onCancel = { disableStep = false })
                        emailStep == 1 -> PinGate(repo, len, onSuccess = { emailStep = 2 }, onCancel = { emailStep = 0 })
                        emailStep == 2 || (masked.isNullOrBlank() && !skipRecovery && recovery.enabled) -> EmailFlow(
                            recovery, onDone = { emailStep = 0 },
                            onSkip = if (emailStep == 2) null else ({ skipRecovery = true }),
                        )
                        else -> AppsScreen(
                            recoveryMasked = masked?.takeIf { it.isNotBlank() },
                            recoveryEnabled = recovery.enabled,
                            onChangeEmail = { emailStep = if (masked.isNullOrBlank()) 2 else 1 },
                            protectionOn = protectionOn,
                            biometricStatus = bio,
                            onOpenA11ySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            onChangePin = { changing = true },
                            onRequestDisable = { disableStep = true },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        protectionOn = isServiceEnabled()
        bio = BiometricStatus.check(this)
        if (!authed && !prompting && bio.usable) lifecycleScope.launch { if (repo.hasPin.first()) promptBiometric() }
    }

    /** Uygulamadan çıkılınca (son uygulamalar, başka uygulama) kendi ayarlarımız yeniden doğrulama ister. */
    override fun onStop() {
        super.onStop()
        authed = false
    }

    private fun promptBiometric() {
        prompting = true
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { prompting = false; authed = true }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { prompting = false } // PIN kapısı görünür kalır
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.locked_title))
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .setConfirmationRequired(false)
                .setNegativeButtonText(getString(R.string.use_pin)).build()
        )
    }

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, LockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.split(':')?.any { it.equals(me, ignoreCase = true) } == true
    }
}
