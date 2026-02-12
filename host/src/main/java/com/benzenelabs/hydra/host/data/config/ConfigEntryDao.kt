package com.benzenelabs.hydra.host.data.config

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO for scoped configuration entries. */
@Dao
interface ConfigEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConfigEntryEntity)

    @Query("SELECT * FROM config_entries WHERE scopeLabel = :scopeLabel AND key = :key")
    suspend fun find(scopeLabel: String, key: String): ConfigEntryEntity?

    @Query("SELECT * FROM config_entries WHERE scopeLabel = :scopeLabel AND key = :key")
    fun observe(scopeLabel: String, key: String): Flow<ConfigEntryEntity?>

    @Query("SELECT * FROM config_entries WHERE scopeLabel = :scopeLabel")
    suspend fun findAll(scopeLabel: String): List<ConfigEntryEntity>

    @Query("DELETE FROM config_entries WHERE scopeLabel = :scopeLabel AND key = :key")
    suspend fun delete(scopeLabel: String, key: String): Int

    @Query("DELETE FROM config_entries WHERE scopeLabel = :scopeLabel")
    suspend fun deleteAll(scopeLabel: String)
}
