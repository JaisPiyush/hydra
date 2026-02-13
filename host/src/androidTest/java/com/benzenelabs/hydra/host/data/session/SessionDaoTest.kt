package com.benzenelabs.hydra.host.data.session

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.benzenelabs.hydra.host.data.db.PublicDatabase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDaoTest {

    private lateinit var db: PublicDatabase
    private lateinit var dao: SessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
        dao = db.sessionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndFindById() = runTest {
        val entity = createEntity(id = "s1")
        dao.insert(entity)
        val retrieved = dao.findById("s1")
        assertEquals(entity, retrieved)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun insertDuplicateIdThrows() = runTest {
        val entity = createEntity(id = "s1")
        dao.insert(entity)
        dao.insert(entity) // ABORT strategy
    }

    @Test
    fun findActiveByRemote() = runTest {
        val channelId = "ch1"
        val remoteId = "r1"

        // Older session
        dao.insert(createEntity("s1", channelId, remoteId, createdAt = 100))
        // Newer session
        dao.insert(createEntity("s2", channelId, remoteId, createdAt = 200))

        val active = dao.findActiveByRemote(channelId, remoteId)
        assertNotNull(active)
        assertEquals("s2", active?.id)
    }

    @Test
    fun findByChannelOrdered() = runTest {
        val ch = "ch1"
        dao.insert(createEntity("s1", ch, updatedAt = 100))
        dao.insert(createEntity("s2", ch, updatedAt = 300))
        dao.insert(createEntity("s3", ch, updatedAt = 200))

        val list = dao.findByChannel(ch)
        assertEquals(3, list.size)
        // Ordered by updatedAt DESC
        assertEquals("s2", list[0].id)
        assertEquals("s3", list[1].id)
        assertEquals("s1", list[2].id)
    }

    @Test
    fun updateState() = runTest {
        dao.insert(createEntity("s1", state = "PENDING"))
        val rows = dao.updateState("s1", "ACTIVE", 2000)
        assertEquals(1, rows)

        val updated = dao.findById("s1")
        assertEquals("ACTIVE", updated?.state)
        assertEquals(2000L, updated?.updatedAt)
    }

    @Test
    fun touchLastMessage() = runTest {
        dao.insert(createEntity("s1", lastMessageAt = null, updatedAt = 1000))
        val rows = dao.touchLastMessage("s1", 5000, 6000)
        assertEquals(1, rows)

        val updated = dao.findById("s1")
        assertEquals(5000L, updated?.lastMessageAt)
        assertEquals(6000L, updated?.updatedAt)
    }

    @Test
    fun observeByChannel() = runTest {
        dao.observeByChannel("ch1").test {
            assertEquals(emptyList<SessionEntity>(), awaitItem())

            dao.insert(createEntity("s1", "ch1"))
            val list1 = awaitItem()
            assertEquals(1, list1.size)
            assertEquals("s1", list1[0].id)

            dao.insert(createEntity("s2", "ch1"))
            val list2 = awaitItem()
            assertEquals(2, list2.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByChannel() = runTest {
        dao.insert(createEntity("s1", "ch1"))
        dao.insert(createEntity("s2", "ch1"))
        dao.insert(createEntity("s3", "ch2"))

        dao.deleteByChannel("ch1")

        assertTrue(dao.findByChannel("ch1").isEmpty())
        assertNotNull(dao.findById("s3"))
    }

    private fun createEntity(
            id: String = "s1",
            channelId: String = "ch1",
            remoteId: String = "r1",
            state: String = "ACTIVE",
            authRef: String? = null,
            metadata: String? = null,
            createdAt: Long = 1000,
            updatedAt: Long = 1000,
            lastMessageAt: Long? = null
    ) =
            SessionEntity(
                    id = id,
                    channelId = channelId,
                    remoteId = remoteId,
                    displayName = "Session $id",
                    state = state,
                    authRef = authRef,
                    metadata = metadata,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    lastMessageAt = lastMessageAt
            )
}
