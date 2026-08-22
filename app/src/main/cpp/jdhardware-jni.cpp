#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>

#define LOG_TAG "jdHardware-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

static void* g_sharedBufferPtr = nullptr;
static jlong g_sharedBufferSize = 0;

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

    int32_t heartbeat;
    int32_t flags;

    memcpy(&heartbeat, g_sharedBufferPtr, sizeof(int32_t));
    memcpy(&flags, (char*)g_sharedBufferPtr + sizeof(int32_t), sizeof(int32_t));

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
 * n6: nativeRelease (Issue #249 Remediation)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n6(JNIEnv* env, jclass clazz) {
    LOGI("jdHardware: n6 release triggered. Clearing native pointers.");
    g_sharedBufferPtr = nullptr;
    g_sharedBufferSize = 0;
    return 0;
}

}
