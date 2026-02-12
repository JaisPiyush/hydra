package com.benzenelabs.hydra.host.data.secret

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey as AesSecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [SecretVault] implementation backed by Android Keystore (AES/GCM) and [SharedPreferences].
 *
 * Values are encrypted before persistence and encoded as:
 * `v1:<base64(iv)>:<base64(ciphertext)>`.
 */
class SecretVaultImpl(context: Context) : SecretVault {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun put(scope: StorageScope, key: SecretKey, value: String) =
        withContext(Dispatchers.IO) {
            runVaultOperation("Failed to store secret") {
                prefs.edit().putString(scopedKey(scope, key), encrypt(value)).apply()
            }
        }

    override suspend fun get(scope: StorageScope.Extension, key: SecretKey): String =
        withContext(Dispatchers.IO) {
            runVaultOperation("Failed to read secret") {
                prefs.getString(scopedKey(scope, key), null)?.let(::decrypt)
                    ?: throw SecretNotFoundException(scope, key)
            }
        }

    override suspend fun exists(scope: StorageScope, key: SecretKey): Boolean =
        withContext(Dispatchers.IO) {
            runVaultOperation("Failed to check secret existence") {
                prefs.contains(scopedKey(scope, key))
            }
        }

    override suspend fun delete(scope: StorageScope, key: SecretKey): Boolean =
        withContext(Dispatchers.IO) {
            runVaultOperation("Failed to delete secret") {
                val namespacedKey = scopedKey(scope, key)
                if (prefs.contains(namespacedKey)) {
                    prefs.edit().remove(namespacedKey).apply()
                    true
                } else {
                    false
                }
            }
        }

    override suspend fun deleteAll(extensionId: ContributionId) =
        withContext(Dispatchers.IO) {
            runVaultOperation("Failed to delete extension secrets") {
                val prefix = extensionPrefix(extensionId)
                val keysToDelete = prefs.all.keys.filter { it.startsWith(prefix) }
                if (keysToDelete.isNotEmpty()) {
                    prefs.edit().also { editor ->
                        keysToDelete.forEach(editor::remove)
                    }.apply()
                }
            }
        }

    override suspend fun listKeys(extensionId: ContributionId): List<SecretKey> =
        withContext(Dispatchers.IO) {
            runVaultOperation("Failed to list extension secret keys") {
                val prefix = extensionPrefix(extensionId)
                prefs.all.keys
                    .filter { it.startsWith(prefix) }
                    .map { key -> SecretKey(key.removePrefix(prefix)) }
            }
        }

    private fun scopedKey(scope: StorageScope, key: SecretKey): String = when (scope) {
        is StorageScope.Agent -> "agent:${key.value}"
        is StorageScope.Extension -> "ext:${scope.contributionId.value}:${key.value}"
    }

    private fun extensionPrefix(extensionId: ContributionId): String =
        "ext:${extensionId.value}:"

    private inline fun <T> runVaultOperation(message: String, block: () -> T): T =
        try {
            block()
        } catch (e: SecretNotFoundException) {
            throw e
        } catch (e: Exception) {
            throw SecretVaultException(message, e)
        }

    private fun keystore(): KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    @Synchronized
    private fun getOrCreateAesKey(): AesSecretKey {
        val ks = keystore()
        val existing = ks.getKey(KEY_ALIAS, null) as? AesSecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return buildString {
            append(PAYLOAD_VERSION)
            append(':')
            append(toBase64(iv))
            append(':')
            append(toBase64(ciphertext))
        }
    }

    private fun decrypt(payload: String): String {
        val parts = payload.split(':', limit = 3)
        require(parts.size == 3 && parts[0] == PAYLOAD_VERSION) { "Unsupported encrypted payload format" }
        val iv = fromBase64(parts[1])
        val ciphertext = fromBase64(parts[2])

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateAesKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun toBase64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun fromBase64(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        private const val PREFS_NAME = "hydra_secret_vault_v2"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "hydra_secret_vault_aes_key"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val PAYLOAD_VERSION = "v1"
    }
}
