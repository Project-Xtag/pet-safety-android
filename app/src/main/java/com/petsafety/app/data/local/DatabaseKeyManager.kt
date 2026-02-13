package com.petsafety.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher database encryption passphrase.
 * The passphrase is randomly generated and encrypted using Android Keystore.
 */
class DatabaseKeyManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "petsafety_db_key"
        private const val PREFS_NAME = "petsafety_db_prefs"
        private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val GCM_TAG_LENGTH = 128
    }

    fun getOrCreatePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)

        if (existing != null) {
            return decrypt(existing)
        }

        // Generate a new random passphrase
        val passphrase = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val encrypted = encrypt(passphrase)
        prefs.edit().putString(KEY_ENCRYPTED_PASSPHRASE, encrypted).apply()
        return passphrase
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        keyStore.getEntry(KEYSTORE_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun encrypt(data: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        // Store IV + encrypted data together
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): ByteArray {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.sliceArray(0 until 12) // GCM IV is 12 bytes
        val encrypted = combined.sliceArray(12 until combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }
}
