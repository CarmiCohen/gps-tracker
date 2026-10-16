#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>
#include <cstdint>
#include <cmath>
#include <algorithm>
#include "EngineConstants.h"

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

// Muzzle Hysteresis State
static int64_t g_stationaryStartRt = 0;
static const int64_t MUZZLE_HYSTERESIS_MS = MUZZLE_HYSTERESIS_MS_VAL;

// System Pressure State
static int g_lastMemLevel = 0;
static int g_lastStorageLevel = 0;

// FastPath State
struct FastPathConfig {
    double baseline;
    double threshold;
    double minThreshold;
    int64_t debounceMs;
    int64_t lastTriggerRt;
    double contractionEma;
    double floorMin;
    int64_t lastBaselineUpdateRt;
};
static FastPathConfig g_fastPathConfigs[3] = {};

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n1(JNIEnv* env, jclass clazz, jobject buffer) {
    if (buffer == nullptr) return -1;
    g_sharedBufferPtr = env->GetDirectBufferAddress(buffer);
    g_sharedBufferSize = env->GetDirectBufferCapacity(buffer);
    LOGI("jdHardware: n1 registered shared buffer");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n2(JNIEnv* env, jclass clazz) {
    return (g_sharedBufferPtr != nullptr) ? 0 : -1;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n3(JNIEnv* env, jclass clazz, jstring deviceId, jint flags) {
    LOGI("jdHardware: n3 native initialization complete");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n4(JNIEnv* env, jclass clazz) {
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n5(JNIEnv* env, jclass clazz, jint budgetLevel) {
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
    g_stationaryStartRt = 0;
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n10(JNIEnv* env, jclass clazz, jint type, jdouble baseline, jdouble threshold, jdouble minThreshold, jlong debounceMs, jdouble contractionEma, jdouble floorMin) {
    if (type < 0 || type >= 3) return -1;
    g_fastPathConfigs[type] = {baseline, threshold, minThreshold, debounceMs, 0, contractionEma, floorMin, 0};
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n11(JNIEnv* env, jclass clazz, jint type, jdouble value, jlong nowRt, jdouble alpha) {
    if (type < 0 || type >= 3) return 0;
    FastPathConfig& cfg = g_fastPathConfigs[type];
    if (std::isnan(value)) return 0;
    if (cfg.baseline < 0 || std::isnan(cfg.baseline)) { cfg.baseline = value; return 0; }
    if (alpha > 0.0 && !std::isnan(alpha)) cfg.baseline = (cfg.baseline * (1.0 - alpha)) + (value * alpha);
    if ((value - cfg.baseline) > cfg.threshold && value >= cfg.minThreshold) {
        if (nowRt - cfg.lastTriggerRt > cfg.debounceMs) {
            cfg.lastTriggerRt = nowRt;
            return 1;
        }
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n12(JNIEnv* env, jclass clazz, jdouble vibration, jdouble adaptiveFloor, jdouble cpuLoad) {
    if (std::isnan(vibration) || std::isnan(adaptiveFloor)) return 1;
    double loadFactor = (cpuLoad > 0.85) ? 2.0 : 1.0;
    double dynamicGate = adaptiveFloor * 1.5 * loadFactor;
    return (vibration < dynamicGate) ? 1 : 0;
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n13(JNIEnv* env, jclass clazz, jdouble currentFloor, jdouble vibration, jint isWarming, jdouble cpuLoad) {
    if (std::isnan(vibration) || std::isnan(currentFloor) || vibration <= 0.0 || cpuLoad > 0.85) {
        return std::isnan(currentFloor) ? VIBRATION_FLOOR_MIN : currentFloor;
    }
    double alpha = (vibration < currentFloor) ? (isWarming ? 0.5 : 0.1) : (vibration < 1.0 ? (isWarming ? 0.1 : 0.01) : 0.0);
    double next = (currentFloor * (1.0 - alpha)) + (vibration * alpha);
    if (std::isnan(next)) return currentFloor;
    return std::max(VIBRATION_FLOOR_MIN, std::min(VIBRATION_FLOOR_MAX, next));
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n14(JNIEnv* env, jclass clazz, jdouble lastHpfValue, jdouble currentRawVibe, jdouble lastRawVibe) {
    if (std::isnan(lastHpfValue)) lastHpfValue = 0.0;
    if (std::isnan(currentRawVibe)) currentRawVibe = 0.0;
    if (std::isnan(lastRawVibe)) lastRawVibe = 0.0;
    return 0.9 * (lastHpfValue + currentRawVibe - lastRawVibe);
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n15(JNIEnv* env, jclass clazz, jdouble currentEnergy, jdouble hpfValue) {
    if (std::isnan(currentEnergy)) currentEnergy = 0.0;
    if (std::isnan(hpfValue)) hpfValue = 0.0;
    return (currentEnergy * 0.9) + (std::abs(hpfValue) * 0.1);
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n16(JNIEnv* env, jclass clazz, jdouble x, jdouble y, jdouble z, jdouble lx, jdouble ly, jdouble lz) {
    if (std::isnan(x) || std::isnan(y) || std::isnan(z) || std::isnan(lx) || std::isnan(ly) || std::isnan(lz)) return 0.0;
    double dx = x - lx, dy = y - ly, dz = z - lz;
    double delta = std::sqrt(dx * dx + dy * dy + dz * dz) / 9.80665;
    return std::isnan(delta) ? 0.0 : delta;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n17(JNIEnv* env, jclass clazz, jdouble peak, jdouble floor, jfloat sens, jdouble cpu) {
    if (std::isnan(peak) || std::isnan(floor)) return 0;
    double loadFactor = (cpu > 0.85) ? 1.5 : 1.0;
    double baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - (double)sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * 7.0 * loadFactor);
    return (peak > dynamicThreshold) ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n18(JNIEnv* env, jclass clazz, jdouble vibe, jdouble floor, jfloat sens, jdouble cpu) {
    if (std::isnan(vibe) || std::isnan(floor)) return 0;
    double loadFactor = (cpu > 0.85) ? 1.5 : 1.0;
    double baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - (double)sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * 2.5 * loadFactor);
    return (vibe > dynamicThreshold) ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n19(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Aligned Inputs (matching Kotlin Oct.16.10 offsets)
    double x = *(double*)(ptr + 0), y = *(double*)(ptr + 8), z = *(double*)(ptr + 16);
    double lx = *(double*)(ptr + 24), ly = *(double*)(ptr + 32), lz = *(double*)(ptr + 40);
    double floor = *(double*)(ptr + 48), cpu = *(double*)(ptr + 56);
    int isWarming = *(int*)(ptr + 64);
    // 68: Padding
    double lastRaw = *(double*)(ptr + 72), lastHpf = *(double*)(ptr + 80), energy = *(double*)(ptr + 88);
    double snr = *(double*)(ptr + 96), thermal = *(double*)(ptr + 104), heap = *(double*)(ptr + 112);
    int64_t nowRt = *(int64_t*)(ptr + 120);

    if (std::isnan(x)) x = 0.0; if (std::isnan(y)) y = 0.0; if (std::isnan(z)) z = 0.0;
    if (std::isnan(lx)) lx = 0.0; if (std::isnan(ly)) ly = 0.0; if (std::isnan(lz)) lz = 0.0;
    if (std::isnan(floor)) floor = VIBRATION_FLOOR_MIN;
    if (std::isnan(lastRaw)) lastRaw = 0.0;
    if (std::isnan(lastHpf)) lastHpf = 0.0;
    if (std::isnan(energy)) energy = 0.0;

    double dx = x - lx, dy = y - ly, dz = z - lz;
    double delta = std::sqrt(dx * dx + dy * dy + dz * dz) / 9.80665;
    if (std::isnan(delta)) delta = 0.0;

    double nextFloor = floor;
    if (delta > 0.0 && cpu <= 0.85) {
        double alpha = (delta < floor) ? (isWarming ? 0.5 : 0.1) : (delta < 1.0 ? (isWarming ? 0.1 : 0.01) : 0.0);
        nextFloor = (floor * (1.0 - alpha)) + (delta * alpha);
        if (std::isnan(nextFloor)) nextFloor = floor;
        nextFloor = std::max(VIBRATION_FLOOR_MIN, std::min(VIBRATION_FLOOR_MAX, nextFloor));
    }
    double nextHpf = 0.9 * (lastHpf + delta - lastRaw);
    if (std::isnan(nextHpf)) nextHpf = 0.0;

    double nextEnergy = (energy * 0.9) + (std::abs(nextHpf) * 0.1);
    if (std::isnan(nextEnergy)) nextEnergy = 0.0;

    double dynamicGate = nextFloor * 1.5 * ((cpu > 0.85) ? 2.0 : 1.0);
    int isStationary = (delta < dynamicGate) ? 1 : 0;
    if (isStationary) {
        if (g_stationaryStartRt == 0) g_stationaryStartRt = nowRt;
    } else { g_stationaryStartRt = 0; }

    // Aligned Outputs
    *(double*)(ptr + 128) = delta;
    *(double*)(ptr + 136) = nextFloor;
    *(double*)(ptr + 144) = nextHpf;
    *(double*)(ptr + 152) = nextEnergy;
    *(int*)(ptr + 160) = isStationary;
    *(int*)(ptr + 164) = (snr > 0.0 && snr < 22.0 && delta > 0.5);
    *(int*)(ptr + 168) = 0; // isMemoryPressureThrottled placeholder
    // 172: Padding
    *(int64_t*)(ptr + 176) = (isStationary && g_stationaryStartRt > 0) ? (nowRt - g_stationaryStartRt) : 0;
    *(int*)(ptr + 184) = 0; // muzzleResetTriggered placeholder
    *(int*)(ptr + 188) = 0; // isJammingCandidate placeholder

    return 0;
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n20(JNIEnv* env, jclass clazz, jdouble baseAlpha, jdouble vibrationRollingSum) {
    if (std::isnan(baseAlpha)) baseAlpha = 0.01;
    if (std::isnan(vibrationRollingSum)) vibrationRollingSum = 0.0;
    double factor = (vibrationRollingSum > 0.5) ? std::max(0.01, 1.0 - ((vibrationRollingSum - 0.5) / 1.0)) : 1.0;
    return baseAlpha * factor;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n21(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 1040) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;
    int count = std::min(*(int*)(ptr + 0), 64), usedCount = 0, snrCount = 0;
    float* cn0s = (float*)(ptr + 260); int* usedInFix = (int*)(ptr + 516); double snrSum = 0.0;
    for (int i = 0; i < count; i++) {
        if (usedInFix[i] != 0) usedCount++;
        if (cn0s[i] > 0.0f && !std::isnan(cn0s[i])) { snrSum += cn0s[i]; snrCount++; }
    }
    *(int*)(ptr + 1040) = count; *(int*)(ptr + 1044) = usedCount;
    *(double*)(ptr + 1048) = (snrCount > 0) ? (snrSum / snrCount) : 0.0;
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n22(JNIEnv* env, jclass clazz, jshortArray buffer) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 128) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Aligned Inputs
    int readCount = *(int*)(ptr + 0);
    // 4: Padding
    double baseAlpha = *(double*)(ptr + 8);
    double vibeRolling = *(double*)(ptr + 16);
    int64_t nowRt = *(int64_t*)(ptr + 24);
    int isWarming = *(int*)(ptr + 32);

    jshort* samples = (jshort*)env->GetPrimitiveArrayCritical(buffer, nullptr);
    int maxAmp = 0;
    for (int i = 0; i < readCount; i++) maxAmp = std::max(maxAmp, std::abs((int)samples[i]));
    env->ReleasePrimitiveArrayCritical(buffer, samples, JNI_ABORT);

    double db = (maxAmp > 0) ? (20.0 * std::log10((double)maxAmp)) : 0.0;
    if (std::isnan(db)) db = 0.0;

    // Aligned Outputs
    *(int*)(ptr + 128) = maxAmp;
    // 132 Padding
    *(double*)(ptr + 136) = db;
    // Further outputs (isSpike, lastSpikeRt) are handled by Kotlin for now or added here

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n23(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Aligned Inputs
    double dist = *(double*)(ptr + 0), maxRange = *(double*)(ptr + 8);
    int64_t nowRt = *(int64_t*)(ptr + 16);
    int isStationary = *(int*)(ptr + 24);
    // 28: Padding
    int64_t stationaryDuration = *(int64_t*)(ptr + 32);
    int isHighLoad = *(int*)(ptr + 40);
    // 44: Padding
    double currentIdx = *(double*)(ptr + 48);
    int rawNear = *(int*)(ptr + 56), isFlickering = *(int*)(ptr + 60);

    if (std::isnan(dist)) dist = maxRange + 1.0;
    if (std::isnan(currentIdx)) currentIdx = 0.0;

    int newValue = (dist < maxRange);
    double rawIdx = std::max(0.0, std::min(1.0, 1.0 - (dist / maxRange)));
    double nextIdx = (currentIdx * (1.0 - PROXIMITY_EMA_ALPHA)) + (rawIdx * PROXIMITY_EMA_ALPHA);
    if (std::isnan(nextIdx)) nextIdx = currentIdx;

    int nextRawNear = rawNear;
    int64_t debounceMs = 0;

    if (newValue != rawNear) {
        if (!newValue && isFlickering && isStationary) {
            nextRawNear = rawNear;
        } else {
            nextRawNear = newValue;
            debounceMs = isStationary ? PROXIMITY_DEBOUNCE_STATIONARY_MS : PROXIMITY_DEBOUNCE_MOVING_MS;
            if (isStationary && stationaryDuration > 0) {
                debounceMs += (int64_t)((stationaryDuration / 3600000.0) * PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR);
            }
            if (isHighLoad) debounceMs = (int64_t)(debounceMs * PROXIMITY_STRESS_SCALING_MULTIPLIER);
            debounceMs = std::min(debounceMs, (int64_t)PROXIMITY_DEBOUNCE_MAX_MS);
        }
    }

    // Aligned Outputs
    *(double*)(ptr + 128) = nextIdx;
    *(int*)(ptr + 136) = nextRawNear;
    // 140 Padding
    *(int64_t*)(ptr + 144) = debounceMs;

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n24(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 128) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Aligned Inputs
    double heapMb = *(double*)(ptr + 0);
    double memPressure = *(double*)(ptr + 8);
    double memCritical = *(double*)(ptr + 16);
    double hysteresis = *(double*)(ptr + 24);

    if (std::isnan(heapMb)) return 0;

    int currentMem = (heapMb >= memCritical) ? 2 : (heapMb >= memPressure ? 1 : 0);
    if (currentMem < g_lastMemLevel && heapMb > ((g_lastMemLevel == 2 ? memCritical : memPressure) - hysteresis)) {
        currentMem = g_lastMemLevel;
    }

    *(int*)(ptr + 64) = currentMem;
    *(int*)(ptr + 68) = (currentMem != 0 && currentMem != g_lastMemLevel);
    g_lastMemLevel = currentMem;

    return 0;
}

}
