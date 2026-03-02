package com.projectorbit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.projectorbit.data.db.converter.Converters
import com.projectorbit.data.db.dao.CelestialBodyDao
import com.projectorbit.data.db.dao.LinkDao
import com.projectorbit.data.db.dao.NebulaFragmentDao
import com.projectorbit.data.db.dao.NoteContentDao
import com.projectorbit.data.db.dao.TagDao
import com.projectorbit.data.db.entity.BodyTagCrossRef
import com.projectorbit.data.db.entity.CelestialBodyEntity
import com.projectorbit.data.db.entity.LinkEntity
import com.projectorbit.data.db.entity.NebulaFragmentEntity
import com.projectorbit.data.db.entity.NoteContentEntity
import com.projectorbit.data.db.entity.TagEntity
import com.projectorbit.data.db.fts.NoteContentFts

@Database(
    entities = [
        CelestialBodyEntity::class,
        NoteContentEntity::class,
        NoteContentFts::class,
        TagEntity::class,
        BodyTagCrossRef::class,
        LinkEntity::class,
        NebulaFragmentEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun celestialBodyDao(): CelestialBodyDao
    abstract fun noteContentDao(): NoteContentDao
    abstract fun linkDao(): LinkDao
    abstract fun tagDao(): TagDao
    abstract fun nebulaFragmentDao(): NebulaFragmentDao

    companion object {
        const val DATABASE_NAME = "orbit.db"

        fun create(context: Context): OrbitDatabase =
            Room.databaseBuilder(context, OrbitDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
