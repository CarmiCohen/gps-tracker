#ifndef ENGINE_CONSTANTS_H
#define ENGINE_CONSTANTS_H

/**
 * EngineConstants.h: Shared hardware limits for JNI.
 * MUST be kept in parity with EngineConstants.kt.
 */

// Vibration limits (G)
#define VIBRATION_FLOOR_MIN 0.02
#define VIBRATION_FLOOR_MAX 2.0

// Proximity Debounce (ms)
#define PROXIMITY_DEBOUNCE_STATIONARY_MS 5000
#define PROXIMITY_DEBOUNCE_MOVING_MS 1000
#define PROXIMITY_DEBOUNCE_MAX_MS 15000
#define PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR 2000
#define PROXIMITY_STRESS_SCALING_MULTIPLIER 2.0

// Muzzle Hysteresis
#define MUZZLE_HYSTERESIS_MS_CONST 2000

#endif // ENGINE_CONSTANTS_H
