package com.benzenelabs.hydra.host.data.db

/**
 * Authoritative list of table names in [PublicDatabase] that the agent may
 * address with raw SQL statements.
 */
object PublicTableRegistry {

    /**
     * Set of table names the agent is permitted to query or mutate.
     * Table names must match exact SQLite table names.
     */
    val PUBLIC_TABLES: Set<String> = setOf(
        // Intentionally empty until host-defined public tables are introduced.
    )

    /** Returns true if [tableName] is listed as a public table (case-insensitive). */
    fun isPublic(tableName: String): Boolean =
        PUBLIC_TABLES.any { it.equals(tableName.trim(), ignoreCase = true) }
}
