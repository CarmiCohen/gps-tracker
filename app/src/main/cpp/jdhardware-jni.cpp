#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>
#include <cstdint>
#include <cmath>
#include <algorithm>

#define LOG_TAG "jdHardware-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

static void* g_sharedBufferPtr = nullptr;
static jlong g_sharedBufferSize = 0;

// Pulse Audit State
static int64_t g_accelAuditStartRt = 0;
static int32_t g_accelEventCount = 0;
static double g_lastCalculatedHz = 0.0;

// Muzzle Hysteresis State (Issue #SIMP-1010-2)
static int64_t g_stationaryStartRt = 0;
static const int64_t MUZZLE_HYSTERESIS_MS = 2000;

// FastPath State (Issue #1176)
struct FastPathConfig {
    double baseline;
    double threshold;
    double minThreshold;
    int64_t debounceMs;
    int64_t lastTriggerRt;
};
static FastPathConfig g_fastPathConfigs[3] = {};

/**
 * n1: nativeRegisterSharedBuffer
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n1(JNIEnv* env, jclass clazz, jobject buffer) {
    if (buffer == nullptr) return -1;
    g_sharedBufferPtr = env->GetDirectBufferAddress(buffer);
    g_sharedBufferSize = env->GetDirectBufferCapacity(buffer);
    LOGI("jdHardware: n1 registered at %p", g_sharedBufferPtr);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n2(JNIEnv* env, jclass clazz) {
    return (g_sharedBufferPtr != nullptr) ? 0 : -1;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n3(JNIEnv* env, jclass clazz, jstring deviceId, jint flags) {
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n6(JNIEnv* env, jclass clazz) {
    g_sharedBufferPtr = nullptr;
    g_sharedBufferSize = 0;
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n7(JNIEnv* env, jclass clazz, jlong nowRt) {
    if (g_accelAuditStartRt == 0) g_accelAuditStartRt = nowRt;
    g_accelEventCount++;
    if (nowRt - g_accelAuditStartRt >= 1000) {
        double durationSec = (nowRt - g_accelAuditStartRt) / 1000.0;
        if (durationSec > 0) g_lastCalculatedHz = g_accelEventCount / durationSec;
    }
    return 0;
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n8(JNIEnv* env, jclass clazz) {
    return (jdouble)g_lastCalculatedHz;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n9(JNIEnv* env, jclass clazz) {
    g_accelAuditStartRt = 0;
    g_accelEventCount = 0;
    g_lastCalculatedHz = 0.0;
    g_stationaryStartRt = 0; // Oct.7.9: Reset muzzle hysteresis state
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n10(JNIEnv* env, jclass clazz, jint type, jdouble baseline, jdouble threshold, jdouble minThreshold, jlong debounceMs) {
    if (type < 0 || type >= 3) return -1;
    g_fastPathConfigs[type] = {baseline, threshold, minThreshold, debounceMs, 0};
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n11(JNIEnv* env, jclass clazz, jint type, jdouble value, jlong nowRt, jdouble alpha) {
    if (type < 0 || type >= 3) return 0;
    FastPathConfig& cfg = g_fastPathConfigs[type];
    cfg.baseline = (cfg.baseline * (1.0 - alpha)) + (value * alpha);
    double diff = std::abs(value - cfg.baseline);
    double dynamicThreshold = std::max(cfg.minThreshold, cfg.threshold);
    if (diff > dynamicThreshold && (nowRt - cfg.lastTriggerRt) > cfg.debounceMs) {
        cfg.lastTriggerRt = nowRt;
        return 1;
    }
    return 0;
}

/**
 * n12: isStationaryNative (Issue #SIMP-1510-1)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n12(JNIEnv* env, jclass clazz, jdouble vibration, jdouble adaptiveFloor, jdouble cpuLoad) {
    double loadFactor = (cpuLoad > 0.85) ? 2.0 : 1.0;
    double dynamicGate = adaptiveFloor * 1.5 * loadFactor;
    double lower = 0.05;
    double upper = 0.12 * loadFactor;
    if (dynamicGate < lower) dynamicGate = lower;
    if (dynamicGate > upper) dynamicGate = upper;
    return (vibration < dynamicGate) ? 1 : 0;
}

/**
 * n13: updateVibrationFloorNative (Issue #SIMP-1510-1)
 * Aligned with SentinelValidator.kt coefficients (Oct.5 Hardening).
 */
JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n13(JNIEnv* env, jclass clazz, jdouble currentFloor, jdouble vibration, jint isWarming, jdouble cpuLoad) {
    if (std::isnan(vibration) || vibration <= 0.0 || cpuLoad > 0.85) return currentFloor;
    double alpha = 0.0;
    if (vibration < currentFloor) {
        alpha = (isWarming ? 0.5 : 0.1); // VIBRATION_EMA_DOWN_FAST = 0.1
    } else if (vibration < 1.0) {
        alpha = (isWarming ? 0.1 : 0.01); // VIBRATION_EMA_UP_FAST = 0.01
    }
    return (currentFloor * (1.0 - alpha)) + (vibration * alpha);
}

/**
 * n14: computeNextHpfNative (VIBRATION_HPF_ALPHA = 0.9)
 */
JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n14(JNIEnv* env, jclass clazz, jdouble lastHpfValue, jdouble currentRawVibe, jdouble lastRawVibe) {
    return 0.9 * (lastHpfValue + currentRawVibe - lastRawVibe);
}

/**
 * n15: computeNextEnergyNative (VIBRATION_ENERGY_EMA_ALPHA = 0.1)
 */
JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n15(JNIEnv* env, jclass clazz, jdouble currentEnergy, jdouble hpfValue) {
    return (currentEnergy * 0.9) + (std::abs(hpfValue) * 0.1);
}

/**
 * n16: calculateVibrationDeltaNative
 */
JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n16(JNIEnv* env, jclass clazz, jdouble x, jdouble y, jdouble z, jdouble lx, jdouble ly, jdouble lz) {
    double dx = x - lx, dy = y - ly, dz = z - lz;
    return std::sqrt(dx * dx + dy * dy + dz * dz) / 9.80665;
}

