package com.projectorbit.app.di

import com.projectorbit.domain.physics.PhysicsThread
import com.projectorbit.domain.physics.PhysicsWorld
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhysicsModule {

    /**
     * PhysicsWorld is a @Singleton — application-scoped, survives configuration changes.
     * It owns its own HandlerThread and command queue. ViewModels receive the same instance.
     */
    @Provides
    @Singleton
    fun providePhysicsWorld(): PhysicsWorld = PhysicsWorld()

    /**
     * PhysicsThread drives the simulation loop at 60Hz.
     * Provided as a @Singleton so OrbitApplication can start/stop it with the process lifecycle.
     * Start is called in OrbitApplication.onCreate(); release in OrbitApplication.onTerminate().
     */
    @Provides
    @Singleton
    fun providePhysicsThread(world: PhysicsWorld): PhysicsThread = PhysicsThread(world)
}
