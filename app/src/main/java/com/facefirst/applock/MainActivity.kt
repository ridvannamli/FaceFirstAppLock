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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.facefirst.applock.lock.BiometricStatus
import com.facefirst.applock.lock.LockAccessibilityService
import com.facefirst.applock.ui.AppsScreen

class MainActivity : ComponentActivity() {
    private var protectionOn by mutableStateOf(false)
    private var bio by mutableStateOf(BiometricStatus.UNAVAILABLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface {
                    AppsScreen(
                        protectionOn = protectionOn,
                        biometricStatus = bio,
                        onOpenA11ySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    )
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
