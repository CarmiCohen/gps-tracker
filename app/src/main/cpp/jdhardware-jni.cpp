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

// Pulse Audit State (Issue #SIMP-1416-1)
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
    if (buffer == nullptr) {
        LOGE("n1: Buffer is null");
        return -1;
    }

    g_sharedBufferPtr = env->GetDirectBufferAddress(buffer);
    g_sharedBufferSize = env->GetDirectBufferCapacity(buffer);

    if (g_sharedBufferPtr == nullptr) {
        LOGE("n1: Failed to get direct buffer address");
        return -2;
    }

    LOGI("jdHardware: n1 registered at %p", g_sharedBufferPtr);
    return 0;
}

/**
 * n2: nativeSyncState
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n2(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr) return -1;
    return 0;
}

/**
 * n3: nativeInit
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n3(JNIEnv* env, jclass clazz, jstring deviceId, jint flags) {
    if (deviceId == nullptr) return -1;
    const char* nativeDeviceId = env->GetStringUTFChars(deviceId, nullptr);
    if (nativeDeviceId != nullptr) {
        LOGI("jdHardware: n3 init for device hash processed");
        env->ReleaseStringUTFChars(deviceId, nativeDeviceId);
    }
    return 0;
}

/**
 * n4: nativePunchHardware
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n4(JNIEnv* env, jclass clazz) {
    return 0;
}

/**
 * n5: nativeSetPowerBudget
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n5(JNIEnv* env, jclass clazz, jint budgetLevel) {
    return 0;
}

/**
 * n6: nativeRelease
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n6(JNIEnv* env, jclass clazz) {
    LOGI("jdHardware: n6 release triggered. Clearing native pointers.");
    g_sharedBufferPtr = nullptr;
    g_sharedBufferSize = 0;
    return 0;
}

/**
 * n7: recordSensorPulse
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n7(JNIEnv* env, jclass clazz, jlong nowRt) {
    if (g_accelAuditStartRt == 0) {
        g_accelAuditStartRt = nowRt;
    }
    g_accelEventCount++;

    if (nowRt - g_accelAuditStartRt >= 1000) {
        double durationSec = (nowRt - g_accelAuditStartRt) / 1000.0;
        if (durationSec > 0) {
            g_lastCalculatedHz = g_accelEventCount / durationSec;
        }
    }
    return 0;
}

/**
 * n8: getSensorAuditHz
 */
JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n8(JNIEnv* env, jclass clazz) {
    return (jdouble)g_lastCalculatedHz;
}

/**
 * n9: resetSensorAudit
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n9(JNIEnv* env, jclass clazz) {
    g_accelAuditStartRt = 0;
    g_accelEventCount = 0;
    g_lastCalculatedHz = 0.0;
    return 0;
}

/**
 * n10: updateFastPathConfig
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n10(JNIEnv* env, jclass clazz, jint type, jdouble baseline, jdouble threshold, jdouble minThreshold, jlong debounceMs) {
    if (type < 0 || type >= 3) return -1;
    g_fastPathConfigs[type].baseline = baseline;
    g_fastPathConfigs[type].threshold = threshold;
    g_fastPathConfigs[type].minThreshold = minThreshold;
    g_fastPathConfigs[type].debounceMs = debounceMs;
    return 0;
}

/**
 * n11: evaluateFastPath
 */
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
 * n12: isStationaryNative
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
 * n13: updateVibrationFloorNative
 */
JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n13(JNIEnv* env, jclass clazz, jdouble currentFloor, jdouble vibration, jint isWarming, jdouble cpuLoad) {
    if (std::isnan(vibration) || vibration <= 0.0 || cpuLoad > 0.85) return currentFloor;

    double alpha = 0.0;
    if (vibration < currentFloor) {
        alpha = isWarming ? 0.5 : 0.01;
    } else if (vibration < 1.0) {
        alpha = isWarming ? 0.1 : 0.001;
    }

    return (currentFloor * (1.0 - alpha)) + (vibration * alpha);
}

}
