package com.benzenelabs.hydra.host.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.benzenelabs.hydra.contributions.api.ContributionId
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages per-extension isolated SQLite database files.
 *
 * File location: `<Context.filesDir>/ext_db/<contributionId.value>.db`.
 */
class ExtensionDatabaseManager(private val context: Context) {

    private val cache = ConcurrentHashMap<String, ExtensionSqlExecutor>()

    /**
     * Returns an [ExtensionSqlExecutor] for the given extension.
     * Creates the database file if it does not yet exist.
     */
    fun getOrCreate(extensionId: ContributionId): ExtensionSqlExecutor =
        cache.getOrPut(extensionId.value) {
            val file = databaseFileFor(extensionId)
            file.parentFile?.mkdirs()
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            ExtensionSqlExecutor(extensionId, db)
        }

    /**
     * Closes the open handle and deletes the database file for [extensionId].
     *
     * @return true if the file was deleted, false if it did not exist.
     */
    fun deleteDatabase(extensionId: ContributionId): Boolean {
        cache.remove(extensionId.value)?.close()
        return databaseFileFor(extensionId).delete()
    }

    /** Closes all open database handles. */
    fun closeAll() {
        cache.values.forEach { it.close() }
        cache.clear()
    }

    /** Returns the file path for an extension database. */
    fun databaseFileFor(extensionId: ContributionId): File =
        File(context.filesDir, "ext_db/${extensionId.value}.db")
}
