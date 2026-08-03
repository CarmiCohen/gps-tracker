#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>

#define LOG_TAG "mbrainSDK-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// Issue #667: Zero-copy shared buffer pointers
static void* g_sharedBufferPtr = nullptr;
static jlong g_sharedBufferSize = 0;

/**
 * nativeRegisterSharedBuffer: Hooks into the JVM's direct buffer to enable
 * zero-allocation state transfer. (Issue #667)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_nativeRegisterSharedBuffer(JNIEnv* env, jclass clazz, jobject buffer) {
    if (buffer == nullptr) {
        LOGE("registerSharedBuffer: Buffer is null");
        return -1;
    }

    g_sharedBufferPtr = env->GetDirectBufferAddress(buffer);
    g_sharedBufferSize = env->GetDirectBufferCapacity(buffer);

    if (g_sharedBufferPtr == nullptr) {
        LOGE("registerSharedBuffer: Failed to get direct buffer address (Buffer must be direct)");
        return -2;
    }

    LOGI("mbrainSDK: Shared buffer registered at %p (Size: %lld)", g_sharedBufferPtr, g_sharedBufferSize);
    return 0;
}

/**
 * nativeSyncState: Reads state from the zero-copy buffer without object allocation.
 * (Issue #667)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_nativeSyncState(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr) return -1;

    // Direct memory access - no JNI boundary crossing overhead for data fields
    int32_t heartbeat;
    int32_t flags;

    memcpy(&heartbeat, g_sharedBufferPtr, sizeof(int32_t));
    memcpy(&flags, (char*)g_sharedBufferPtr + sizeof(int32_t), sizeof(int32_t));

    // Internal hardware sync logic placeholder
    // LOGI("mbrainSDK: Sync - Heartbeat: %d, Flags: 0x%08X", heartbeat, flags);

    return 0;
}

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
    LOGI("mbrainSDK: Hardware punch triggered");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_nativeSetPowerBudget(JNIEnv* env, jclass clazz, jint budgetLevel) {
    LOGI("mbrainSDK: Setting power budget to %d", budgetLevel);
    return 0;
}

}
