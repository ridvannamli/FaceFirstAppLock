package com.facefirst.applock.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/** Telefonun kendi ekran kilidiyle (PIN/desen/şifre) onay. PIN sıfırlamada ikinci etken olarak kullanılır. */
object DeviceLock {
    fun isSecure(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    fun confirm(activity: Activity, title: String, onResult: (Boolean) -> Unit) {
        val b = BiometricPrompt.Builder(activity).setTitle(title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL) // yalnızca ekran kilidi
        } else {
            @Suppress("DEPRECATION") b.setDeviceCredentialAllowed(true) // Android 10: sistem biyometriyi de sunabilir
        }
        try {
            b.build().authenticate(CancellationSignal(), activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onResult(false)
            })
        } catch (e: Exception) {
            onResult(false)
        }
    }
}
