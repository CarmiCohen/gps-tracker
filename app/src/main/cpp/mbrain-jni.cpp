#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "mbrainSDK-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_initMbrain(JNIEnv* env, jobject thiz, jstring deviceId, jint flags) {
    const char* nativeDeviceId = env->GetStringUTFChars(deviceId, nullptr);
    if (nativeDeviceId != nullptr) {
        LOGI("Initializing mbrainSDK for device: %s with flags: %d", nativeDeviceId, flags);
        env->ReleaseStringUTFChars(deviceId, nativeDeviceId);
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_punchHardware(JNIEnv* env, jobject thiz) {
    LOGI("mbrainSDK: Hardware punch triggered");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_MbrainHardwareManager_setPowerBudget(JNIEnv* env, jobject thiz, jint budgetLevel) {
    LOGI("mbrainSDK: Setting power budget to %d", budgetLevel);
    return 0;
}

}
