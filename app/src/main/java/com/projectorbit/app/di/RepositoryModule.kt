package com.projectorbit.app.di

import com.projectorbit.data.repository.CelestialBodyRepositoryImpl
import com.projectorbit.data.repository.LinkRepositoryImpl
import com.projectorbit.data.repository.NebulaFragmentRepositoryImpl
import com.projectorbit.data.repository.NoteContentRepositoryImpl
import com.projectorbit.data.repository.TagRepositoryImpl
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.LinkRepository
import com.projectorbit.domain.repository.NebulaFragmentRepository
import com.projectorbit.domain.repository.NoteContentRepository
import com.projectorbit.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCelestialBodyRepository(
        impl: CelestialBodyRepositoryImpl
    ): CelestialBodyRepository

    @Binds
    @Singleton
    abstract fun bindNoteContentRepository(
        impl: NoteContentRepositoryImpl
    ): NoteContentRepository

    @Binds
    @Singleton
    abstract fun bindLinkRepository(
        impl: LinkRepositoryImpl
    ): LinkRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        impl: TagRepositoryImpl
    ): TagRepository

    @Binds
    @Singleton
    abstract fun bindNebulaFragmentRepository(
        impl: NebulaFragmentRepositoryImpl
    ): NebulaFragmentRepository
}
