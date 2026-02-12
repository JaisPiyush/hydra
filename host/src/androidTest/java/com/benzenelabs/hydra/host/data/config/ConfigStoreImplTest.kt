package com.benzenelabs.hydra.host.data.config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.db.PublicDatabase
import com.benzenelabs.hydra.host.data.secret.fakes.FakeSecretVault
import com.benzenelabs.hydra.contributions.api.ContributionId
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigStoreImplTest {

    private lateinit var db: PublicDatabase
    private lateinit var vault: FakeSecretVault
    private lateinit var store: ConfigStoreImpl

    private val scopeAgent = StorageScope.Agent
    private val extId = ContributionId("test.ext")
    private val scopeExt = StorageScope.Extension(extId)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
        vault = FakeSecretVault()
        store = ConfigStoreImpl(db.configEntryDao(), vault)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun setAndGet_nonSecretField_persistsInDao() = runTest {
        val field = ConfigField("theme", "auto", isSecret = false)

        // Initial get returns default
        assertEquals("auto", store.get(scopeAgent, field))

        store.set(scopeAgent, field, "dark")
        assertEquals("dark", store.get(scopeAgent, field))
    }

    @Test
    fun setAndGet_secretField_persistsInVaultAndDaoSentinel() = runTest {
        val field = ConfigField("api_key", null, isSecret = true)

        store.set(scopeExt, field, "12345")

        // Value is retrieved from vault
        assertEquals("12345", store.get(scopeExt, field))

        // Check backing stores directly
        // DAO should have sentinel (null value, isSecret=true)
        val entry = db.configEntryDao().find(scopeExt.label, field.name)
        assertNull(entry?.value)
        assertTrue(entry?.isSecret == true)

        // Vault should have value
        assertTrue(vault.exists(scopeExt, field.secretKey))
    }

    @Test
    fun get_secretField_agentScope_throwsAccessDenied() = runTest {
        val field = ConfigField("api_key", null, isSecret = true)
        store.set(scopeAgent, field, "agent_secret") // Agent CAN write

        try {
            store.get(scopeAgent, field) // Agent CANNOT read
            fail("Expected ConfigSecretAccessDeniedException")
        } catch (e: ConfigSecretAccessDeniedException) {
            // Success
        }
    }

    @Test
    fun delete_removesValue_andReturnsDefault() = runTest {
        val field = ConfigField("theme", "auto", isSecret = false)
        store.set(scopeExt, field, "light")

        store.delete(scopeExt, field)

        assertEquals("auto", store.get(scopeExt, field))
    }

    @Test
    fun delete_secretField_removesFromVaultToo() = runTest {
        val field = ConfigField("token", null, isSecret = true)
        store.set(scopeExt, field, "abc")

        store.delete(scopeExt, field)

        assertNull(store.get(scopeExt, field)) // Default is null
        assertFalse(vault.exists(scopeExt, field.secretKey))
    }

    @Test
    fun getAll_returnsSnapshot_withSecretValuesNull() = runTest {
        val f1 = ConfigField("k1", "d1", false)
        val f2 = ConfigField("k2", null, true)

        store.set(scopeExt, f1, "v1")
        store.set(scopeExt, f2, "s2")

        val all = store.getAll(scopeExt)
        assertEquals(2, all.size)
        assertEquals("v1", all["k1"])
        assertNull(all["k2"]) // Secret value must be null
    }

    @Test
    fun deleteAll_clearsScope() = runTest {
        val f1 = ConfigField("k1", "d1", false)
        val f2 = ConfigField("k2", null, true)

        store.set(scopeExt, f1, "v1")
        store.set(scopeExt, f2, "s2")

        store.deleteAll(scopeExt)

        assertEquals("d1", store.get(scopeExt, f1))
        assertNull(store.get(scopeExt, f2))
        assertFalse(vault.exists(scopeExt, f2.secretKey))
    }

    @Test
    fun observe_emitsChanges() = runTest {
        val field = ConfigField("mode", "default", false)

        store.observe(scopeAgent, field).test {
            assertEquals("default", awaitItem()) // Initial emit

            store.set(scopeAgent, field, "custom")
            assertEquals("custom", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
