package com.benzenelabs.hydra.host.data.session

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisted sessions.
 *
 * The composite index on (channelId, remoteId) enables fast lookups by
 * platform conversation identifier.
 */
@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["channelId", "remoteId"], unique = false),
        Index(value = ["channelId"])
    ]
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val remoteId: String,
    val displayName: String,
    val state: String,
    val authRef: String?,
    val metadata: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessageAt: Long?
)
