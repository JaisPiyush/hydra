package com.benzenelabs.hydra.host.data.secret

import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.contributions.api.ContributionId

/**
 * Encrypted key-value store for sensitive values.
 */
interface SecretVault {

    /** Stores or updates a secret for [scope]. */
    suspend fun put(scope: StorageScope, key: SecretKey, value: String)

    /**
     * Reads a secret value for an extension scope.
     * Agent reads are prevented by the method signature.
     */
    suspend fun get(scope: StorageScope.Extension, key: SecretKey): String

    /** Returns true if a secret exists for [scope] and [key]. */
    suspend fun exists(scope: StorageScope, key: SecretKey): Boolean

    /** Deletes a secret for [scope] and [key], returning true if it existed. */
    suspend fun delete(scope: StorageScope, key: SecretKey): Boolean

    /** Deletes all secrets owned by [extensionId]. */
    suspend fun deleteAll(extensionId: ContributionId)

    /** Returns all secret keys owned by [extensionId] (keys only). */
    suspend fun listKeys(extensionId: ContributionId): List<SecretKey>
}

/** Thrown when a secret is not found for the given scope and key. */
class SecretNotFoundException(scope: StorageScope, key: SecretKey) :
    Exception("Secret not found: scope=${scope.label}, key=${key.value}")

/** Wraps underlying storage or encryption failures. */
class SecretVaultException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
