package com.facefirst.applock.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Cihaza bağlı gizli anahtarla (Android Keystore) MAC üretir; anahtar dışarı çıkmaz. */
interface Pepper { fun mac(data: ByteArray): ByteArray }

object PinCrypto {
    const val ITERATIONS = 200_000

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    /** PBKDF2-HMAC-SHA256(PIN, salt) sonra Keystore anahtarıyla HMAC: hash çevrimdışı tek başına kırılamaz. */
    fun hash(pin: String, salt: ByteArray, pepper: Pepper, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        try {
            val dk = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return pepper.mac(dk)
        } finally {
            spec.clearPassword()
        }
    }

    fun equalConstantTime(a: ByteArray, b: ByteArray) = MessageDigest.isEqual(a, b)
}

/** Hatalı denemelerde artan bekleme (ms). İlk 4 hata ücretsiz. */
object Throttle {
    fun delayMs(failed: Int): Long = when {
        failed < 5 -> 0
        failed == 5 -> 30_000
        failed == 6 -> 60_000
        failed == 7 -> 5 * 60_000
        failed == 8 -> 15 * 60_000
        else -> 30 * 60_000
    }
}

/** Kurtarma kodu: 12 karakter (60 bit), karışabilecek harfler yok (I, O, 0, 1). Biçim: XXXX-XXXX-XXXX. */
object RecoveryCode {
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun generate(random: java.util.Random = SecureRandom()): String =
        (0 until 12).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("").chunked(4).joinToString("-")
    /** Kullanıcı girişini karşılaştırma biçimine getirir (büyük harf, ayraçsız). */
    fun normalize(input: String): String = input.uppercase().filter { it in ALPHABET }
    fun wellFormed(input: String): Boolean = normalize(input).length == 12
}
