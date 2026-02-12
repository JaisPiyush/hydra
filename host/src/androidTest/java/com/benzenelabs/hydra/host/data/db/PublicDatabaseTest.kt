package com.benzenelabs.hydra.host.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PublicDatabaseTest {

    private lateinit var db: PublicDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun databaseCreatedSuccess() {
        assertNotNull(db)
    }

    @Test
    fun daosAreAccessible() {
        assertNotNull(db.blobMetadataDao())
        assertNotNull(db.configEntryDao())
        assertNotNull(db.vectorRecordDao())
    }
}
