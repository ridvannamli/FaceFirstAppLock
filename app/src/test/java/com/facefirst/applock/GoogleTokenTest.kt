package com.facefirst.applock

import com.facefirst.applock.data.GoogleToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class GoogleTokenTest {
    private fun jwt(payload: String) = "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray()) + ".sig"
    private val ok = jwt("""{"iss":"https://accounts.google.com","aud":"client-1","sub":"1234567890","email":"ridvan@gmail.com","nonce":"n1","exp":2000,"iat":1000}""")

    @Test fun parsesClaims() {
        val c = GoogleToken.parse(ok)!!
        assertEquals("1234567890", c.sub); assertEquals("client-1", c.aud); assertEquals(2000L, c.exp); assertEquals("n1", c.nonce); assertEquals("ridvan@gmail.com", c.email)
    }
    @Test fun validWhenAudienceIssuerNonceAndExpiryMatch() = assertTrue(GoogleToken.valid(GoogleToken.parse(ok)!!, "client-1", "n1", 1500))
    @Test fun rejectsWrongAudience() = assertFalse(GoogleToken.valid(GoogleToken.parse(ok)!!, "other-app", "n1", 1500))
    @Test fun rejectsWrongNonce() = assertFalse(GoogleToken.valid(GoogleToken.parse(ok)!!, "client-1", "replayed", 1500))
    @Test fun rejectsExpired() = assertFalse(GoogleToken.valid(GoogleToken.parse(ok)!!, "client-1", "n1", 2001))
    @Test fun rejectsWrongIssuer() {
        val c = GoogleToken.parse(jwt("""{"iss":"https://evil.example","aud":"client-1","sub":"1","nonce":"n1","exp":2000}"""))!!
        assertFalse(GoogleToken.valid(c, "client-1", "n1", 1500))
    }
    @Test fun rejectsEmptyClientId() = assertFalse(GoogleToken.valid(GoogleToken.parse(ok)!!.copy(aud = ""), "", "n1", 1500))
    @Test fun malformedTokenIsNull() {
        assertNull(GoogleToken.parse("not-a-jwt")); assertNull(GoogleToken.parse("a.b.c")); assertNull(GoogleToken.parse(jwt("""{"aud":"x"}""")))
    }
    @Test fun masksEmail() {
        assertEquals("r***@gmail.com", GoogleToken.maskEmail("ridvan@gmail.com")); assertEquals("Google hesabı", GoogleToken.maskEmail(null))
    }
}
