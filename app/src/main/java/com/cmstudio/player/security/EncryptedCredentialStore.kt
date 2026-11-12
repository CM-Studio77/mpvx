package com.cmstudio.player.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore (AES-256-GCM) store for sensitive network share passwords
 * and media-server access tokens. Never stored inside `mpvx_player_prefs` or exported in JSON backups.
 */
object EncryptedCredentialStore {

    private const val PREF_FILE = "mpvx_secure_credentials"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "mpvx_cred_aes_gcm_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LEN_BYTES = 12
    private const val TAG_LEN_BITS = 128
    private const val PREFIX_ENC = "enc_v1:"

    private val lock = Any()
    private val inMemoryFallback = linkedMapOf<String, String>()

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun putSecret(context: Context?, key: String, secret: String) {
        val cleanKey = key.trim()
        if (cleanKey.isEmpty()) return
        synchronized(lock) {
            if (secret.isEmpty()) {
                inMemoryFallback.remove(cleanKey)
                context?.let { prefs(it).edit().remove(cleanKey).apply() }
                return
            }
            inMemoryFallback[cleanKey] = secret
            if (context == null) return
            val encrypted = encryptString(secret) ?: return
            prefs(context).edit().putString(cleanKey, encrypted).apply()
        }
    }

    fun getSecret(context: Context?, key: String): String {
        val cleanKey = key.trim()
        if (cleanKey.isEmpty()) return ""
        synchronized(lock) {
            inMemoryFallback[cleanKey]?.let { return it }
            if (context == null) return ""
            val raw = prefs(context).getString(cleanKey, null).orEmpty()
            if (raw.isEmpty()) return ""
            val decrypted = decryptString(raw).orEmpty()
            if (decrypted.isNotEmpty()) {
                inMemoryFallback[cleanKey] = decrypted
            }
            return decrypted
        }
    }

    fun removeSecret(context: Context?, key: String) {
        val cleanKey = key.trim()
        if (cleanKey.isEmpty()) return
        synchronized(lock) {
            inMemoryFallback.remove(cleanKey)
            context?.let { prefs(it).edit().remove(cleanKey).apply() }
        }
    }

    fun clearAll(context: Context?) {
        synchronized(lock) {
            inMemoryFallback.clear()
            context?.let { prefs(it).edit().clear().apply() }
        }
    }

    private fun getOrCreateKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            gen.init(spec)
            gen.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    private fun encryptString(plain: String): String? {
        val key = getOrCreateKey() ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            if (iv.size != IV_LEN_BYTES) return null
            val ciphertext = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            PREFIX_ENC + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decryptString(encoded: String): String? {
        if (!encoded.startsWith(PREFIX_ENC)) return null
        val key = getOrCreateKey() ?: return null
        return try {
            val raw = Base64.decode(encoded.removePrefix(PREFIX_ENC), Base64.NO_WRAP)
            if (raw.size <= IV_LEN_BYTES) return null
            val iv = raw.copyOfRange(0, IV_LEN_BYTES)
            val ciphertext = raw.copyOfRange(IV_LEN_BYTES, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LEN_BITS, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }
}
