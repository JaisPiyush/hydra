package com.benzenelabs.hydra.host.data.config

import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.secret.SecretNotFoundException
import com.benzenelabs.hydra.host.data.secret.SecretVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * [ConfigStore] backed by Room for non-secret values and [SecretVault] for secret values.
 */
class ConfigStoreImpl(
    private val dao: ConfigEntryDao,
    private val secretVault: SecretVault
) : ConfigStore {

    override suspend fun set(scope: StorageScope, field: ConfigField, value: String) {
        withContext(Dispatchers.IO) {
            runConfigOperation("Failed to set config '${field.name}'") {
                if (field.isSecret) {
                    enforceAgentSecretReadWriteDenied(scope, field.name)
                    secretVault.put(scope, field.secretKey, value)
                }
                dao.upsert(
                    ConfigEntryEntity(
                        scopeLabel = scope.label,
                        key = field.name,
                        value = if (field.isSecret) null else value,
                        isSecret = field.isSecret,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override suspend fun get(scope: StorageScope, field: ConfigField): String? =
        withContext(Dispatchers.IO) {
            runConfigOperation("Failed to read config '${field.name}'") {
                if (field.isSecret) {
                    enforceAgentSecretReadWriteDenied(scope, field.name)
                    val extensionScope = scope as StorageScope.Extension
                    val entry = dao.find(scope.label, field.name)
                    if (entry == null) {
                        field.defaultValue
                    } else {
                        secretVault.get(extensionScope, field.secretKey)
                    }
                } else {
                    dao.find(scope.label, field.name)?.value ?: field.defaultValue
                }
            }
        }

    override suspend fun delete(scope: StorageScope, field: ConfigField) {
        withContext(Dispatchers.IO) {
            runConfigOperation("Failed to delete config '${field.name}'") {
                dao.delete(scope.label, field.name)
                if (field.isSecret && scope is StorageScope.Extension) {
                    secretVault.delete(scope, field.secretKey)
                }
            }
        }
    }

    override suspend fun getAll(scope: StorageScope): Map<String, String?> =
        withContext(Dispatchers.IO) {
            runConfigOperation("Failed to list config for scope ${scope.label}") {
                dao.findAll(scope.label).associate { entity ->
                    entity.key to if (entity.isSecret) null else entity.value
                }
            }
        }

    override fun observe(scope: StorageScope, field: ConfigField): Flow<String?> =
        dao.observe(scope.label, field.name).map { entity ->
            if (field.isSecret) {
                when (scope) {
                    is StorageScope.Agent -> null
                    is StorageScope.Extension -> {
                        if (entity == null) {
                            field.defaultValue
                        } else {
                            try {
                                secretVault.get(scope, field.secretKey)
                            } catch (_: SecretNotFoundException) {
                                null
                            }
                        }
                    }
                }
            } else {
                entity?.value ?: field.defaultValue
            }
        }

    override suspend fun deleteAll(scope: StorageScope) {
        withContext(Dispatchers.IO) {
            runConfigOperation("Failed to delete all config for scope ${scope.label}") {
                dao.deleteAll(scope.label)
                if (scope is StorageScope.Extension) {
                    secretVault.deleteAll(scope.contributionId)
                }
            }
        }
    }

    private fun enforceAgentSecretReadWriteDenied(scope: StorageScope, fieldName: String) {
        if (scope is StorageScope.Agent) {
            throw ConfigSecretAccessDeniedException(fieldName)
        }
    }

    private suspend inline fun <T> runConfigOperation(
        message: String,
        crossinline block: suspend () -> T
    ): T =
        try {
            block()
        } catch (e: ConfigSecretAccessDeniedException) {
            throw e
        } catch (e: Exception) {
            throw ConfigException(message, e)
        }
}
