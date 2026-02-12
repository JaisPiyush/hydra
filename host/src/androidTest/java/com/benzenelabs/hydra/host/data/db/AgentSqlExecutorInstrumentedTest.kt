package com.benzenelabs.hydra.host.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgentSqlExecutorInstrumentedTest {

    private lateinit var roomDb: PublicDatabase
    private lateinit var db: SupportSQLiteDatabase
    private lateinit var executor: AgentSqlExecutor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        roomDb =
                Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java)
                        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // Safe for in-memory
                        .build()
        db = roomDb.openHelper.writableDatabase
        executor = AgentSqlExecutor(db)

        // Setup initial schema for testing
        // We create a table that we will 'whitelist'
        db.execSQL("CREATE TABLE public_test (id INTEGER PRIMARY KEY, name TEXT)")
        db.execSQL("INSERT INTO public_test (id, name) VALUES (1, 'foo')")

        // Create a private table
        db.execSQL("CREATE TABLE private_test (id INTEGER PRIMARY KEY, secret TEXT)")
        db.execSQL("INSERT INTO private_test (id, secret) VALUES (1, 'hidden')")

        setPublicTables(setOf("public_test"))
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        roomDb.close()
        setPublicTables(emptySet())
    }

    @Test
    fun query_publicTable_returnsRows() = runTest {
        val result = executor.query("SELECT * FROM public_test WHERE id = ?", listOf(1L))

        assertEquals(listOf("id", "name"), result.columns)
        assertEquals(1, result.rows.size)
        assertEquals(1L, result.rows[0][0])
        assertEquals("foo", result.rows[0][1])
    }

    @Test
    fun execute_insert_returnsRowsAffected() = runTest {
        val result = executor.execute("INSERT INTO public_test (name) VALUES (?)", listOf("bar"))

        assertEquals(1, result.rowsAffected)
        // lastInsertRowId might be 2
        assertEquals(2L, result.lastInsertRowId)
    }

    @Test
    fun query_privateTable_throwsUnauthorized() = runTest {
        try {
            executor.query("SELECT * FROM private_test")
            fail("Expected UnauthorizedTableException")
        } catch (e: UnauthorizedTableException) {
            // Success
        }
    }

    @Test
    fun execute_privateTable_throwsUnauthorized() = runTest {
        try {
            executor.execute("DELETE FROM private_test")
            fail("Expected UnauthorizedTableException")
        } catch (e: UnauthorizedTableException) {
            // Success
        }
    }

    @Test
    fun query_joinPublicAndPrivate_throwsUnauthorized() = runTest {
        try {
            executor.query(
                    "SELECT * FROM public_test JOIN private_test ON public_test.id = private_test.id"
            )
            fail("Expected UnauthorizedTableException")
        } catch (e: UnauthorizedTableException) {
            // Success
        }
    }

    private fun setPublicTables(tables: Set<String>) {
        val field = PublicTableRegistry::class.java.getDeclaredField("PUBLIC_TABLES")
        field.isAccessible = true
        field.set(PublicTableRegistry, tables)
    }
}
