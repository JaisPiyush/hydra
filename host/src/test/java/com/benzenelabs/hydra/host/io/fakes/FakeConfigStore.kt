package com.benzenelabs.hydra.host.io.fakes

import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.config.ConfigField
import com.benzenelabs.hydra.host.data.config.ConfigStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeConfigStore : ConfigStore {

    private val values = mutableMapOf<Pair<StorageScope, String>, String>()

    override suspend fun get(scope: StorageScope, field: ConfigField): String? {
        return values[scope to field.name]
    }

    override suspend fun set(scope: StorageScope, field: ConfigField, value: String) {
        values[scope to field.name] = value
    }

    override suspend fun delete(scope: StorageScope, field: ConfigField) {
        values.remove(scope to field.name)
    }

    override suspend fun getAll(scope: StorageScope): Map<String, String?> {
        // Simple scan, not efficient but fine for fakes
        return values.filterKeys { it.first == scope }.mapKeys { it.key.second }
    }

    override suspend fun deleteAll(scope: StorageScope) {
        val keysToRemove = values.keys.filter { it.first == scope }
        keysToRemove.forEach { values.remove(it) }
    }

    override fun observe(scope: StorageScope, field: ConfigField): Flow<String?> {
        // Simple observation using state flow for test purposes if needed
        return MutableStateFlow(values[scope to field.name])
    }

    // Helper to preload data
    fun put(scope: StorageScope, key: String, value: String) {
        values[scope to key] = value
    }
}
