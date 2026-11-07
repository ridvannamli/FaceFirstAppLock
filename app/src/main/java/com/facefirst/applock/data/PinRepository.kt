package com.facefirst.applock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64

private val Context.pinStore by preferencesDataStore("pin_store")

/** Zaman görüntüsü: duvar saati (kullanıcı değiştirebilir), tek yönlü süre sayacı ve açılış sayısı. */
data class TimeSnap(val wall: Long, val elapsed: Long, val boot: Int)
interface TimeSource { fun now(): TimeSnap }

class SystemTimeSource(private val context: Context) : TimeSource {
    override fun now() = TimeSnap(
        System.currentTimeMillis(),
        android.os.SystemClock.elapsedRealtime(),
        android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 0),
    )
}

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val failed: Int, val retryAfterMs: Long) : PinResult
    data class Locked(val retryAfterMs: Long) : PinResult
    data object NotSet : PinResult
}

class PinRepository(
    private val store: DataStore<Preferences>,
    private val pepper: Pepper,
    private val time: TimeSource,
    private val iterations: Int = PinCrypto.ITERATIONS,
) {
    private val kSalt = stringPreferencesKey("salt")
    private val kHash = stringPreferencesKey("hash")
    private val kLen = intPreferencesKey("len")
    private val kFailed = intPreferencesKey("failed")
    private val kUntil = longPreferencesKey("locked_until")
    private val kDur = longPreferencesKey("lock_dur")
    private val kElapsed = longPreferencesKey("lock_elapsed")
    private val kBoot = intPreferencesKey("lock_boot")

    val hasPin: Flow<Boolean> = store.data.map { it[kHash] != null }
    val pinLength: Flow<Int> = store.data.map { it[kLen] ?: 4 }

    /** PIN hiç düz metin saklanmaz/loglanmaz. 4 veya 6 hane, yalnızca rakam. */
    suspend fun setPin(pin: String, length: Int) {
        require(length == 4 || length == 6) { "PIN 4 veya 6 haneli olmalı" }
        require(pin.length == length && pin.all { it.isDigit() }) { "Geçersiz PIN" }
        val salt = PinCrypto.newSalt()
        val hash = PinCrypto.hash(pin, salt, pepper, iterations)
        val enc = Base64.getEncoder()
        store.edit {
            it[kSalt] = enc.encodeToString(salt); it[kHash] = enc.encodeToString(hash)
            it[kLen] = length; it[kFailed] = 0; it[kUntil] = 0; it[kDur] = 0
        }
    }

    /** Kalan bekleme: aynı açılışta tek yönlü sayaçla (saat oynamaları işe yaramaz); yeniden başlatmada duvar saatiyle. */
    private fun remaining(p: Preferences, now: TimeSnap): Long {
        val dur = p[kDur] ?: 0
        if (dur <= 0) return 0
        val rem = if (p[kBoot] == now.boot) dur - (now.elapsed - (p[kElapsed] ?: 0)) else (p[kUntil] ?: 0) - now.wall
        return maxOf(0, rem)
    }

    suspend fun verify(pin: String, now: TimeSnap = time.now()): PinResult {
        val p = store.data.first()
        val hashStr = p[kHash] ?: return PinResult.NotSet
        val rem = remaining(p, now)
        if (rem > 0) return PinResult.Locked(rem)
        val dec = Base64.getDecoder()
        val actual = PinCrypto.hash(pin, dec.decode(p[kSalt]!!), pepper, iterations)
        if (PinCrypto.equalConstantTime(actual, dec.decode(hashStr))) {
            store.edit { it[kFailed] = 0; it[kUntil] = 0; it[kDur] = 0 }
            return PinResult.Success
        }
        val failed = (p[kFailed] ?: 0) + 1
        val delay = Throttle.delayMs(failed)
        store.edit {
            it[kFailed] = failed
            it[kDur] = delay; it[kUntil] = if (delay > 0) now.wall + delay else 0
            it[kElapsed] = now.elapsed; it[kBoot] = now.boot
        }
        return PinResult.Wrong(failed, delay)
    }

    /** Eski PIN doğrulanırsa yenisini kaydeder. */
    suspend fun changePin(old: String, new: String, length: Int, now: TimeSnap = time.now()): PinResult {
        val r = verify(old, now)
        if (r is PinResult.Success) setPin(new, length)
        return r
    }

    companion object {
        fun create(context: Context) = PinRepository(context.applicationContext.pinStore, KeystorePepper(), SystemTimeSource(context.applicationContext))
    }
}
