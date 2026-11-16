package com.facefirst.applock.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.facefirst.applock.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom
import java.util.Base64

sealed interface GoogleResult {
    data class Ok(val claims: IdClaims) : GoogleResult
    data object Cancelled : GoogleResult
    data object Disabled : GoogleResult
    data class Error(val detail: String) : GoogleResult
}

/**
 * "Google ile giriş" (Credential Manager). İstemci kimliği gizli değildir; derlemede -PgoogleClientId ile verilir.
 * Belirtecin imzası cihazda ayrıca doğrulanmaz: belirteç doğrudan Google Play Hizmetleri'nden alınır ve
 * hedef kitle/yayıncı/süre/nonce kontrol edilir (bkz. docs/SECURITY.md).
 */
class GoogleAuth(private val clientId: String = BuildConfig.GOOGLE_CLIENT_ID) {
    val enabled get() = clientId.isNotBlank()

    suspend fun signIn(activityContext: Context): GoogleResult {
        if (!enabled) return GoogleResult.Disabled
        val nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(24).also { SecureRandom().nextBytes(it) })
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).setNonce(nonce).build())
            .build()
        return try {
            val cred = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val claims = GoogleToken.parse(GoogleIdTokenCredential.createFrom(cred.data).idToken)
                if (claims != null && GoogleToken.valid(claims, clientId, nonce, System.currentTimeMillis() / 1000)) GoogleResult.Ok(claims)
                else GoogleResult.Error("belirteç geçersiz")
            } else GoogleResult.Error("beklenmeyen kimlik türü")
        } catch (e: GetCredentialCancellationException) {
            GoogleResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleResult.Error(e.type)
        } catch (e: Exception) {
            GoogleResult.Error(e.javaClass.simpleName)
        }
    }
}