/**
 * n17: isShockViolatedNative
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n17(JNIEnv* env, jclass clazz, jdouble peak, jdouble floor, jfloat sens, jdouble cpu) {
    double loadFactor = (cpu > 0.85) ? 1.5 : 1.0;
    double baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * 7.0 * loadFactor);
    return (peak > dynamicThreshold) ? 1 : 0;
}

/**
 * n18: isVibrationSuspiciousNative
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n18(JNIEnv* env, jclass clazz, jdouble vibe, jdouble floor, jfloat sens, jdouble cpu) {
    double loadFactor = (cpu > 0.85) ? 1.5 : 1.0;
    double baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * 2.5 * loadFactor);
    return (vibe > dynamicThreshold) ? 1 : 0;
}

/**
 * n19: processVibrationBatch (Issue #1450)
 * Consolidates all granular vibration math into one call.
 * Oct.7.9: Added muzzle hysteresis offloading (#SIMP-1010-2).
 * Oct.7.6: Expanded with forensic snapshots (snr, thermal, heap) for multi-sensor
 * correlation logic and memory pressure evaluation (#SIMP-1007-16).
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n19(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;

    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Inputs (Offset 0)
    double x = *(double*)(ptr + 0);
    double y = *(double*)(ptr + 8);
    double z = *(double*)(ptr + 16);
    double lx = *(double*)(ptr + 24);
    double ly = *(double*)(ptr + 32);
    double lz = *(double*)(ptr + 40);
    double floor = *(double*)(ptr + 48);
    double cpu = *(double*)(ptr + 56);
    int isWarming = *(int*)(ptr + 64);
    double lastRaw = *(double*)(ptr + 68);
    double lastHpf = *(double*)(ptr + 76);
    double energy = *(double*)(ptr + 84);

    // Oct.7.6 Forensic Expansion (Offset 92)
    double snr = *(double*)(ptr + 92);
    double thermal = *(double*)(ptr + 100);
    double heapMb = *(double*)(ptr + 108);

    // Oct.7.9 Time Context (Offset 116)
    int64_t nowRt = *(int64_t*)(ptr + 116);

    // 1. Delta (n16 equivalent)
    double dx = x - lx, dy = y - ly, dz = z - lz;
    double delta = std::sqrt(dx * dx + dy * dy + dz * dz) / 9.80665;

    // 2. Floor Update (n13 equivalent)
    double nextFloor = floor;
    if (!std::isnan(delta) && delta > 0.0 && cpu <= 0.85) {
        double alpha = 0.0;
        if (delta < floor) {
            alpha = (isWarming ? 0.5 : 0.1);
        } else if (delta < 1.0) {
            alpha = (isWarming ? 0.1 : 0.01);
        }
        nextFloor = (floor * (1.0 - alpha)) + (delta * alpha);
    }

    // 3. HPF (n14 equivalent)
    double nextHpf = 0.9 * (lastHpf + delta - lastRaw);

    // 4. Energy (n15 equivalent)
    double nextEnergy = (energy * 0.9) + (std::abs(nextHpf) * 0.1);

    // 5. Stationary Check (n12 equivalent)
    double loadFactor = (cpu > 0.85) ? 2.0 : 1.0;
    double dynamicGate = nextFloor * 1.5 * loadFactor;
    double lower = 0.05, upper = 0.12 * loadFactor;
    if (dynamicGate < lower) dynamicGate = lower;
    if (dynamicGate > upper) dynamicGate = upper;
    int isStationary = (delta < dynamicGate) ? 1 : 0;

    // 6. Native Anomaly Detection (Oct.7.6)
    int isSuspiciousNoise = (snr > 0.0 && snr < 20.0 && delta > 0.5) ? 1 : 0;
    int isMemoryThrottled = (heapMb > 256.0) ? 1 : 0;

    // 7. Muzzle Hysteresis (Oct.7.9, #SIMP-1010-2)
    int64_t stationaryDuration = 0;
    int muzzleResetTriggered = 0;
    if (isStationary) {
        if (g_stationaryStartRt == 0) g_stationaryStartRt = nowRt;
        stationaryDuration = nowRt - g_stationaryStartRt;
        if (stationaryDuration > MUZZLE_HYSTERESIS_MS) {
            muzzleResetTriggered = 1;
        }
    } else {
        g_stationaryStartRt = 0;
    }

    // Outputs (Offset 128)
    *(double*)(ptr + 128) = delta;
    *(double*)(ptr + 136) = nextFloor;
    *(double*)(ptr + 144) = nextHpf;
    *(double*)(ptr + 152) = nextEnergy;
    *(int*)(ptr + 160) = isStationary;

    // Oct.7.6 Outputs (Offset 164)
    *(int*)(ptr + 164) = isSuspiciousNoise;
    *(int*)(ptr + 168) = isMemoryThrottled;

    // Oct.7.9 Outputs (Offset 172)
    *(int64_t*)(ptr + 172) = stationaryDuration;
    *(int*)(ptr + 180) = muzzleResetTriggered;

    return 0;
}

/**
 * n20: computeAdaptiveAcousticAlphaNative (Issue #SIMP-1010-1)
 */
JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n20(JNIEnv* env, jclass clazz, jdouble baseAlpha, jdouble vibrationRollingSum) {
    double factor = 1.0;
    if (vibrationRollingSum > 0.5) {
        factor = std::max(0.01, 1.0 - ((vibrationRollingSum - 0.5) / 1.0));
    }
    return baseAlpha * factor;
}

}
