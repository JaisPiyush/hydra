package com.benzenelabs.hydra.host.data.db

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes raw SQL statements on behalf of the AI agent against [PublicDatabase].
 *
 * Only tables registered in [PublicTableRegistry] may be accessed.
 */
class AgentSqlExecutor(
    private val db: SupportSQLiteDatabase,
    private val registry: PublicTableRegistry = PublicTableRegistry
) {

    /**
     * Executes a SELECT statement and returns rows as a [SqlQueryResult].
     *
     * @throws UnauthorizedTableException if any referenced table is not public.
     * @throws SqlExecutionException if SQLite reports an error.
     */
    suspend fun query(sql: String, bindArgs: List<Any?> = emptyList()): SqlQueryResult =
        withContext(Dispatchers.IO) {
            validateTables(sql)
            try {
                db.query(sql, bindArgs.toTypedArray()).use { c ->
                    val columns = (0 until c.columnCount).map(c::getColumnName)
                    val rows = mutableListOf<List<Any?>>()
                    while (c.moveToNext()) {
                        rows += (0 until c.columnCount).map { i ->
                            when (c.getType(i)) {
                                Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                                Cursor.FIELD_TYPE_STRING -> c.getString(i)
                                Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
                                else -> null
                            }
                        }
                    }
                    SqlQueryResult(columns = columns, rows = rows)
                }
            } catch (e: Exception) {
                throw SqlExecutionException("SQL query failed: ${e.message}", e)
            }
        }

    /**
     * Executes an INSERT, UPDATE, DELETE, or DDL statement.
     *
     * @throws UnauthorizedTableException if any referenced table is not public.
     * @throws SqlExecutionException if SQLite reports an error.
     */
    suspend fun execute(sql: String, bindArgs: List<Any?> = emptyList()): SqlMutationResult =
        withContext(Dispatchers.IO) {
            validateTables(sql)
            try {
                db.execSQL(sql, bindArgs.toTypedArray())
                SqlMutationResult(
                    rowsAffected = db.query("SELECT changes()").use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    },
                    lastInsertRowId = db.query("SELECT last_insert_rowid()").use { c ->
                        if (c.moveToFirst()) c.getLong(0) else -1L
                    }
                )
            } catch (e: Exception) {
                throw SqlExecutionException("SQL execution failed: ${e.message}", e)
            }
        }

    private fun validateTables(sql: String) {
        extractTableNames(sql).forEach { tableName ->
            if (!registry.isPublic(tableName)) {
                throw UnauthorizedTableException(tableName)
            }
        }
    }

    /**
     * Extracts table names from SQL clauses: FROM, JOIN, INTO, UPDATE, and TABLE.
     */
    internal fun extractTableNames(sql: String): List<String> {
        val pattern = Regex(
            """(?:FROM|JOIN|INTO|UPDATE|TABLE)\s+([a-zA-Z_][a-zA-Z0-9_]*)""",
            RegexOption.IGNORE_CASE
        )
        return pattern.findAll(sql).map { it.groupValues[1] }.toList()
    }
}

/**
 * Result of a successful SELECT execution.
 */
data class SqlQueryResult(
    val columns: List<String>,
    val rows: List<List<Any?>>
)

/**
 * Result of a successful INSERT/UPDATE/DELETE execution.
 */
data class SqlMutationResult(
    val rowsAffected: Int,
    val lastInsertRowId: Long
)

/** Thrown when SQL references a table not in [PublicTableRegistry]. */
class UnauthorizedTableException(tableName: String) :
    SecurityException("Agent SQL access denied to table: '$tableName'")

/** Thrown when SQLite reports a runtime error during execution. */
class SqlExecutionException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
