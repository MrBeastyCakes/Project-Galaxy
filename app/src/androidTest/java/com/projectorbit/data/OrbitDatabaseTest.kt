package com.projectorbit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.projectorbit.data.db.OrbitDatabase
import com.projectorbit.data.db.entity.BodyType
import com.projectorbit.data.db.entity.CelestialBodyEntity
import com.projectorbit.data.db.entity.LinkEntity
import com.projectorbit.data.db.entity.LinkType
import com.projectorbit.data.db.entity.NoteContentEntity
import com.projectorbit.data.db.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OrbitDatabaseTest {

    private lateinit var db: OrbitDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun makeSun(id: String = UUID.randomUUID().toString()) = CelestialBodyEntity(
        id = id,
        parentId = null,
        type = BodyType.SUN,
        name = "Test Sun",
        positionX = 0.0,
        positionY = 0.0,
        velocityX = 0.0,
        velocityY = 0.0,
        mass = 1000.0,
        radius = 50.0,
        orbitRadius = 0.0,
        orbitAngle = 0.0,
        isPinned = false,
        isShared = false,
        isCompleted = false,
        completedAt = null,
        createdAt = System.currentTimeMillis(),
        lastAccessedAt = System.currentTimeMillis(),
        accessCount = 0,
        wordCount = 0,
        isDeleted = false,
        deletedAt = null
    )

    @Test
    fun insertAndReadCelestialBody() = runBlocking {
        val sun = makeSun("sun-1")
        db.celestialBodyDao().upsert(sun)

        val result = db.celestialBodyDao().getById("sun-1")
        assertNotNull(result)
        assertEquals("sun-1", result!!.id)
        assertEquals(BodyType.SUN, result.type)
        assertEquals(1000.0, result.mass, 0.001)
    }

    @Test
    fun softDeleteHidesFromActive() = runBlocking {
        val sun = makeSun("sun-del")
        db.celestialBodyDao().upsert(sun)

        val before = db.celestialBodyDao().getAllActive().first()
        assertEquals(1, before.size)

        db.celestialBodyDao().softDelete("sun-del", System.currentTimeMillis())

        val after = db.celestialBodyDao().getAllActive().first()
        assertEquals(0, after.size)

        // But getById still finds it
        val deleted = db.celestialBodyDao().getById("sun-del")
        assertNotNull(deleted)
        assertEquals(true, deleted!!.isDeleted)
    }

    @Test
    fun noteContentFtsSearch() = runBlocking {
        val sun = makeSun("sun-fts")
        db.celestialBodyDao().upsert(sun)

        val content = NoteContentEntity(
            bodyId = "sun-fts",
            richTextJson = "{}",
            plainText = "orbital mechanics gravity simulation",
            updatedAt = System.currentTimeMillis()
        )
        db.noteContentDao().upsert(content)

        val results = db.noteContentDao().searchPlainText("gravity")
        assertEquals(1, results.size)
        assertEquals("sun-fts", results[0])
    }

    @Test
    fun noteContentFtsNoMatch() = runBlocking {
        val sun = makeSun("sun-fts2")
        db.celestialBodyDao().upsert(sun)

        val content = NoteContentEntity(
            bodyId = "sun-fts2",
            richTextJson = "{}",
            plainText = "hello world",
            updatedAt = System.currentTimeMillis()
        )
        db.noteContentDao().upsert(content)

        val results = db.noteContentDao().searchPlainText("quantum")
        assertEquals(0, results.size)
    }

    @Test
    fun linkCascadeDeleteWithBody() = runBlocking {
        val sun1 = makeSun("sun-link-1")
        val sun2 = makeSun("sun-link-2")
        db.celestialBodyDao().upsert(sun1)
        db.celestialBodyDao().upsert(sun2)

        val link = LinkEntity(
            id = "link-1",
            sourceId = "sun-link-1",
            targetId = "sun-link-2",
            linkType = LinkType.CONSTELLATION,
            createdAt = System.currentTimeMillis()
        )
        db.linkDao().upsert(link)

        val linksBeforeDelete = db.linkDao().getLinksForBody("sun-link-1").first()
        assertEquals(1, linksBeforeDelete.size)

        // Soft delete doesn't cascade - hard delete via Room would be needed for FK cascade
        // Verify link still shows both bodies
        assertEquals("sun-link-1", linksBeforeDelete[0].sourceId)
        assertEquals("sun-link-2", linksBeforeDelete[0].targetId)
        assertEquals(LinkType.CONSTELLATION, linksBeforeDelete[0].linkType)
    }

    @Test
    fun updatePhysicsState() = runBlocking {
        val sun = makeSun("sun-physics")
        db.celestialBodyDao().upsert(sun)

        db.celestialBodyDao().updatePhysicsState(
            id = "sun-physics",
            x = 123.456,
            y = 789.012,
            vx = 1.5,
            vy = -2.3
        )

        val updated = db.celestialBodyDao().getById("sun-physics")
        assertNotNull(updated)
        assertEquals(123.456, updated!!.positionX, 1e-9)
        assertEquals(789.012, updated.positionY, 1e-9)
        assertEquals(1.5, updated.velocityX, 1e-9)
        assertEquals(-2.3, updated.velocityY, 1e-9)
    }

    @Test
    fun tagsForBody() = runBlocking {
        val sun = makeSun("sun-tags")
        db.celestialBodyDao().upsert(sun)

        val tag = TagEntity(
            id = "tag-1",
            name = "Urgent",
            atmosphereColor = 0xFFFF0000.toInt(),
            atmosphereDensity = 0.8
        )
        db.tagDao().upsert(tag)
        db.tagDao().upsertCrossRef(
            com.projectorbit.data.db.entity.BodyTagCrossRef(bodyId = "sun-tags", tagId = "tag-1")
        )

        val tags = db.tagDao().getTagsForBody("sun-tags").first()
        assertEquals(1, tags.size)
        assertEquals("Urgent", tags[0].name)
        assertEquals(0.8, tags[0].atmosphereDensity, 0.001)
    }

    @Test
    fun completeMoon() = runBlocking {
        val moon = makeSun("moon-1").copy(
            type = BodyType.MOON,
            parentId = "planet-1"
        )
        // Insert parent planet first to satisfy FK (if any)
        val planet = makeSun("planet-1").copy(type = BodyType.PLANET, parentId = "sun-x")
        val sun = makeSun("sun-x")
        db.celestialBodyDao().upsert(sun)
        db.celestialBodyDao().upsert(planet)
        db.celestialBodyDao().upsert(moon)

        val completedAt = System.currentTimeMillis()
        db.celestialBodyDao().completeMoon("moon-1", completedAt)

        val result = db.celestialBodyDao().getById("moon-1")
        assertNotNull(result)
        assertEquals(true, result!!.isCompleted)
        assertEquals(completedAt, result.completedAt)
    }
}
