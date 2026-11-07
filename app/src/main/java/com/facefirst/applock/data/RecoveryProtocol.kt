package com.facefirst.applock.data

import java.util.Base64

/** İmzalayıcı: Android'de Keystore ECDSA P-256; testlerde sahte. */
interface Signer {
    fun sign(data: ByteArray): ByteArray
    fun publicKeySpki(): ByteArray
}

data class SignedRequest(val path: String, val body: String, val signatureB64: String)

object RecoveryProtocol {
    /** Sunucuyla aynı sözleşme: imzalanan metin = "$path\n$body". */
    fun build(path: String, deviceId: String, now: Long, fields: Map<String, Any>, signer: Signer): SignedRequest {
        val all = linkedMapOf<String, Any>("deviceId" to deviceId, "ts" to now).apply { putAll(fields) }
        val body = toJson(all)
        val sig = signer.sign("$path\n$body".toByteArray())
        return SignedRequest(path, body, Base64.getEncoder().encodeToString(sig))
    }

    fun toJson(m: Map<String, Any>): String = m.entries.joinToString(",", "{", "}") { (k, v) ->
        "\"${esc(k)}\":" + if (v is Number) v.toString() else "\"${esc(v.toString())}\""
    }

    private fun esc(s: String) = buildString {
        for (c in s) when {
            c == '"' -> append("\\\""); c == '\\' -> append("\\\\")
            c < ' ' -> append("\\u%04x".format(c.code)); else -> append(c)
        }
    }

    fun publicKeyB64(signer: Signer): String = Base64.getEncoder().encodeToString(signer.publicKeySpki())
}

enum class RecoveryError { DISABLED, NETWORK, RATE_LIMITED, WRONG_CODE, NO_CODE, TOO_MANY_ATTEMPTS, BAD_EMAIL, NO_RECOVERY_EMAIL, OTHER;
    companion object {
        fun fromServer(code: String?): RecoveryError = when (code) {
            "rate_limited" -> RATE_LIMITED; "wrong_code" -> WRONG_CODE; "no_code" -> NO_CODE
            "too_many_attempts" -> TOO_MANY_ATTEMPTS; "bad_email" -> BAD_EMAIL
            "no_recovery_email" -> NO_RECOVERY_EMAIL; else -> OTHER
        }
    }
}

sealed interface RecoveryResult {
    data class Ok(val masked: String? = null) : RecoveryResult
    data class Fail(val error: RecoveryError) : RecoveryResult
}
