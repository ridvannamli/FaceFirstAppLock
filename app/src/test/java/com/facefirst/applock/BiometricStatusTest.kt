package com.facefirst.applock

import androidx.biometric.BiometricManager as BM
import androidx.biometric.BiometricPrompt
import com.facefirst.applock.lock.BiometricStatus
import com.facefirst.applock.lock.ErrorAction
import com.facefirst.applock.lock.errorAction
import com.facefirst.applock.lock.shouldFallBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricStatusTest {
    private val ok = BM.BIOMETRIC_SUCCESS
    @Test fun strongWins() = assertEquals(BiometricStatus.STRONG, BiometricStatus.from(ok, ok))
    @Test fun weakOnly() = assertEquals(BiometricStatus.WEAK_ONLY, BiometricStatus.from(BM.BIOMETRIC_ERROR_NONE_ENROLLED, ok))
    @Test fun notEnrolled() = assertEquals(BiometricStatus.NOT_ENROLLED, BiometricStatus.from(BM.BIOMETRIC_ERROR_NONE_ENROLLED, BM.BIOMETRIC_ERROR_NONE_ENROLLED))
    @Test fun noHardware() = assertEquals(BiometricStatus.NO_HARDWARE, BiometricStatus.from(BM.BIOMETRIC_ERROR_NO_HARDWARE, BM.BIOMETRIC_ERROR_NO_HARDWARE))
    @Test fun hwUnavailable() = assertEquals(BiometricStatus.UNAVAILABLE, BiometricStatus.from(BM.BIOMETRIC_ERROR_HW_UNAVAILABLE, BM.BIOMETRIC_ERROR_HW_UNAVAILABLE))
    @Test fun usableOnlyWhenReady() {
        assertTrue(BiometricStatus.STRONG.usable); assertTrue(BiometricStatus.WEAK_ONLY.usable)
        assertFalse(BiometricStatus.NOT_ENROLLED.usable); assertFalse(BiometricStatus.UNAVAILABLE.usable)
    }
    @Test fun userCancelGoesHome() = assertEquals(ErrorAction.GO_HOME, errorAction(BiometricPrompt.ERROR_USER_CANCELED))
    @Test fun negativeButtonAndLockoutFallBack() {
        assertEquals(ErrorAction.SHOW_FALLBACK, errorAction(BiometricPrompt.ERROR_NEGATIVE_BUTTON))
        assertEquals(ErrorAction.SHOW_FALLBACK, errorAction(BiometricPrompt.ERROR_LOCKOUT))
        assertEquals(ErrorAction.SHOW_FALLBACK, errorAction(BiometricPrompt.ERROR_LOCKOUT_PERMANENT))
    }
    @Test fun fallsBackToPinAfterRepeatedFailures() {
        assertFalse(shouldFallBack(0)); assertFalse(shouldFallBack(1)); assertTrue(shouldFallBack(2)); assertTrue(shouldFallBack(5))
    }
    @Test fun canceledAndTimeoutShowPin() {
        assertEquals(ErrorAction.SHOW_FALLBACK, errorAction(BiometricPrompt.ERROR_CANCELED))
        assertEquals(ErrorAction.SHOW_FALLBACK, errorAction(BiometricPrompt.ERROR_TIMEOUT))
        assertEquals(ErrorAction.SHOW_FALLBACK, errorAction(BiometricPrompt.ERROR_HW_UNAVAILABLE))
    }
}
