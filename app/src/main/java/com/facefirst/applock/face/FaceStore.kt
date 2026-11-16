package com.facefirst.applock.face

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Yüz şablonları: fotoğraf SAKLANMAZ; yalnızca sayısal şablonlar, Android Keystore anahtarıyla
 * (AES-GCM) şifreli olarak uygulamanın özel dizininde durur. Cihazdan çıkmaz, yedeğe girmez.
 */
class FaceStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "face_tpl.bin")

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build()
            )
        }.generateKey()
    }

    fun exists(): Boolean = file.exists()

    fun save(templates: List<FloatArray>) {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val ct = c.doFinal(FaceMath.encodeTemplates(templates))
        file.writeBytes(byteArrayOf(c.iv.size.toByte()) + c.iv + ct)
    }

    /** Bozuk ya da çözülemeyen kayıt yok sayılır (yüzle açma kapanır, PIN kalır). */
    fun load(): List<FloatArray>? = runCatching {
        if (!file.exists()) return null
        val all = file.readBytes()
        val n = all[0].toInt()
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all, 1, n)) }
        FaceMath.decodeTemplates(c.doFinal(all, 1 + n, all.size - 1 - n)).takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun delete() { file.delete() }

    private companion object { const val ALIAS = "face_tpl_v1" }
}
