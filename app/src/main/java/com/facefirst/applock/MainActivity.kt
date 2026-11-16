package com.facefirst.applock

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.data.PolicyRepository
import com.facefirst.applock.data.GoogleAuth
import com.facefirst.applock.data.GoogleLinkStore
import com.facefirst.applock.face.FaceStore
import com.facefirst.applock.lock.LockAccessibilityService
import com.facefirst.applock.lock.LockRuntime
import com.facefirst.applock.ui.AppsScreen
import com.facefirst.applock.ui.AuthScreen
import com.facefirst.applock.ui.GoogleLinkFlow
import com.facefirst.applock.ui.FaceEnroll
import com.facefirst.applock.ui.PinFlow
import com.facefirst.applock.ui.PinGate
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repo by lazy { PinRepository.create(this) }
    private val policies by lazy { PolicyRepository(this) }
    private val gAuth = GoogleAuth()
    private val gStore by lazy { GoogleLinkStore(this) }
    private val faceStore by lazy { FaceStore(this) }
    private var protectionOn by mutableStateOf(false)
    private var changing by mutableStateOf(false)
    private var emailStep by mutableStateOf(0)       // 0 yok, 1 PIN kapısı, 2 Google hesabı bağlama
    private var enrollStep by mutableStateOf(0)      // 0 yok, 1 PIN kapısı, 2 yüz kaydı
    private var disableStep by mutableStateOf(false) // genel korumayı kapatmadan önce PIN kapısı
    private var skipRecovery by mutableStateOf(false)
    private var authed by mutableStateOf(false)      // uygulamanın kendi ayarları da korunur
    private var resumes by mutableIntStateOf(0)
    private var faceEnrolled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) // ekran görüntüsü/son uygulamalar önizlemesi engellenir
        faceEnrolled = faceStore.exists()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.safeDrawingPadding()) {
                    val hasPin by repo.hasPin.collectAsState(initial = null)
                    val len by repo.pinLength.collectAsState(initial = 4)
                    val masked by gStore.masked.collectAsState(initial = "…")
                    val challenge by policies.faceChallenge.collectAsState(initial = null)
                    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                        if (ok) enrollStep = 1
                    }
                    val ch = challenge
                    when {
                        hasPin == null || masked == "…" || ch == null -> {}
                        hasPin == false -> PinFlow(
                            requireOld = false, verifyOld = { PinResult.NotSet },
                            save = { pin, l, _ -> repo.setPin(pin, l); authed = true }, onCancel = null,
                        )
                        !authed -> key(resumes) {
                            AuthScreen(
                                pinRepo = repo, title = stringResource(R.string.app_name), challengeOn = ch,
                                onSuccess = { authed = true }, onCancel = { finish() }, onForgot = null,
                            )
                        }
                        changing -> PinFlow(
                            requireOld = true, oldLength = len, verifyOld = { repo.verify(it) },
                            save = { pin, l, old -> repo.changePin(old.orEmpty(), pin, l); changing = false },
                            onCancel = { changing = false },
                        )
                        disableStep -> PinGate(repo, len, onSuccess = {
                            disableStep = false
                            lifecycleScope.launch { policies.setEnabled(false); LockRuntime.machine?.resetSessions() }
                        }, onCancel = { disableStep = false })
                        // Yüz kaydı/değişimi her zaman PIN ister (yüzle yüz değiştirilemez).
                        enrollStep == 1 -> PinGate(repo, len, onSuccess = { enrollStep = 2 }, onCancel = { enrollStep = 0 })
                        enrollStep == 2 -> FaceEnroll(
                            onDone = { list -> faceStore.save(list); faceEnrolled = true; enrollStep = 0 },
                            onCancel = { enrollStep = 0 },
                        )
                        emailStep == 1 -> PinGate(repo, len, onSuccess = { emailStep = 2 }, onCancel = { emailStep = 0 })
                        emailStep == 2 || (masked.isNullOrBlank() && !skipRecovery && gAuth.enabled) -> GoogleLinkFlow(
                            gAuth, gStore, onDone = { emailStep = 0 },
                            onSkip = { if (emailStep == 2) emailStep = 0 else skipRecovery = true },
                        )
                        else -> AppsScreen(
                            recoveryMasked = masked?.takeIf { it.isNotBlank() },
                            recoveryEnabled = gAuth.enabled,
                            onChangeEmail = { emailStep = if (masked.isNullOrBlank()) 2 else 1 },
                            protectionOn = protectionOn,
                            faceEnrolled = faceEnrolled,
                            challengeOn = ch,
                            onEnrollFace = {
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) enrollStep = 1
                                else cameraPermission.launch(Manifest.permission.CAMERA)
                            },
                            onDeleteFace = { faceStore.delete(); faceEnrolled = false },
                            onToggleChallenge = { on -> lifecycleScope.launch { policies.setFaceChallenge(on) } },
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
        resumes++
    }

    /** Uygulamadan çıkılınca (son uygulamalar, başka uygulama) kendi ayarlarımız yeniden doğrulama ister. */
    override fun onStop() {
        super.onStop()
        authed = false
    }

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, LockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.split(':')?.any { it.equals(me, ignoreCase = true) } == true
    }
}
