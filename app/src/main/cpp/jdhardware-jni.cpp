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

// System Pressure State (Issue #SIMP-1014-2)
static int g_lastMemLevel = 0;
static int g_lastStorageLevel = 0;

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
    LOGI("jdHardware: n1 registered at %p with size %lld", g_sharedBufferPtr, (long long)g_sharedBufferSize);
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
    g_lastMemLevel = 0;
    g_lastStorageLevel = 0;
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
    if (cfg.baseline < 0) { cfg.baseline = value; return 0; }

    if (alpha > 0.0) cfg.baseline = (cfg.baseline * (1.0 - alpha)) + (value * alpha);

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
    double loadFactor = (cpuLoad > 0.85) ? 2.0 : 1.0;
    double dynamicGate = adaptiveFloor * 1.5 * loadFactor;
    if (dynamicGate < 0.05) dynamicGate = 0.05;
    if (dynamicGate > 0.12 * loadFactor) dynamicGate = 0.12 * loadFactor;
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
    double baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - (double)sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * 7.0 * loadFactor);
    return (peak > dynamicThreshold) ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n18(JNIEnv* env, jclass clazz, jdouble vibe, jdouble floor, jfloat sens, jdouble cpu) {
    double loadFactor = (cpu > 0.85) ? 1.5 : 1.0;
    double baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - (double)sens)) * loadFactor;
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
        double alpha = (delta < floor) ? (isWarming ? 0.5 : 0.1) : (delta < 1.0 ? (isWarming ? 0.1 : 0.01) : 0.0);
        nextFloor = (floor * (1.0 - alpha)) + (delta * alpha);
    }

    double nextHpf = 0.9 * (lastHpf + delta - lastRaw);
    double nextEnergy = (energy * 0.9) + (std::abs(nextHpf) * 0.1);

    double loadFactor = (cpu > 0.85) ? 2.0 : 1.0;
    double dynamicGate = nextFloor * 1.5 * loadFactor;
    if (dynamicGate < 0.05) dynamicGate = 0.05;
    if (dynamicGate > 0.12 * loadFactor) dynamicGate = 0.12 * loadFactor;
    int isStationary = (delta < dynamicGate) ? 1 : 0;

    // Oct.10.10: Aligned SNR threshold with EngineConstants.JUMP_GATE_LOW_SNR_THRESHOLD (22.0)
    int isSuspiciousNoise = (snr > 0.0 && snr < 22.0 && delta > 0.5) ? 1 : 0;
    int isMemoryThrottled = (heapMb > 256.0) ? 1 : 0;

    int64_t stationaryDuration = 0;
    int muzzleResetTriggered = 0;
    if (isStationary) {
        if (g_stationaryStartRt == 0) g_stationaryStartRt = nowRt;
        stationaryDuration = nowRt - g_stationaryStartRt;
        if (stationaryDuration > MUZZLE_HYSTERESIS_MS) muzzleResetTriggered = 1;
    } else {
        g_stationaryStartRt = 0;
    }

    // Oct.10.10: Aligned SNR threshold with EngineConstants.JUMP_GATE_LOW_SNR_THRESHOLD (22.0)
    int isJammingCandidate = (snr > 0.0 && snr < 22.0 && delta < 0.15) ? 1 : 0;

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

