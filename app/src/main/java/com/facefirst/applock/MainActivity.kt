package com.facefirst.applock

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.facefirst.applock.lock.BiometricStatus
import com.facefirst.applock.lock.LockAccessibilityService
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.ui.AppsScreen
import com.facefirst.applock.ui.PinFlow

class MainActivity : ComponentActivity() {
    private val repo by lazy { PinRepository.create(this) }
    private var protectionOn by mutableStateOf(false)
    private var changing by mutableStateOf(false)
    private var bio by mutableStateOf(BiometricStatus.UNAVAILABLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface {
                    val hasPin by repo.hasPin.collectAsState(initial = null)
                    val len by repo.pinLength.collectAsState(initial = 4)
                    when {
                        hasPin == null -> {}
                        hasPin == false -> PinFlow(
                            requireOld = false, verifyOld = { PinResult.NotSet },
                            save = { pin, l, _ -> repo.setPin(pin, l) }, onCancel = null,
                        )
                        changing -> PinFlow(
                            requireOld = true, oldLength = len, verifyOld = { repo.verify(it) },
                            save = { pin, l, old -> repo.changePin(old.orEmpty(), pin, l); changing = false },
                            onCancel = { changing = false },
                        )
                        else -> AppsScreen(
                            protectionOn = protectionOn,
                            biometricStatus = bio,
                            onOpenA11ySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            onChangePin = { changing = true },
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
    }

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, LockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.split(':')?.any { it.equals(me, ignoreCase = true) } == true
    }
}
