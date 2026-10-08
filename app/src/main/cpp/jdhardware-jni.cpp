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

// Pressure Hysteresis States (Issue #SIMP-1013-1, #SIMP-1013-2)
static int g_currentMemoryLevel = 0; // 0: Normal, 1: High, 2: Critical
static int g_currentStorageLevel = 0; // 0: Normal, 1: Low, 2: Critical

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
    g_stationaryStartRt = 0;
    g_currentMemoryLevel = 0;
    g_currentStorageLevel = 0;
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

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n13(JNIEnv* env, jclass clazz, jdouble currentFloor, jdouble vibration, jint isWarming, jdouble cpuLoad) {
    if (std::isnan(vibration) || vibration <= 0.0 || cpuLoad > 0.85) return currentFloor;
    double alpha = 0.0;
    if (vibration < currentFloor) {
        alpha = (isWarming ? 0.5 : 0.1);
    } else if (vibration < 1.0) {
        alpha = (isWarming ? 0.1 : 0.01);
    }
    return (currentFloor * (1.0 - alpha)) + (vibration * alpha);
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n14(JNIEnv* env, jclass clazz, jdouble lastHpfValue, jdouble currentRawVibe, jdouble lastRawVibe) {
    return 0.9 * (lastHpfValue + currentRawVibe - lastRawVibe);
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n15(JNIEnv* env, jclass clazz, jdouble currentEnergy, jdouble hpfValue) {
    return (currentEnergy * 0.9) + (std::abs(hpfValue) * 0.1);
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n16(JNIEnv* env, jclass clazz, jdouble x, jdouble y, jdouble z, jdouble lx, jdouble ly, jdouble lz) {
    double dx = x - lx, dy = y - ly, dz = z - lz;
    return std::sqrt(dx * dx + dy * dy + dz * dz) / 9.80665;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n17(JNIEnv* env, jclass clazz, jdouble peak, jdouble floor, jfloat sens, jdouble cpu) {
    double loadFactor = (cpu > 0.85) ? 1.5 : 1.0;
    double baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * 7.0 * loadFactor);
    return (peak > dynamicThreshold) ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n18(JNIEnv* env, jclass clazz, jdouble vibe, jdouble floor, jfloat sens, jdouble cpu) {
    double loadFactor = (cpu > 0.85) ? 1.5 : 1.0;
    double baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * 2.5 * loadFactor);
    return (vibe > dynamicThreshold) ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n19(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

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
    double snr = *(double*)(ptr + 92);
    double thermal = *(double*)(ptr + 100);
    double heapMb = *(double*)(ptr + 108);
    int64_t nowRt = *(int64_t*)(ptr + 116);

    double dx = x - lx, dy = y - ly, dz = z - lz;
    double delta = std::sqrt(dx * dx + dy * dy + dz * dz) / 9.80665;

    double nextFloor = floor;
    if (!std::isnan(delta) && delta > 0.0 && cpu <= 0.85) {
        double alpha = 0.0;
        if (delta < floor) { alpha = (isWarming ? 0.5 : 0.1); }
        else if (delta < 1.0) { alpha = (isWarming ? 0.1 : 0.01); }
        nextFloor = (floor * (1.0 - alpha)) + (delta * alpha);
    }

    double nextHpf = 0.9 * (lastHpf + delta - lastRaw);
    double nextEnergy = (energy * 0.9) + (std::abs(nextHpf) * 0.1);

    double loadFactor = (cpu > 0.85) ? 2.0 : 1.0;
    double dynamicGate = nextFloor * 1.5 * loadFactor;
    double lower = 0.05, upper = 0.12 * loadFactor;
    if (dynamicGate < lower) dynamicGate = lower;
    if (dynamicGate > upper) dynamicGate = upper;
    int isStationary = (delta < dynamicGate) ? 1 : 0;

    int isSuspiciousNoise = (snr > 0.0 && snr < 20.0 && delta > 0.5) ? 1 : 0;
    int isMemoryThrottled = (heapMb > 256.0) ? 1 : 0;

    int64_t stationaryDuration = 0;
    int muzzleResetTriggered = 0;
    if (isStationary) {
        if (g_stationaryStartRt == 0) g_stationaryStartRt = nowRt;
        stationaryDuration = nowRt - g_stationaryStartRt;
        if (stationaryDuration > MUZZLE_HYSTERESIS_MS) { muzzleResetTriggered = 1; }
    } else { g_stationaryStartRt = 0; }

    int isJammingCandidate = 0;
    if (snr > 0.0 && snr < 18.0) { if (delta < 0.15) { isJammingCandidate = 1; } }

    *(double*)(ptr + 128) = delta;
    *(double*)(ptr + 136) = nextFloor;
    *(double*)(ptr + 144) = nextHpf;
    *(double*)(ptr + 152) = nextEnergy;
    *(int*)(ptr + 160) = isStationary;
    *(int*)(ptr + 164) = isSuspiciousNoise;
    *(int*)(ptr + 168) = isMemoryThrottled;
    *(int64_t*)(ptr + 172) = stationaryDuration;
    *(int*)(ptr + 180) = muzzleResetTriggered;
    *(int*)(ptr + 184) = isJammingCandidate;

    return 0;
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n20(JNIEnv* env, jclass clazz, jdouble baseAlpha, jdouble vibrationRollingSum) {
    double factor = 1.0;
    if (vibrationRollingSum > 0.5) {
        factor = std::max(0.01, 1.0 - ((vibrationRollingSum - 0.5) / 1.0));
    }
    return baseAlpha * factor;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n21(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 856) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;
    int count = *(int*)ptr;
    if (count < 0) return -2;
    if (count > 64) count = 64;

    int* svids = (int*)(ptr + 4);
    float* cn0s = (float*)(ptr + 4 + 64 * 4);
    uint8_t* usedInFix = (uint8_t*)(ptr + 4 + 64 * 4 + 64 * 4);

    int usedCount = 0;
    double sumSnr = 0.0;
    for (int i = 0; i < count; i++) {
        if (usedInFix[i]) {
            usedCount++;
            sumSnr += cn0s[i];
        }
    }

    *(int*)(ptr + 840) = count;
    *(int*)(ptr + 844) = usedCount;
    *(double*)(ptr + 848) = (usedCount > 0) ? (sumSnr / usedCount) : 0.0;

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n22(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 1024) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    int readCount = *(int*)(ptr + 0);
    double baseAlpha = *(double*)(ptr + 4);
    double vibeSum = *(double*)(ptr + 12);
    int64_t nowRt = *(int64_t*)(ptr + 20);
    int isWarming = *(int*)(ptr + 28);
    int16_t* samples = (int16_t*)(ptr + 32);

    int maxAmp = 0;
    double sumSq = 0.0;
    int limit = std::min(readCount, 496);
    for (int i = 0; i < limit; i++) {
        int amp = std::abs(samples[i]);
        if (amp > maxAmp) maxAmp = amp;
        sumSq += (double)samples[i] * samples[i];
    }

    double rms = std::sqrt(sumSq / (limit > 0 ? limit : 1));
    double db = 20.0 * std::log10(rms > 1.0 ? rms : 1.0);

    *(int*)(ptr + 1000) = maxAmp;
    *(double*)(ptr + 1004) = db;
    *(int*)(ptr + 1012) = 0;
    *(int64_t*)(ptr + 1016) = 0;

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n23(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    double dist = *(double*)(ptr + 0);
    double maxRange = *(double*)(ptr + 8);
    int64_t nowRt = *(int64_t*)(ptr + 16);
    int isStationary = *(int*)(ptr + 24);
    int64_t stationaryDurationMs = *(int64_t*)(ptr + 28);
    int isHighLoad = *(int*)(ptr + 36);
    double currentIdx = *(double*)(ptr + 40);
    int rawNear = *(int*)(ptr + 48);
    int isFlickering = *(int*)(ptr + 52);

    double nextIdx = currentIdx;
    if (rawNear) { nextIdx = 0.0; } else { nextIdx = 1.0; }

    int64_t debounceMs = isStationary ? 5000 : 1000;
    if (isHighLoad) debounceMs *= 2;

    *(double*)(ptr + 128) = nextIdx;
    *(int*)(ptr + 136) = rawNear;
    *(int64_t*)(ptr + 140) = debounceMs;

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n24(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    double heapMb = *(double*)(ptr + 0);
    double pressureThresholdMb = *(double*)(ptr + 8);
    double criticalThresholdMb = *(double*)(ptr + 16);
    double hysteresisOffsetMb = *(double*)(ptr + 24);

    int nextLevel = g_currentMemoryLevel;
    bool needsFlush = false;

    if (heapMb >= criticalThresholdMb) {
        if (g_currentMemoryLevel < 2) { nextLevel = 2; needsFlush = true; }
    } else if (heapMb >= pressureThresholdMb) {
        if (g_currentMemoryLevel < 1) { nextLevel = 1; needsFlush = true; }
    }

    if (g_currentMemoryLevel == 2) {
        if (heapMb < (criticalThresholdMb - hysteresisOffsetMb)) { nextLevel = (heapMb >= pressureThresholdMb) ? 1 : 0; }
    } else if (g_currentMemoryLevel == 1) {
        if (heapMb < (pressureThresholdMb - hysteresisOffsetMb)) { nextLevel = 0; }
    }

    g_currentMemoryLevel = nextLevel;
    *(int*)(ptr + 128) = g_currentMemoryLevel;
    *(int*)(ptr + 132) = (needsFlush ? 1 : 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n25(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    double availableMb = *(double*)(ptr + 0);
    double lowThresholdMb = *(double*)(ptr + 8);
    double criticalThresholdMb = *(double*)(ptr + 16);
    double hysteresisOffsetMb = *(double*)(ptr + 24);

    int nextLevel = g_currentStorageLevel;
    bool needsPrune = false;

    if (availableMb <= criticalThresholdMb) {
        if (g_currentStorageLevel < 2) { nextLevel = 2; needsPrune = true; }
    } else if (availableMb <= lowThresholdMb) {
        if (g_currentStorageLevel < 1) { nextLevel = 1; needsPrune = true; }
    }

    if (g_currentStorageLevel == 2) {
        if (availableMb > (criticalThresholdMb + hysteresisOffsetMb)) { nextLevel = (availableMb <= lowThresholdMb) ? 1 : 0; }
    } else if (g_currentStorageLevel == 1) {
        if (availableMb > (lowThresholdMb + hysteresisOffsetMb)) { nextLevel = 0; }
    }

    g_currentStorageLevel = nextLevel;
    *(int*)(ptr + 128) = g_currentStorageLevel;
    *(int*)(ptr + 132) = (needsPrune ? 1 : 0);
    return 0;
}

/**
 * n26: processSystemPressure (Issue #SIMP-1014-2)
 * Unified evaluation of memory and storage pressure in a single JNI crossing.
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n26(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Memory Inputs
    double heapMb = *(double*)(ptr + 0);
    double memPressureThresholdMb = *(double*)(ptr + 8);
    double memCriticalThresholdMb = *(double*)(ptr + 16);
    double memHysteresisOffsetMb = *(double*)(ptr + 24);

    // Storage Inputs
    double storageAvailableMb = *(double*)(ptr + 32);
    double storageLowThresholdMb = *(double*)(ptr + 40);
    double storageCriticalThresholdMb = *(double*)(ptr + 48);
    double storageHysteresisOffsetMb = *(double*)(ptr + 56);

    // 1. Evaluate Memory
    int nextMemLevel = g_currentMemoryLevel;
    bool needsMemFlush = false;

    if (heapMb >= memCriticalThresholdMb) {
        if (g_currentMemoryLevel < 2) { nextMemLevel = 2; needsMemFlush = true; }
    } else if (heapMb >= memPressureThresholdMb) {
        if (g_currentMemoryLevel < 1) { nextMemLevel = 1; needsMemFlush = true; }
    }

    if (g_currentMemoryLevel == 2) {
        if (heapMb < (memCriticalThresholdMb - memHysteresisOffsetMb)) {
            nextMemLevel = (heapMb >= memPressureThresholdMb) ? 1 : 0;
        }
    } else if (g_currentMemoryLevel == 1) {
        if (heapMb < (memPressureThresholdMb - memHysteresisOffsetMb)) { nextMemLevel = 0; }
    }
    g_currentMemoryLevel = nextMemLevel;

    // 2. Evaluate Storage
    int nextStorageLevel = g_currentStorageLevel;
    bool needsStoragePrune = false;

    if (storageAvailableMb <= storageCriticalThresholdMb) {
        if (g_currentStorageLevel < 2) { nextStorageLevel = 2; needsStoragePrune = true; }
    } else if (storageAvailableMb <= storageLowThresholdMb) {
        if (g_currentStorageLevel < 1) { nextStorageLevel = 1; needsStoragePrune = true; }
    }

    if (g_currentStorageLevel == 2) {
        if (storageAvailableMb > (storageCriticalThresholdMb + storageHysteresisOffsetMb)) {
            nextStorageLevel = (storageAvailableMb <= storageLowThresholdMb) ? 1 : 0;
        }
    } else if (g_currentStorageLevel == 1) {
        if (storageAvailableMb > (storageLowThresholdMb + storageHysteresisOffsetMb)) { nextStorageLevel = 0; }
    }
    g_currentStorageLevel = nextStorageLevel;

    // 3. Write Outputs
    *(int*)(ptr + 128) = g_currentMemoryLevel;
    *(int*)(ptr + 132) = (needsMemFlush ? 1 : 0);
    *(int*)(ptr + 136) = g_currentStorageLevel;
    *(int*)(ptr + 140) = (needsStoragePrune ? 1 : 0);

    return 0;
}

}
