package com.benzenelabs.hydra.host.data.secret.fakes

import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.secret.SecretKey
import com.benzenelabs.hydra.host.data.secret.SecretNotFoundException
import com.benzenelabs.hydra.host.data.secret.SecretVault
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeSecretVault : SecretVault {

    private val store = mutableMapOf<String, String>()
    private val mutex = Mutex()

    private fun scopedKey(scope: StorageScope, key: SecretKey): String =
            when (scope) {
                is StorageScope.Agent -> "agent:${key.value}"
                is StorageScope.Extension -> "ext:${scope.contributionId.value}:${key.value}"
            }

    override suspend fun put(scope: StorageScope, key: SecretKey, value: String) {
        mutex.withLock { store[scopedKey(scope, key)] = value }
    }

    override suspend fun get(scope: StorageScope.Extension, key: SecretKey): String {
        return mutex.withLock { store[scopedKey(scope, key)] }
                ?: throw SecretNotFoundException(scope, key)
    }

    override suspend fun exists(scope: StorageScope, key: SecretKey): Boolean {
        return mutex.withLock { store.containsKey(scopedKey(scope, key)) }
    }

    override suspend fun delete(scope: StorageScope, key: SecretKey): Boolean {
        return mutex.withLock { store.remove(scopedKey(scope, key)) != null }
    }

    override suspend fun deleteAll(extensionId: ContributionId) {
        val prefix = "ext:${extensionId.value}:"
        mutex.withLock { store.keys.filter { it.startsWith(prefix) }.forEach { store.remove(it) } }
    }

    override suspend fun listKeys(extensionId: ContributionId): List<SecretKey> {
        val prefix = "ext:${extensionId.value}:"
        return mutex.withLock {
            store.keys.filter { it.startsWith(prefix) }.map { SecretKey(it.removePrefix(prefix)) }
        }
    }
}
