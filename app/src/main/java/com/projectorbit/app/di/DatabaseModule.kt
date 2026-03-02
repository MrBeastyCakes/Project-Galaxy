package com.projectorbit.app.di

import android.content.Context
import com.projectorbit.data.db.OrbitDatabase
import com.projectorbit.data.db.dao.CelestialBodyDao
import com.projectorbit.data.db.dao.LinkDao
import com.projectorbit.data.db.dao.NebulaFragmentDao
import com.projectorbit.data.db.dao.NoteContentDao
import com.projectorbit.data.db.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOrbitDatabase(@ApplicationContext context: Context): OrbitDatabase =
        OrbitDatabase.create(context)

    @Provides
    fun provideCelestialBodyDao(db: OrbitDatabase): CelestialBodyDao = db.celestialBodyDao()

    @Provides
    fun provideNoteContentDao(db: OrbitDatabase): NoteContentDao = db.noteContentDao()

    @Provides
    fun provideLinkDao(db: OrbitDatabase): LinkDao = db.linkDao()

    @Provides
    fun provideTagDao(db: OrbitDatabase): TagDao = db.tagDao()

    @Provides
    fun provideNebulaFragmentDao(db: OrbitDatabase): NebulaFragmentDao = db.nebulaFragmentDao()
}
