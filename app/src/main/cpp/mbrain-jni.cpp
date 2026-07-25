#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "mbrainSDK-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Issue #580 Hardening:
 * - Matched Kotlin 'native' prefix.
 * - Added basic error checking for string operations.
 */

JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_nativeInitMbrain(JNIEnv* env, jclass clazz, jstring deviceId, jint flags) {
    if (deviceId == nullptr) {
        LOGE("initMbrain: deviceId is null");
        return -1;
    }
    const char* nativeDeviceId = env->GetStringUTFChars(deviceId, nullptr);
    if (nativeDeviceId != nullptr) {
        LOGI("Initializing mbrainSDK for device: %s with flags: %d", nativeDeviceId, flags);
        env->ReleaseStringUTFChars(deviceId, nativeDeviceId);
    } else {
        LOGE("initMbrain: Failed to get native string");
        return -2;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_nativePunchHardware(JNIEnv* env, jclass clazz) {
    // Audit: Simple logging call, no stateful allocations or signals that could collide at native level.
    // Collision prevention is handled at the JVM level via ReentrantLock.
    LOGI("mbrainSDK: Hardware punch triggered");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_nativeSetPowerBudget(JNIEnv* env, jclass clazz, jint budgetLevel) {
    LOGI("mbrainSDK: Setting power budget to %d", budgetLevel);
    return 0;
}

}
