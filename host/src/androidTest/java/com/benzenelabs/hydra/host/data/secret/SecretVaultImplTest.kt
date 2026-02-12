package com.benzenelabs.hydra.host.data.secret

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretVaultImplTest {

    private lateinit var vault: SecretVaultImpl
    private val scopeAgent = StorageScope.Agent
    private val extId = ContributionId("test.ext")
    private val scopeExt = StorageScope.Extension(extId)
    private val key1 = SecretKey("key_one")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Ensure clean state (SharedPrefs persist across tests)
        // We can't easily clear encrypted prefs without knowing the filename and key alias.
        // But for testing, maybe we just use unique keys or try to clear it.
        // SecretVaultImpl uses "hydra_secret_vault".

        // We can use the vault itself to clean up if we knew what keys existed.
        // Or we can just use new keys for each test or rely on `deleteAll`.

        vault = SecretVaultImpl(context)
    }

    @Test
    fun putValues_persistAcrossInstances() = runTest {
        vault.put(scopeExt, key1, "secret_value")

        // Create new instance
        val newVault = SecretVaultImpl(ApplicationProvider.getApplicationContext())
        val retrieved = newVault.get(scopeExt, key1)

        assertEquals("secret_value", retrieved)
    }

    @Test
    fun put_overwritesExistingValue() = runTest {
        vault.put(scopeExt, key1, "initial")
        vault.put(scopeExt, key1, "updated")

        assertEquals("updated", vault.get(scopeExt, key1))
    }

    @Test
    fun get_throwsIfNotFound() = runTest {
        val missingKey = SecretKey("missing")
        try {
            vault.get(scopeExt, missingKey)
            fail("Expected SecretNotFoundException")
        } catch (e: SecretNotFoundException) {
            // Success
        }
    }

    @Test
    fun exists_returnsCorrectly() = runTest {
        assertFalse(vault.exists(scopeExt, key1))

        vault.put(scopeExt, key1, "val")
        assertTrue(vault.exists(scopeExt, key1))

        vault.delete(scopeExt, key1)
        assertFalse(vault.exists(scopeExt, key1))
    }

    @Test
    fun delete_returnsFalseForMissingKey() = runTest { assertFalse(vault.delete(scopeExt, key1)) }

    @Test
    fun deleteAll_removesOnlyTargetExtensionSecrets() = runTest {
        val otherExtId = ContributionId("other.ext")
        val scopeOther = StorageScope.Extension(otherExtId)

        vault.put(scopeExt, key1, "val1")
        vault.put(scopeOther, key1, "val2")

        vault.deleteAll(extId)

        assertFalse(vault.exists(scopeExt, key1))
        assertTrue(vault.exists(scopeOther, key1))
    }

    @Test
    fun listKeys_returnsOnlyOwnedKeys() = runTest {
        val key2 = SecretKey("key_two")
        vault.put(scopeExt, key1, "v1")
        vault.put(scopeExt, key2, "v2")
        vault.put(scopeAgent, key1, "agent_val") // should not show up

        val keys = vault.listKeys(extId).map { it.value }.toSet()

        assertEquals(2, keys.size)
        assertTrue(keys.contains("key_one"))
        assertTrue(keys.contains("key_two"))
    }
}
