package com.benzenelabs.hydra.host.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtensionSqlExecutorTest {

    private lateinit var db: SQLiteDatabase
    private lateinit var executor: ExtensionSqlExecutor

    @Before
    fun setUp() {
        db = SQLiteDatabase.create(null) // In-memory database
        executor = ExtensionSqlExecutor(ContributionId("test.ext"), db)

        db.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, name TEXT)")
    }

    @After
    fun tearDown() {
        executor.close()
    }

    @Test
    fun query_returnsCorrectRows() = runTest {
        db.execSQL("INSERT INTO t (id, name) VALUES (1, 'foo')")

        val result = executor.query("SELECT * FROM t WHERE id = ?", listOf(1))

        assertEquals(listOf("id", "name"), result.columns)
        assertEquals(1, result.rows.size)
        assertEquals(1L, result.rows[0][0])
        assertEquals("foo", result.rows[0][1])
    }

    @Test
    fun execute_insert_returnsRowsAffected() = runTest {
        val result = executor.execute("INSERT INTO t (name) VALUES (?)", listOf("bar"))

        assertEquals(1, result.rowsAffected)
        // first insert was manually done in setup? No, setup just created table.
        // Wait, query test inserted one. This test is separate. ID should be 1.
        assertEquals(1L, result.lastInsertRowId)
    }

    @Test
    fun close_closesDatabase() {
        executor.close()
        assertFalse(db.isOpen)
    }

    @Test
    fun query_afterClose_throwsException() = runTest {
        executor.close()
        try {
            executor.query("SELECT * FROM t")
            fail("Expected SqlExecutionException")
        } catch (e: SqlExecutionException) {
            // Success
        }
    }
}
