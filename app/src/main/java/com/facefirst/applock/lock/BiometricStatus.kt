package com.facefirst.applock.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt

/** Cihazın üçüncü taraf BiometricPrompt'a sunduğu biyometrik durum (yüz/parmak izi ayrımı yapılmaz). */
enum class BiometricStatus {
    STRONG,          // Sınıf 3: yüz veya parmak izi, güçlü
    WEAK_ONLY,       // Sınıf 2: yalnızca zayıf biyometrik kullanılabilir
    NOT_ENROLLED,    // Donanım var, kayıtlı biyometrik yok
    NO_HARDWARE,
    UNAVAILABLE;     // geçici veya desteklenmiyor

    companion object {
        fun from(strongCode: Int, weakCode: Int): BiometricStatus = when {
            strongCode == BiometricManager.BIOMETRIC_SUCCESS -> STRONG
            weakCode == BiometricManager.BIOMETRIC_SUCCESS -> WEAK_ONLY
            weakCode == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> NOT_ENROLLED
            weakCode == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> NO_HARDWARE
            else -> UNAVAILABLE
        }

        fun check(context: Context): BiometricStatus {
            val m = BiometricManager.from(context)
            return from(m.canAuthenticate(BIOMETRIC_STRONG), m.canAuthenticate(BIOMETRIC_WEAK))
        }
    }

    val usable get() = this == STRONG || this == WEAK_ONLY
}

/** Hata koduna göre kilit ekranının yapacağı şey. */
enum class ErrorAction { GO_HOME, SHOW_FALLBACK }

fun errorAction(code: Int): ErrorAction = when (code) {
    BiometricPrompt.ERROR_USER_CANCELED -> ErrorAction.GO_HOME
    else -> ErrorAction.SHOW_FALLBACK // negatif düğme, kilitlenme, donanım hatası, vb. → PIN'e düş
}

/** Biyometrik art arda bu kadar tanınmazsa istem kapatılır ve PIN ekranına geçilir. */
const val MAX_BIO_FAILS = 2
fun shouldFallBack(failures: Int, limit: Int = MAX_BIO_FAILS) = failures >= limit
