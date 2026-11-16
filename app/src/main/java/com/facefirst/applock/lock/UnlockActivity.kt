package com.facefirst.applock.lock

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
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
import com.facefirst.applock.BuildConfig
import com.facefirst.applock.R
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PolicyRepository
import com.facefirst.applock.data.GoogleAuth
import com.facefirst.applock.data.GoogleLinkStore
import com.facefirst.applock.ui.AuthScreen
import com.facefirst.applock.ui.GoogleResetFlow

/** Kilit ekranı: önce uygulama içi yüz tanıma, olmazsa uygulama PIN'i. Sistem biyometrisi/parmak izi kullanılmaz. */
class UnlockActivity : ComponentActivity() {
    private var target by mutableStateOf("")
    private var resumes by mutableIntStateOf(0)
    private var resetting by mutableStateOf(false)
    private val repo by lazy { PinRepository.create(this) }
    private val gAuth = GoogleAuth()
    private val gStore by lazy { GoogleLinkStore(this) }
    private val policies by lazy { PolicyRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        target = intent.getStringExtra(EXTRA_PACKAGE) ?: run { goHome(); return }
        setContent {
            BackHandler { if (resetting) resetting = false else goHome() }
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    val challenge by policies.faceChallenge.collectAsState(initial = null)
                    val ch = challenge
                    if (resetting) GoogleResetFlow(
                        auth = gAuth, store = gStore, repo = repo, onCancel = { resetting = false },
                        onFinished = { LockRuntime.machine?.onScreenOff(); unlock() }, // önceki oturumlar sıfırlanır
                    ) else if (ch != null) key(target, resumes) {
                        AuthScreen(
                            pinRepo = repo, title = stringResource(R.string.locked_title), challengeOn = ch,
                            onSuccess = { unlock() }, onCancel = { goHome() }, onForgot = { resetting = true },
                        )
                    }
                }
            }
        }
    }

    /** singleTask: kilit ekranı açıkken başka bir kilitli uygulamaya geçilirse hedef güncellenir. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_PACKAGE)?.let { target = it }
    }

    /** Her öne gelişte doğrulama baştan başlar (kamera oturumu yenilenir). */
    override fun onResume() {
        super.onResume()
        resumes++
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
