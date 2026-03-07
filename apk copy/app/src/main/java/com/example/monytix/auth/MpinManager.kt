package com.example.monytix.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

object MpinManager {

    private const val PREFS_NAME = "mpin_secure_prefs"
    private const val KEY_MPIN_HASH = "mpin_hash"
    private const val KEY_MPIN_SALT = "mpin_salt"
    private const val KEY_MPIN_SET = "mpin_set"
    private const val SALT_BYTES = 32
    private const val HASH_ALGORITHM = "SHA-256"

    private fun getPrefs(context: Context): SharedPreferences {
        return getPrefsOrClearAndRetry(context, clearOnFailure = false)
    }

    /**
     * Create EncryptedSharedPreferences. If decryption fails (e.g. AEADBadTagException after
     * app reinstall or Keystore change), clear the prefs file and retry once so the app
     * doesn't crash and the user can set MPIN again.
     */
    private fun getPrefsOrClearAndRetry(context: Context, clearOnFailure: Boolean): SharedPreferences {
        if (clearOnFailure) {
            try {
                context.deleteSharedPreferences(PREFS_NAME)
            } catch (e: Exception) {
                Log.w("MpinManager", "Could not delete corrupted prefs", e)
            }
        }
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            val isCorrupted = e is AEADBadTagException
                || e is javax.crypto.BadPaddingException
                || e is android.security.KeyStoreException
                || e.cause is AEADBadTagException
            if (isCorrupted && !clearOnFailure) {
                Log.w("MpinManager", "EncryptedSharedPreferences decryption failed (Keystore/prefs corrupted), clearing and retrying", e)
                getPrefsOrClearAndRetry(context, clearOnFailure = true)
            } else {
                throw e
            }
        }
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
