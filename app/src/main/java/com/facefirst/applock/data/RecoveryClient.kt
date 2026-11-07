package com.facefirst.applock.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.facefirst.applock.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

private val Context.recoveryStore by preferencesDataStore("recovery_store")
private val K_DEVICE = stringPreferencesKey("device_id")
private val K_MASKED = stringPreferencesKey("masked_email")

class KeystoreSigner(private val alias: String = "recovery_ec_v1") : Signer {
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun ensureKey() {
        if (keyStore().containsAlias(alias)) return
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).build()
            )
        }.generateKeyPair()
    }
    override fun sign(data: ByteArray): ByteArray {
        ensureKey()
        return Signature.getInstance("SHA256withECDSA").run {
            initSign(keyStore().getKey(alias, null) as PrivateKey); update(data); sign()
        }
    }
    override fun publicKeySpki(): ByteArray { ensureKey(); return keyStore().getCertificate(alias).publicKey.encoded }
}

/** PIN kurtarma sunucusu istemcisi. Hiçbir gizli anahtar içermez; istekler cihaz anahtarıyla imzalanır. */
class RecoveryClient(
    context: Context,
    private val baseUrl: String = BuildConfig.RECOVERY_URL,
    private val signer: Signer = KeystoreSigner(),
) {
    private val store = context.applicationContext.recoveryStore
    val maskedEmail: Flow<String?> = store.data.map { it[K_MASKED] }
    val enabled get() = baseUrl.isNotBlank()

    private suspend fun deviceId(): String {
        store.data.first()[K_DEVICE]?.let { return it }
        val id = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(24).also { SecureRandom().nextBytes(it) })
        store.edit { it[K_DEVICE] = id }
        return id
    }

    private suspend fun call(path: String, fields: Map<String, Any>): RecoveryResult {
        if (!enabled) return RecoveryResult.Fail(RecoveryError.DISABLED)
        return withContext(Dispatchers.IO) {
            try {
                val req = RecoveryProtocol.build(path, deviceId(), System.currentTimeMillis(), fields, signer)
                val c = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 15_000; doOutput = true
                    setRequestProperty("content-type", "application/json"); setRequestProperty("x-sig", req.signatureB64)
                }
                c.outputStream.use { it.write(req.body.toByteArray()) }
                val text = (if (c.responseCode in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()
                val j = runCatching { JSONObject(text) }.getOrNull()
                if (c.responseCode in 200..299 && j?.optBoolean("ok") == true) RecoveryResult.Ok(j.optString("masked").ifEmpty { null })
                else RecoveryResult.Fail(RecoveryError.fromServer(j?.optString("error")))
            } catch (e: java.io.IOException) { RecoveryResult.Fail(RecoveryError.NETWORK) }
            catch (e: Exception) { RecoveryResult.Fail(RecoveryError.OTHER) }
        }
    }

    suspend fun startEmail(email: String) =
        call("/v1/email/start", mapOf("email" to email, "publicKey" to RecoveryProtocol.publicKeyB64(signer)))
    suspend fun verifyEmail(code: String): RecoveryResult {
        val r = call("/v1/email/verify", mapOf("code" to code))
        if (r is RecoveryResult.Ok) store.edit { it[K_MASKED] = r.masked ?: "" }
        return r
    }
    suspend fun startReset() = call("/v1/reset/start", emptyMap())
    suspend fun verifyReset(code: String) = call("/v1/reset/verify", mapOf("code" to code))
    suspend fun resetDone() = call("/v1/reset/done", emptyMap())
}
