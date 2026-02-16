package com.example.monytix.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

object MpinManager {

    private const val PREFS_NAME = "mpin_secure_prefs"
    private const val KEY_MPIN_HASH = "mpin_hash"
    private const val KEY_MPIN_SALT = "mpin_salt"
    private const val KEY_MPIN_SET = "mpin_set"
    private const val SALT_BYTES = 32
    private const val HASH_ALGORITHM = "SHA-256"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun hashWithSalt(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
    }

    fun isMpinSet(context: Context): Boolean {
        return try {
            getPrefs(context).getBoolean(KEY_MPIN_SET, false)
        } catch (e: Exception) {
            false
        }
    }

    fun setMpin(context: Context, pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hashWithSalt(pin, salt)
        getPrefs(context).edit()
            .putString(KEY_MPIN_HASH, hash)
            .putString(KEY_MPIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putBoolean(KEY_MPIN_SET, true)
            .apply()
    }

    fun verifyMpin(context: Context, pin: String): Boolean {
        return try {
            val prefs = getPrefs(context)
            val storedHash = prefs.getString(KEY_MPIN_HASH, null) ?: return false
            val storedSalt = prefs.getString(KEY_MPIN_SALT, null)
            if (storedSalt == null) {
                // Legacy: migration from hashCode-based storage; require re-set
                return false
            }
            val salt = Base64.decode(storedSalt, Base64.DEFAULT)
            hashWithSalt(pin, salt) == storedHash
        } catch (e: Exception) {
            false
        }
    }
}
