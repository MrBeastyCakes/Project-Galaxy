package com.projectorbit.app

import android.app.Application
import com.projectorbit.domain.physics.PhysicsThread
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OrbitApplication : Application() {

    /**
     * PhysicsThread is injected here so it can be started and released
     * with the application process lifecycle.
     *
     * Using field injection (@Inject) because Application subclasses cannot
     * use constructor injection with Hilt.
     */
    @Inject
    lateinit var physicsThread: PhysicsThread

    override fun onCreate() {
        super.onCreate()
        physicsThread.start()
    }

    override fun onTerminate() {
        physicsThread.release()
        super.onTerminate()
    }
}
