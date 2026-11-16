package com.facefirst.applock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64

data class IdClaims(val sub: String, val aud: String, val iss: String, val exp: Long, val nonce: String?, val email: String?)

/** Google kimlik belirteci (JWT) çözümü ve doğrulama kuralları. Saf mantık, birim testli. */
object GoogleToken {
    fun parse(jwt: String): IdClaims? = runCatching {
        val parts = jwt.split('.')
        require(parts.size == 3)
        val json = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        IdClaims(str(json, "sub")!!, str(json, "aud")!!, str(json, "iss")!!, num(json, "exp")!!, str(json, "nonce"), str(json, "email"))
    }.getOrNull()

    private fun str(json: String, key: String): String? =
        Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(json)?.groupValues?.get(1)

    private fun num(json: String, key: String): Long? =
        Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()

    /** Belirteç bu uygulama için mi, Google'dan mı, süresi geçmemiş mi ve bu istek için mi (nonce)? */
    fun valid(c: IdClaims, clientId: String, nonce: String, nowSec: Long): Boolean =
        clientId.isNotBlank() && c.aud == clientId &&
            (c.iss == "accounts.google.com" || c.iss == "https://accounts.google.com") &&
            c.exp > nowSec && c.nonce == nonce && c.sub.isNotBlank()

    fun maskEmail(e: String?): String {
        val at = e?.indexOf('@') ?: -1
        return if (e == null || at < 1) "Google hesabı" else "${e[0]}***${e.substring(at)}"
    }
}

private val Context.googleStore by preferencesDataStore("google_link")

/** Bağlı Google hesabının kimliği (sub) ve maskeli e-postası; yalnızca cihazda durur. */
class GoogleLinkStore(context: Context) {
    private val store = context.applicationContext.googleStore
    private val kSub = stringPreferencesKey("sub")
    private val kMasked = stringPreferencesKey("masked")
    val sub: Flow<String?> = store.data.map { it[kSub] }
    val masked: Flow<String?> = store.data.map { it[kMasked] }
    suspend fun save(c: IdClaims) { store.edit { it[kSub] = c.sub; it[kMasked] = GoogleToken.maskEmail(c.email) } }
    suspend fun clear() { store.edit { it.clear() } }
}