/**
 * n21: processGnssBatch (Issue #SIMP-1011-1)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n21(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 1040) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    int count = *(int*)(ptr + 0);
    if (count > 64) count = 64;

    int usedCount = 0;
    double snrSum = 0.0;
    int snrCount = 0;

    int* svids = (int*)(ptr + 4);
    float* cn0s = (float*)(ptr + 260);
    int* usedInFix = (int*)(ptr + 516);
    // int* constellations = (int*)(ptr + 772); // Not currently used for logic

    for (int i = 0; i < count; i++) {
        if (usedInFix[i] != 0) usedCount++;
        float snr = cn0s[i];
        if (snr > 0.0f) {
            snrSum += snr;
            snrCount++;
        }
    }

    *(int*)(ptr + 1040) = count;
    *(int*)(ptr + 1044) = usedCount;
    *(double*)(ptr + 1048) = (snrCount > 0) ? (snrSum / snrCount) : 0.0;

    return 0;
}

/**
 * n22: processAcousticBatch (Issue #SIMP-1011-2)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n22(JNIEnv* env, jclass clazz, jshortArray buffer) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 128) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    int readCount = *(int*)(ptr + 0);
    double baseAlpha = *(double*)(ptr + 4);
    double vibeSum = *(double*)(ptr + 12);
    int64_t nowRt = *(int64_t*)(ptr + 20);
    int isWarming = *(int*)(ptr + 28);

    jshort* samples = env->GetShortArrayElements(buffer, nullptr);
    if (samples == nullptr) return -2;

    int maxAmp = 0;
    for (int i = 0; i < readCount; i++) {
        int amp = std::abs((int)samples[i]);
        if (amp > maxAmp) maxAmp = amp;
    }
    env->ReleaseShortArrayElements(buffer, samples, JNI_ABORT);

    double db = (maxAmp > 0) ? (20.0 * std::log10((double)maxAmp)) : 0.0;

    // Adaptive Alpha (n20 logic)
    double factor = 1.0;
    if (vibeSum > 0.5) factor = std::max(0.01, 1.0 - ((vibeSum - 0.5) / 1.0));
    double alpha = baseAlpha * factor;

    // FastPath Acoustic Evaluation (n11 logic)
    int isSpike = 0;
    FastPathConfig& cfg = g_fastPathConfigs[0]; // FASTPATH_ACOUSTIC = 0
    if (cfg.baseline < 0) {
        cfg.baseline = db;
    } else {
        double effectiveAlpha = (isWarming != 0) ? 0.0 : alpha;
        cfg.baseline = (cfg.baseline * (1.0 - effectiveAlpha)) + (db * effectiveAlpha);
        if ((db - cfg.baseline) > cfg.threshold && db >= cfg.minThreshold) {
            if (nowRt - cfg.lastTriggerRt > cfg.debounceMs) {
                cfg.lastTriggerRt = nowRt;
                isSpike = 1;
            }
        }
    }

    *(int*)(ptr + 128) = maxAmp;
    *(double*)(ptr + 132) = db;
    *(int*)(ptr + 140) = isSpike;
    *(int64_t*)(ptr + 144) = cfg.lastTriggerRt;

    return 0;
}

/**
 * n23: processProximityBatch (Issue #SIMP-1011-3)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n23(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 128) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    double distance = *(double*)(ptr + 0);
    double maxRange = *(double*)(ptr + 8);
    int64_t nowRt = *(int64_t*)(ptr + 16);
    int isStationary = *(int*)(ptr + 24);
    int64_t statDuration = *(int64_t*)(ptr + 28);
    int isHighLoad = *(int*)(ptr + 36);
    double currentIdx = *(double*)(ptr + 40);
    int rawNear = *(int*)(ptr + 48);
    int isFlickering = *(int*)(ptr + 52);

    bool newValue = distance < maxRange;
    double rawIdx = std::max(0.0, std::min(1.0, 1.0 - (distance / maxRange)));
    double nextIdx = (currentIdx * (1.0 - 0.15)) + (rawIdx * 0.15); // PROXIMITY_EMA_ALPHA = 0.15

    int nextRawNear = rawNear;
    int64_t debounceMs = 0;

    if ((newValue ? 1 : 0) != rawNear) {
        if (!newValue && isFlickering != 0 && isStationary != 0) {
            // Guard against display flicker
            nextRawNear = rawNear;
        } else {
            nextRawNear = (newValue ? 1 : 0);
            int64_t calcDebounce = (isStationary != 0) ? 5000 : 1000;
            if (isStationary != 0 && statDuration > 0) {
                calcDebounce += (int64_t)((statDuration / 3600000.0) * 2000.0);
            }
            if (isHighLoad != 0) calcDebounce = (int64_t)((double)calcDebounce * 2.0);
            if (calcDebounce > 15000) calcDebounce = 15000;
            debounceMs = calcDebounce;
        }
    }

    *(double*)(ptr + 128) = nextIdx;
    *(int*)(ptr + 136) = nextRawNear;
    *(int64_t*)(ptr + 140) = debounceMs;

    return 0;
}

/**
 * n24: processSystemPressureNative (Issue #SIMP-1014-2)
 */
JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n24(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 128) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    double heapMb = *(double*)(ptr + 0);
    double memThreshold = *(double*)(ptr + 8);
    double memCritical = *(double*)(ptr + 16);
    double memHysteresis = *(double*)(ptr + 24);
    double storageAvail = *(double*)(ptr + 32);
    double storageLow = *(double*)(ptr + 40);
    double storageCritical = *(double*)(ptr + 48);
    double storageHysteresis = *(double*)(ptr + 56);

    int currentMem = 0;
    if (heapMb >= memCritical) currentMem = 2;
    else if (heapMb >= memThreshold) currentMem = 1;

    if (currentMem < g_lastMemLevel) {
        double threshold = (g_lastMemLevel == 2) ? memCritical : memThreshold;
        if (heapMb > threshold - memHysteresis) currentMem = g_lastMemLevel;
    }

    int needsMemFlush = (currentMem != 0 && currentMem != g_lastMemLevel) ? 1 : 0;
    g_lastMemLevel = currentMem;

    int currentStorage = 0;
    if (storageAvail <= storageCritical) currentStorage = 2;
    else if (storageAvail <= storageLow) currentStorage = 1;

    if (currentStorage < g_lastStorageLevel) {
        double threshold = (g_lastStorageLevel == 2) ? storageCritical : storageLow;
        if (storageAvail < threshold + storageHysteresis) currentStorage = g_lastStorageLevel;
    }

    int needsStoragePrune = (currentStorage != 0 && currentStorage != g_lastStorageLevel) ? 1 : 0;
    g_lastStorageLevel = currentStorage;

    *(int*)(ptr + 64) = currentMem;
    *(int*)(ptr + 68) = needsMemFlush;
    *(int*)(ptr + 72) = currentStorage;
    *(int*)(ptr + 76) = needsStoragePrune;

    return 0;
}

}
