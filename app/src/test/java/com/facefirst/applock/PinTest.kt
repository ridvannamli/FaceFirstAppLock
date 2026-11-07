package com.facefirst.applock

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.facefirst.applock.data.Pepper
import com.facefirst.applock.data.PinCrypto
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PinResult
import com.facefirst.applock.data.Throttle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PinTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun pepper(k: String = "k") = object : Pepper {
        override fun mac(data: ByteArray) = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(k.toByteArray(), "HmacSHA256")) }.doFinal(data)
    }
    private fun repo(p: Pepper = pepper(), name: String = "a") = PinRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + Job())) { tmp.newFile("$name.preferences_pb").also { it.delete() } },
        p, iterations = 1_000,
    )

    @Test fun throttleIncreases() {
        assertEquals(0, Throttle.delayMs(4)); assertEquals(30_000, Throttle.delayMs(5))
        assertTrue(Throttle.delayMs(8) > Throttle.delayMs(7)); assertTrue(Throttle.delayMs(20) >= Throttle.delayMs(9))
    }
    @Test fun hashDependsOnPinSaltAndPepper() {
        val s = ByteArray(16) { 1 }
        val h = PinCrypto.hash("1234", s, pepper(), 1_000)
        assertTrue(PinCrypto.equalConstantTime(h, PinCrypto.hash("1234", s, pepper(), 1_000)))
        assertFalse(PinCrypto.equalConstantTime(h, PinCrypto.hash("1235", s, pepper(), 1_000)))
        assertFalse(PinCrypto.equalConstantTime(h, PinCrypto.hash("1234", ByteArray(16) { 2 }, pepper(), 1_000)))
        assertFalse(PinCrypto.equalConstantTime(h, PinCrypto.hash("1234", s, pepper("other"), 1_000)))
    }
    @Test fun setAndVerify() = runBlocking {
        val r = repo(); assertFalse(r.hasPin.first()); assertEquals(PinResult.NotSet, r.verify("1234"))
        r.setPin("1234", 4); assertTrue(r.hasPin.first())
        assertEquals(PinResult.Success, r.verify("1234"))
        assertTrue(r.verify("0000") is PinResult.Wrong)
    }
    @Test fun lockoutAfterFiveWrongThenWaitsThenRecovers() = runBlocking {
        val r = repo(); r.setPin("123456", 6)
        var t = 1_000_000L
        repeat(4) { assertEquals(0, (r.verify("000000", t) as PinResult.Wrong).retryAfterMs) }
        assertEquals(30_000, (r.verify("000000", t) as PinResult.Wrong).retryAfterMs)
        assertTrue(r.verify("123456", t + 1_000) is PinResult.Locked) // doğru PIN bile bekleme süresince reddedilir
        t += 31_000
        assertEquals(PinResult.Success, r.verify("123456", t))
        assertEquals(0, (r.verify("000000", t) as PinResult.Wrong).retryAfterMs - 0 * 1) // sayaç sıfırlandı
    }
    @Test fun changePinNeedsOld() = runBlocking {
        val r = repo(); r.setPin("1111", 4)
        assertTrue(r.changePin("2222", "3333", 4) is PinResult.Wrong)
        assertEquals(PinResult.Success, r.verify("1111"))
        assertEquals(PinResult.Success, r.changePin("1111", "333333", 6))
        assertEquals(PinResult.Success, r.verify("333333")); assertEquals(6, r.pinLength.first())
    }
    @Test fun invalidPinRejected() {
        val r = repo()
        runCatching { runBlocking { r.setPin("12a4", 4) } }.let { assertTrue(it.isFailure) }
        runCatching { runBlocking { r.setPin("123", 4) } }.let { assertTrue(it.isFailure) }
    }
}
