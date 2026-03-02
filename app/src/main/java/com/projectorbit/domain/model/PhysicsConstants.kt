package com.projectorbit.domain.model

/**
 * Global tuning parameters for the physics engine.
 * All values are configurable but have well-tested defaults.
 */
object PhysicsConstants {
    /** Overall gravity strength. */
    const val G: Double = 50.0

    /** Gravity softening -- prevents force singularity at r -> 0.
     *  Formula: F = G * m1 * m2 / (r^2 + epsilon^2) */
    const val EPSILON: Double = 1.0

    /** Hard cap on velocity magnitude per tick. Prevents escape/explosion. */
    const val MAX_VELOCITY: Double = 500.0

    /** Orbit spring stiffness -- how strongly bodies stick to their orbitRadius. */
    const val ORBIT_SPRING_K: Double = 0.5

    /** Orbital speed multiplier for tangential velocity injection. */
    const val TANGENTIAL_VELOCITY_FACTOR: Double = 1.0

    /** Velocity decay per tick. Prevents runaway oscillation. */
    const val DAMPING_FACTOR: Double = 0.995

    /** How fast stale notes drift outward per day of inactivity. */
    const val DECAY_DRIFT_RATE: Double = 0.001

    /** Minimum mass for any body. */
    const val MASS_FLOOR: Double = 1.0

    /** Extra mass for pinned notes. */
    const val MASS_PIN_BONUS: Double = 5.0

    /** Mass gained per word of note content. */
    const val MASS_WORD_COUNT_FACTOR: Double = 0.01

    /** Mass gained per access event. */
    const val MASS_ACCESS_FACTOR: Double = 0.1

    /** Barnes-Hut approximation threshold. Higher = faster, less accurate. */
    const val BARNES_HUT_THETA: Double = 0.7

    /** Fixed timestep for deterministic simulation (1/60 second). */
    const val FIXED_DT: Double = 1.0 / 60.0

    /** Fixed mass for Sun bodies. */
    const val SUN_MASS: Double = 100.0

    /** Fixed mass for Moon bodies (excluded from Barnes-Hut tree). */
    const val MOON_MASS: Double = 0.5

    /** Fixed mass for Asteroid bodies. */
    const val ASTEROID_MASS: Double = 0.2

    /** Number of days without access before orbital decay begins. */
    const val DECAY_THRESHOLD_DAYS: Long = 60L

    /** Damping coefficient for Sun/fixed bodies (they don't move). */
    const val FIXED_DAMPING: Double = 1.0

    /** Milliseconds per day, for decay calculations. */
    const val MS_PER_DAY: Long = 86_400_000L
}
