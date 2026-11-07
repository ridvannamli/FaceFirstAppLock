package com.facefirst.applock

import com.facefirst.applock.data.RecoveryError
import com.facefirst.applock.data.RecoveryProtocol
import com.facefirst.applock.data.Signer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import java.util.Base64

class RecoveryProtocolTest {
    private val kp = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val signer = object : Signer {
        override fun sign(data: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(kp.private); update(data); sign() }
        override fun publicKeySpki() = kp.public.encoded
    }

    @Test fun bodyHasDeviceTsAndFields() {
        val r = RecoveryProtocol.build("/v1/reset/start", "dev", 123L, mapOf("email" to "a@b.co"), signer)
        assertEquals("""{"deviceId":"dev","ts":123,"email":"a@b.co"}""", r.body)
    }
    @Test fun jsonEscapesQuotesAndControlChars() {
        assertEquals("""{"k":"a\"b\\c\u000a"}""", RecoveryProtocol.toJson(mapOf("k" to "a\"b\\c\n")))
    }
    @Test fun signatureVerifiesOverPathNewlineBody() {
        val r = RecoveryProtocol.build("/v1/email/verify", "dev", 1L, mapOf("code" to "123456"), signer)
        val pub = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(signer.publicKeySpki()))
        val ok = Signature.getInstance("SHA256withECDSA").run {
            initVerify(pub); update("${r.path}\n${r.body}".toByteArray()); verify(Base64.getDecoder().decode(r.signatureB64))
        }
        assertTrue(ok)
    }
    @Test fun serverErrorsMapped() {
        assertEquals(RecoveryError.WRONG_CODE, RecoveryError.fromServer("wrong_code"))
        assertEquals(RecoveryError.RATE_LIMITED, RecoveryError.fromServer("rate_limited"))
        assertEquals(RecoveryError.OTHER, RecoveryError.fromServer("???"))
        assertEquals(RecoveryError.OTHER, RecoveryError.fromServer(null))
    }
}
