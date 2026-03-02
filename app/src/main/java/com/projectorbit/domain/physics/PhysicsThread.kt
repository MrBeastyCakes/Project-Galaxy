package com.projectorbit.domain.physics

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.projectorbit.domain.model.PhysicsConstants

/**
 * Dedicated HandlerThread that drives the physics simulation loop at a fixed timestep.
 *
 * Threading contract:
 *  - [PhysicsWorld.tick] is only ever called from this thread.
 *  - The main thread and renderer communicate with [PhysicsWorld] via the
 *    [PhysicsWorld.commandQueue] and [PhysicsWorld.snapshotRef] respectively.
 *  - Fixed timestep with accumulator pattern: decouples from actual frame rate.
 *    Any remaining accumulated time carries over to the next frame budget.
 */
class PhysicsThread(private val world: PhysicsWorld) {

    private val TAG = "PhysicsThread"

    private val handlerThread = HandlerThread("physics-thread").also { it.start() }
    private val handler = Handler(handlerThread.looper)

    /** Target tick interval in milliseconds (1000 / 60 ≈ 16.67ms). */
    private val tickIntervalMs: Long = (PhysicsConstants.FIXED_DT * 1000.0).toLong()

    @Volatile
    private var running = false

    private var lastTickTimeNs: Long = 0L

    /** Accumulated time that has not yet been consumed by physics ticks (ns). */
    private var accumulator: Double = 0.0

    private val dtNs: Double = PhysicsConstants.FIXED_DT * 1_000_000_000.0

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return

            val nowNs = System.nanoTime()
            if (lastTickTimeNs == 0L) {
                lastTickTimeNs = nowNs
            }

            val elapsed = (nowNs - lastTickTimeNs).toDouble()
            lastTickTimeNs = nowNs

            // Accumulator pattern -- cap max accumulated time to 4 ticks to prevent
            // spiral-of-death after long pauses (e.g. debugger attach)
            accumulator += elapsed
            val maxAccumulator = dtNs * 4.0
            if (accumulator > maxAccumulator) {
                Log.w(TAG, "Physics accumulator capped at ${accumulator / 1_000_000.0}ms -- possible frame budget overrun")
                accumulator = maxAccumulator
            }

            var ticksThisFrame = 0
            while (accumulator >= dtNs) {
                try {
                    world.tick(PhysicsConstants.FIXED_DT)
                } catch (e: Exception) {
                    Log.e(TAG, "Physics tick threw exception: ${e.message}", e)
                }
                accumulator -= dtNs
                ticksThisFrame++
            }

            // Schedule next tick
            handler.postDelayed(this, tickIntervalMs)
        }
    }

    /**
     * Start the physics loop. Safe to call from any thread.
     */
    fun start() {
        if (running) return
        running = true
        lastTickTimeNs = 0L
        accumulator = 0.0
        handler.post(tickRunnable)
        Log.d(TAG, "Physics thread started")
    }

    /**
     * Stop the physics loop. Safe to call from any thread.
     * Does not destroy the HandlerThread -- call [release] for full cleanup.
     */
    fun stop() {
        running = false
        handler.removeCallbacks(tickRunnable)
        Log.d(TAG, "Physics thread stopped")
    }

    /**
     * Stop the physics loop and shut down the HandlerThread.
     * Call this from onDestroy() of the Application or Activity.
     */
    fun release() {
        stop()
        handlerThread.quitSafely()
        Log.d(TAG, "Physics thread released")
    }

    val isRunning: Boolean get() = running
}
