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

}
