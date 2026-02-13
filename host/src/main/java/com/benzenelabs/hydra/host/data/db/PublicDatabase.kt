package com.benzenelabs.hydra.host.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.benzenelabs.hydra.host.data.blob.BlobMetadataDao
import com.benzenelabs.hydra.host.data.blob.BlobMetadataEntity
import com.benzenelabs.hydra.host.data.config.ConfigEntryDao
import com.benzenelabs.hydra.host.data.config.ConfigEntryEntity
import com.benzenelabs.hydra.host.data.session.SessionDao
import com.benzenelabs.hydra.host.data.session.SessionEntity
import com.benzenelabs.hydra.host.data.vector.VectorRecordDao
import com.benzenelabs.hydra.host.data.vector.VectorRecordEntity

/**
 * Room database for all host-managed tables.
 *
 * Tables in this database are divided into two categories:
 * - Public tables: explicitly registered in [PublicTableRegistry].
 * - Internal tables: host-private tables not reachable via agent SQL.
 */
@Database(
    entities = [
        BlobMetadataEntity::class,
        ConfigEntryEntity::class,
        VectorRecordEntity::class,
        SessionEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class PublicDatabase : RoomDatabase() {
    abstract fun blobMetadataDao(): BlobMetadataDao
    abstract fun configEntryDao(): ConfigEntryDao
    abstract fun vectorRecordDao(): VectorRecordDao
    abstract fun sessionDao(): SessionDao
}
