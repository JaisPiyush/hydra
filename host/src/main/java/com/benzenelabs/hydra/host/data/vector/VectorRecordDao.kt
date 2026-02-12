package com.benzenelabs.hydra.host.data.vector

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** DAO for vector records. */
@Dao
interface VectorRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VectorRecordEntity)

    @Query("SELECT * FROM vector_records WHERE scopeLabel = :scopeLabel AND collection = :collection AND id = :id")
    suspend fun find(scopeLabel: String, collection: String, id: String): VectorRecordEntity?

    @Query("SELECT * FROM vector_records WHERE scopeLabel = :scopeLabel AND collection = :collection")
    suspend fun findAll(scopeLabel: String, collection: String): List<VectorRecordEntity>

    @Query("DELETE FROM vector_records WHERE scopeLabel = :scopeLabel AND collection = :collection AND id = :id")
    suspend fun delete(scopeLabel: String, collection: String, id: String): Int

    @Query("DELETE FROM vector_records WHERE scopeLabel = :scopeLabel")
    suspend fun deleteAll(scopeLabel: String)

    @Query("SELECT COUNT(*) FROM vector_records WHERE scopeLabel = :scopeLabel AND collection = :collection")
    suspend fun count(scopeLabel: String, collection: String): Long
}
