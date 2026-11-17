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
import com.facefirst.applock.face.FaceStore
import com.facefirst.applock.lock.AppDeviceAdmin
import com.facefirst.applock.lock.LockAccessibilityService
import com.facefirst.applock.lock.LockRuntime
import com.facefirst.applock.ui.AppsScreen
import com.facefirst.applock.ui.AuthScreen
import com.facefirst.applock.ui.RecoveryCodeScreen
import com.facefirst.applock.ui.ResetFlow
import com.facefirst.applock.ui.FaceEnroll
import com.facefirst.applock.ui.PinFlow
import com.facefirst.applock.ui.PinGate
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repo by lazy { PinRepository.create(this) }
    private val policies by lazy { PolicyRepository(this) }
    private val faceStore by lazy { FaceStore(this) }
    private var protectionOn by mutableStateOf(false)
    private var changing by mutableStateOf(false)
    private var codeStep by mutableStateOf(false)    // yeni kurtarma kodu öncesi PIN kapısı
    private var shownCode by mutableStateOf<String?>(null) // bir kez gösterilecek kurtarma kodu
    private var enrollStep by mutableStateOf(0)      // 0 yok, 1 PIN kapısı, 2 yüz kaydı
    private var disableStep by mutableStateOf(false) // genel korumayı kapatmadan önce PIN kapısı
    private var resetting by mutableStateOf(false)    // giriş ekranından "PIN'imi unuttum"
    private var guardOffStep by mutableStateOf(false) // kaldırma korumasını kapatmadan önce PIN kapısı
    private var adminOffStep by mutableStateOf(false) // cihaz yöneticisini kapatmadan önce PIN kapısı
    private var adminOn by mutableStateOf(false)
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
                    val hasCode by repo.hasRecovery.collectAsState(initial = null)
                    val challenge by policies.faceChallenge.collectAsState(initial = null)
                    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                        if (ok) enrollStep = 1
                    }
                    val ch = challenge
                    val guard by policies.uninstallGuard.collectAsState(initial = false)
                    when {
                        hasPin == null || hasCode == null || ch == null -> {}
                        hasPin == false -> PinFlow(
                            requireOld = false, verifyOld = { PinResult.NotSet },
                            save = { pin, l, _ -> repo.setPin(pin, l); shownCode = repo.newRecoveryCode(); authed = true }, onCancel = null,
                        )
                        resetting -> ResetFlow(
                            repo = repo, onCancel = { resetting = false },
                            onFinished = {
                                resetting = false; changing = false; disableStep = false; guardOffStep = false; adminOffStep = false; enrollStep = 0; codeStep = false
                                LockRuntime.machine?.resetSessions(); authed = true
                            },
                        )
                        !authed -> key(resumes) {
                            AuthScreen(
                                pinRepo = repo, title = stringResource(R.string.app_name), challengeOn = ch,
                                onSuccess = { authed = true }, onCancel = { finish() }, onForgot = { resetting = true },
                            )
                        }
                        shownCode != null -> RecoveryCodeScreen(shownCode.orEmpty()) { shownCode = null }
                        changing -> PinFlow(
                            requireOld = true, oldLength = len, verifyOld = { repo.verify(it) },
                            save = { pin, l, old -> repo.changePin(old.orEmpty(), pin, l); changing = false },
                            onCancel = { changing = false }, onForgot = { resetting = true },
                        )
                        disableStep -> PinGate(repo, len, onSuccess = {
                            disableStep = false
                            lifecycleScope.launch { policies.setEnabled(false); LockRuntime.machine?.resetSessions() }
                        }, onCancel = { disableStep = false }, onForgot = { resetting = true })
                        adminOffStep -> PinGate(repo, len, onSuccess = {
                            adminOffStep = false
                            AppDeviceAdmin.disable(this); adminOn = AppDeviceAdmin.isActive(this)
                        }, onCancel = { adminOffStep = false }, onForgot = { resetting = true })
                        guardOffStep -> PinGate(repo, len, onSuccess = {
                            guardOffStep = false
                            lifecycleScope.launch { policies.setUninstallGuard(false) }
                        }, onCancel = { guardOffStep = false }, onForgot = { resetting = true })
                        // Yüz kaydı/değişimi her zaman PIN ister (yüzle yüz değiştirilemez).
                        enrollStep == 1 -> PinGate(repo, len, onSuccess = { enrollStep = 2 }, onCancel = { enrollStep = 0 }, onForgot = { resetting = true })
                        enrollStep == 2 -> FaceEnroll(
                            onDone = { list -> faceStore.save(list); faceEnrolled = true; enrollStep = 0 },
                            onCancel = { enrollStep = 0 },
                        )
                        codeStep -> PinGate(repo, len, onSuccess = {
                            codeStep = false
                            lifecycleScope.launch { shownCode = repo.newRecoveryCode() }
                        }, onCancel = { codeStep = false }, onForgot = { resetting = true })
                        else -> AppsScreen(
                            hasRecoveryCode = hasCode == true,
                            onNewCode = { codeStep = true },
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
                            guardOn = guard,
                            adminOn = adminOn,
                            onToggleAdmin = { on -> if (on) startActivity(AppDeviceAdmin.enableIntent(this)) else adminOffStep = true },
                            onToggleGuard = { on -> if (on) lifecycleScope.launch { policies.setUninstallGuard(true) } else guardOffStep = true },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        protectionOn = isServiceEnabled()
        adminOn = AppDeviceAdmin.isActive(this)
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
