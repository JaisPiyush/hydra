package com.benzenelabs.hydra.host.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtensionDatabaseManagerTest {

    private lateinit var manager: ExtensionDatabaseManager
    private val ext1 = ContributionId("ext.one")
    private val ext2 = ContributionId("ext.two")

    @Before
    fun setUp() {
        manager = ExtensionDatabaseManager(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        manager.closeAll()
        manager.deleteDatabase(ext1)
        manager.deleteDatabase(ext2)
    }

    @Test
    fun getOrCreate_returnsExecutorAndCachesIt() {
        val exec1 = manager.getOrCreate(ext1)
        assertNotNull(exec1)

        val exec2 = manager.getOrCreate(ext1)
        assertSame(exec1, exec2)
    }

    @Test
    fun deleteDatabase_removesFileAndClosesExecutor() {
        val exec = manager.getOrCreate(ext1)
        val file = manager.databaseFileFor(ext1)

        assertTrue(file.exists())

        manager.deleteDatabase(ext1)

        assertFalse(file.exists())

        // Verify it is closed
        runTest {
            try {
                exec.query("SELECT 1")
                fail("Expected executor to be closed")
            } catch (e: SqlExecutionException) {
                // Success
            }
        }
    }

    @Test
    fun closeAll_closesAllOpenDatabases() = runTest {
        val exec1 = manager.getOrCreate(ext1)
        val exec2 = manager.getOrCreate(ext2)

        manager.closeAll()

        try {
            exec1.query("SELECT 1")
            fail("Expected exec1 to be closed")
        } catch (e: SqlExecutionException) {
            // Success
        }

        try {
            exec2.query("SELECT 1")
            fail("Expected exec2 to be closed")
        } catch (e: SqlExecutionException) {
            // Success
        }
    }
}
