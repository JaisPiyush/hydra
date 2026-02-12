package com.benzenelabs.hydra.host.data.blob

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** DAO for blob metadata records. */
@Dao
interface BlobMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlobMetadataEntity)

    @Query("SELECT * FROM blob_metadata WHERE blobId = :blobId")
    suspend fun findById(blobId: String): BlobMetadataEntity?

    @Query("SELECT * FROM blob_metadata WHERE ownerScopeLabel = :scopeLabel")
    suspend fun findByOwner(scopeLabel: String): List<BlobMetadataEntity>

    @Query("DELETE FROM blob_metadata WHERE blobId = :blobId")
    suspend fun deleteById(blobId: String): Int

    @Query("DELETE FROM blob_metadata WHERE ownerScopeLabel = :scopeLabel")
    suspend fun deleteByOwner(scopeLabel: String)

    @Query("SELECT COUNT(*) FROM blob_metadata WHERE ownerScopeLabel = :scopeLabel")
    suspend fun countByOwner(scopeLabel: String): Long
}
