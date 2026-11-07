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

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val failed: Int, val retryAfterMs: Long) : PinResult
    data class Locked(val retryAfterMs: Long) : PinResult
    data object NotSet : PinResult
}

class PinRepository(
    private val store: DataStore<Preferences>,
    private val pepper: Pepper,
    private val iterations: Int = PinCrypto.ITERATIONS,
) {
    private val kSalt = stringPreferencesKey("salt")
    private val kHash = stringPreferencesKey("hash")
    private val kLen = intPreferencesKey("len")
    private val kFailed = intPreferencesKey("failed")
    private val kUntil = longPreferencesKey("locked_until")

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
            it[kLen] = length; it[kFailed] = 0; it[kUntil] = 0
        }
    }

    suspend fun verify(pin: String, now: Long = System.currentTimeMillis()): PinResult {
        val p = store.data.first()
        val hashStr = p[kHash] ?: return PinResult.NotSet
        val until = p[kUntil] ?: 0
        if (now < until) return PinResult.Locked(until - now)
        val dec = Base64.getDecoder()
        val actual = PinCrypto.hash(pin, dec.decode(p[kSalt]!!), pepper, iterations)
        if (PinCrypto.equalConstantTime(actual, dec.decode(hashStr))) {
            store.edit { it[kFailed] = 0; it[kUntil] = 0 }
            return PinResult.Success
        }
        val failed = (p[kFailed] ?: 0) + 1
        val delay = Throttle.delayMs(failed)
        store.edit { it[kFailed] = failed; it[kUntil] = if (delay > 0) now + delay else 0 }
        return PinResult.Wrong(failed, delay)
    }

    /** Eski PIN doğrulanırsa yenisini kaydeder. */
    suspend fun changePin(old: String, new: String, length: Int, now: Long = System.currentTimeMillis()): PinResult {
        val r = verify(old, now)
        if (r is PinResult.Success) setPin(new, length)
        return r
    }

    companion object {
        fun create(context: Context) = PinRepository(context.applicationContext.pinStore, KeystorePepper())
    }
}
