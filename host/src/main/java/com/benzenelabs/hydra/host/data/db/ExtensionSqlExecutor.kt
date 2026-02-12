package com.benzenelabs.hydra.host.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes raw SQL statements on behalf of a specific extension against its isolated
 * SQLite database file.
 */
class ExtensionSqlExecutor(
    val extensionId: ContributionId,
    private val db: SQLiteDatabase
) {

    /**
     * Executes a SELECT statement.
     *
     * @throws SqlExecutionException on SQLite error.
     */
    suspend fun query(sql: String, bindArgs: List<Any?> = emptyList()): SqlQueryResult =
        withContext(Dispatchers.IO) {
            try {
                db.rawQuery(sql, bindArgs.map { it?.toString() }.toTypedArray()).use { c ->
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
                throw SqlExecutionException("Extension SQL query failed: ${e.message}", e)
            }
        }

    /**
     * Executes an INSERT, UPDATE, DELETE, or DDL statement.
     *
     * @throws SqlExecutionException on SQLite error.
     */
    suspend fun execute(sql: String, bindArgs: List<Any?> = emptyList()): SqlMutationResult =
        withContext(Dispatchers.IO) {
            try {
                db.execSQL(sql, bindArgs.toTypedArray())
                SqlMutationResult(
                    rowsAffected = db.rawQuery("SELECT changes()", null).use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    },
                    lastInsertRowId = db.rawQuery("SELECT last_insert_rowid()", null).use { c ->
                        if (c.moveToFirst()) c.getLong(0) else -1L
                    }
                )
            } catch (e: Exception) {
                throw SqlExecutionException("Extension SQL execution failed: ${e.message}", e)
            }
        }

    /** Closes the underlying [SQLiteDatabase] handle. */
    fun close() {
        if (db.isOpen) db.close()
    }
}
