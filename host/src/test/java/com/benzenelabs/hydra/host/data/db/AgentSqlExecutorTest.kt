package com.benzenelabs.hydra.host.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class AgentSqlExecutorTest {

    // We can test extractTableNames because it is internal and visible to tests in the same package
    // if we are in the correct source set.
    // However, unit tests are in `src/test`, and the code is in `src/main`.
    // Kotlin `internal` is visible across modules in the same project only if configured,
    // but usually standard Gradle setup allows test to see internal of main.

    private val executor = AgentSqlExecutor(mock(SupportSQLiteDatabase::class.java))

    @Test
    fun `extractTableNames finds tables in various clauses`() {
        val cases =
                mapOf(
                        "SELECT * FROM users" to listOf("users"),
                        "INSERT INTO logs VALUES (1)" to listOf("logs"),
                        "UPDATE events SET seen=1" to listOf("events"),
                        "DELETE FROM cache" to listOf("cache"),
                        "SELECT * FROM a JOIN b ON a.id = b.id" to listOf("a", "b"),
                        "CREATE TABLE new_table (id INT)" to listOf("new_table"),
                        "SELECT * FROM Users" to
                                listOf(
                                        "Users"
                                ), // Case preservation? logic says regex is case insensitive,
                        // capture is group 1.
                        // Regex: (?:FROM|JOIN|INTO|UPDATE|TABLE)\s+([a-zA-Z_][a-zA-Z0-9_]*)
                        "SELECT * FROM  users " to listOf("users") // extra spaces
                )

        cases.forEach { (sql, expected) ->
            val result = executor.extractTableNames(sql)
            assertEquals(expected, result, "Failed for SQL: $sql")
        }
    }
}
