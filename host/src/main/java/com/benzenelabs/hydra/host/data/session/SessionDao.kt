package com.benzenelabs.hydra.host.data.session

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SessionEntity)

    @Update
    suspend fun update(entity: SessionEntity): Int

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun findById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE channelId = :channelId AND remoteId = :remoteId ORDER BY createdAt DESC LIMIT 1")
    suspend fun findActiveByRemote(channelId: String, remoteId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE channelId = :channelId ORDER BY updatedAt DESC")
    suspend fun findByChannel(channelId: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE state = :state")
    suspend fun findByState(state: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE channelId = :channelId")
    fun observeByChannel(channelId: String): Flow<List<SessionEntity>>

    @Query("UPDATE sessions SET state = :state, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: String, updatedAt: Long): Int

    @Query("UPDATE sessions SET lastMessageAt = :lastMessageAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchLastMessage(id: String, lastMessageAt: Long, updatedAt: Long): Int

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM sessions WHERE channelId = :channelId")
    suspend fun deleteByChannel(channelId: String)
}
