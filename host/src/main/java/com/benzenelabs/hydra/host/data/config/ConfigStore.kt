package com.benzenelabs.hydra.host.data.config

import com.benzenelabs.hydra.host.data.StorageScope
import kotlinx.coroutines.flow.Flow

/**
 * Scoped key-value configuration store.
 */
interface ConfigStore {

    /** Sets or updates a config value for [scope] under [field]. */
    suspend fun set(scope: StorageScope, field: ConfigField, value: String)

    /** Returns the value for [field] under [scope], or null if not set. */
    suspend fun get(scope: StorageScope, field: ConfigField): String?

    /** Deletes the stored value for [field] under [scope]. */
    suspend fun delete(scope: StorageScope, field: ConfigField)

    /** Returns all config entries for [scope] as a snapshot. */
    suspend fun getAll(scope: StorageScope): Map<String, String?>

    /** Returns a [Flow] that emits the current value and re-emits on change. */
    fun observe(scope: StorageScope, field: ConfigField): Flow<String?>

    /** Deletes all config for [scope]. */
    suspend fun deleteAll(scope: StorageScope)
}

/** Thrown when [StorageScope.Agent] attempts to read a secret config field. */
class ConfigSecretAccessDeniedException(fieldName: String) :
    SecurityException("Agent may not read secret config field: '$fieldName'")

/** Wraps underlying storage failures. */
class ConfigException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
