package com.benzenelabs.hydra.host.data.config

import androidx.room.Entity
import androidx.room.Index

/**
 * Room entity for a single stored configuration value.
 */
@Entity(
    tableName = "config_entries",
    primaryKeys = ["scopeLabel", "key"],
    indices = [Index(value = ["scopeLabel"])]
)
data class ConfigEntryEntity(
    val scopeLabel: String,
    val key: String,
    val value: String?,
    val isSecret: Boolean,
    val updatedAt: Long
)
