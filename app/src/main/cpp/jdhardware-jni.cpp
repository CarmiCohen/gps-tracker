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
    double loadFactor = (cpuLoad > SENSOR_LOAD_GATE_CPU_THRESHOLD_VAL) ? 2.0 : 1.0;
    double dynamicGate = adaptiveFloor * 1.5 * loadFactor;
    return (vibration < dynamicGate) ? 1 : 0;
}

JNIEXPORT jdouble JNICALL
Java_com_gps19_app_JdHardwareManager_n13(JNIEnv* env, jclass clazz, jdouble currentFloor, jdouble vibration, jint isWarming, jdouble cpuLoad) {
    if (std::isnan(vibration) || std::isnan(currentFloor) || vibration <= 0.0 || cpuLoad > SENSOR_LOAD_GATE_CPU_THRESHOLD_VAL) {
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
    double loadFactor = (cpu > SENSOR_LOAD_GATE_CPU_THRESHOLD_VAL) ? 1.5 : 1.0;
    double baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - (double)sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * VIBRATION_SHOCK_MULTIPLIER_VAL * loadFactor);
    return (peak > dynamicThreshold) ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n18(JNIEnv* env, jclass clazz, jdouble vibe, jdouble floor, jfloat sens, jdouble cpu) {
    if (std::isnan(vibe) || std::isnan(floor)) return 0;
    double loadFactor = (cpu > SENSOR_LOAD_GATE_CPU_THRESHOLD_VAL) ? 1.5 : 1.0;
    double baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - (double)sens)) * loadFactor;
    double dynamicThreshold = std::max(baseThreshold, floor * VIBRATION_SUSPICIOUS_MULTIPLIER_VAL * loadFactor);
    return (vibe > dynamicThreshold) ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n19(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // ISSUE-OCT18.0-F2: Reinforced NaN sanitization and alignment safety.
    double x, y, z, lx, ly, lz, floor, cpu, lastRaw, lastHpf, energy, snr, thermal, heap;
    int32_t isWarming;
    int64_t nowRt;

    std::memcpy(&x, ptr + 0, 8);
    std::memcpy(&y, ptr + 8, 8);
    std::memcpy(&z, ptr + 16, 8);
    std::memcpy(&lx, ptr + 24, 8);
    std::memcpy(&ly, ptr + 32, 8);
    std::memcpy(&lz, ptr + 40, 8);
    std::memcpy(&floor, ptr + 48, 8);
    std::memcpy(&cpu, ptr + 56, 8);
    std::memcpy(&isWarming, ptr + 64, 4);
    std::memcpy(&lastRaw, ptr + 72, 8);
    std::memcpy(&lastHpf, ptr + 80, 8);
    std::memcpy(&energy, ptr + 88, 8);
    std::memcpy(&snr, ptr + 96, 8);
    std::memcpy(&thermal, ptr + 104, 8);
    std::memcpy(&heap, ptr + 112, 8);
    std::memcpy(&nowRt, ptr + 120, 8);

    if (std::isnan(x)) x = 0.0; if (std::isnan(y)) y = 0.0; if (std::isnan(z)) z = 0.0;
    if (std::isnan(lx)) lx = 0.0; if (std::isnan(ly)) ly = 0.0; if (std::isnan(lz)) lz = 0.0;
    if (std::isnan(floor)) floor = VIBRATION_FLOOR_MIN;
    if (std::isnan(lastRaw)) lastRaw = 0.0;
    if (std::isnan(lastHpf)) lastHpf = 0.0;
    if (std::isnan(energy)) energy = 0.0;
    if (std::isnan(snr)) snr = 0.0;
    if (std::isnan(thermal)) thermal = 0.0;
    if (std::isnan(heap)) heap = 0.0;

    double dx = x - lx, dy = y - ly, dz = z - lz;
    double delta = std::sqrt(dx * dx + dy * dy + dz * dz) / 9.80665;
    if (std::isnan(delta)) delta = 0.0;

    double nextFloor = floor;
    if (delta > 0.0 && cpu <= SENSOR_LOAD_GATE_CPU_THRESHOLD_VAL) {
        double alpha = (delta < floor) ? (isWarming ? 0.5 : 0.1) : (delta < 1.0 ? (isWarming ? 0.1 : 0.01) : 0.0);
        nextFloor = (floor * (1.0 - alpha)) + (delta * alpha);
        if (std::isnan(nextFloor)) nextFloor = floor;
        nextFloor = std::max(VIBRATION_FLOOR_MIN, std::min(VIBRATION_FLOOR_MAX, nextFloor));
    }
    double nextHpf = 0.9 * (lastHpf + delta - lastRaw);
    if (std::isnan(nextHpf)) nextHpf = 0.0;

    double nextEnergy = (energy * 0.9) + (std::abs(nextHpf) * 0.1);
    if (std::isnan(nextEnergy)) nextEnergy = 0.0;

    double dynamicGate = nextFloor * 1.5 * ((cpu > SENSOR_LOAD_GATE_CPU_THRESHOLD_VAL) ? 2.0 : 1.0);
    int32_t isStationary = (delta < dynamicGate) ? 1 : 0;
    if (isStationary) {
        if (g_stationaryStartRt == 0) g_stationaryStartRt = nowRt;
    } else { g_stationaryStartRt = 0; }

    double loadFactor = (cpu > SENSOR_LOAD_GATE_CPU_THRESHOLD_VAL) ? 1.5 : 1.0;
    double suspiciousThreshold = std::max((0.05 + (0.45 - 0.05) * 0.5) * loadFactor, nextFloor * VIBRATION_SUSPICIOUS_MULTIPLIER_VAL * loadFactor);
    int32_t isSuspicious = (delta > suspiciousThreshold) ? 1 : 0;
    int32_t isMemThrottled = (g_lastMemLevel > 0) ? 1 : 0;
    int32_t muzzleReset = (delta > VIBRATION_SHOCK_THRESHOLD_G_VAL) ? 1 : 0;
    int32_t isJamming = (snr > 0.0 && snr < JUMP_GATE_LOW_SNR_THRESHOLD_VAL && delta > 0.5) ? 1 : 0;

    // Aligned Outputs
    std::memcpy(ptr + 128, &delta, 8);
    std::memcpy(ptr + 136, &nextFloor, 8);
    std::memcpy(ptr + 144, &nextHpf, 8);
    std::memcpy(ptr + 152, &nextEnergy, 8);
    std::memcpy(ptr + 160, &isStationary, 4);
    std::memcpy(ptr + 164, &isSuspicious, 4);
    std::memcpy(ptr + 168, &isMemThrottled, 4);

    int64_t stationaryDur = (isStationary && g_stationaryStartRt > 0) ? (nowRt - g_stationaryStartRt) : 0;
    std::memcpy(ptr + 176, &stationaryDur, 8);
    std::memcpy(ptr + 184, &muzzleReset, 4);
    std::memcpy(ptr + 188, &isJamming, 4);

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
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 1060) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // ISSUE-OCT18.0-F2: Hardened GNSS batch alignment and memcpy migration.
    int32_t rawCount;
    std::memcpy(&rawCount, ptr + 0, 4);
    int count = std::min(rawCount, 64), usedCount = 0, snrCount = 0;

    // New re-aligned offsets: svid@8, cn0@264, usedInFix@520
    uint8_t* svidPtr = ptr + 8;
    uint8_t* cn0Ptr = ptr + 264;
    uint8_t* usedInFixPtr = ptr + 520;
    double snrSum = 0.0;

    for (int i = 0; i < count; i++) {
        int32_t used;
        std::memcpy(&used, usedInFixPtr + (i * 4), 4);
        if (used != 0) usedCount++;

        float cn0;
        std::memcpy(&cn0, cn0Ptr + (i * 4), 4);
        if (cn0 > 0.0f && !std::isnan(cn0)) { snrSum += cn0; snrCount++; }
    }

    // Outputs re-aligned: View@1040, Used@1044, AvgSnr@1048
    std::memcpy(ptr + 1040, &count, 4);
    std::memcpy(ptr + 1044, &usedCount, 4);
    double avgSnr = (snrCount > 0) ? (snrSum / snrCount) : 0.0;
    if (std::isnan(avgSnr)) avgSnr = 0.0;
    std::memcpy(ptr + 1048, &avgSnr, 8);

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n22(JNIEnv* env, jclass clazz, jshortArray buffer) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Aligned Inputs
    int32_t readCount;
    double baseAlpha, vibeRolling;
    int64_t nowRt;
    int32_t isWarming;

    std::memcpy(&readCount, ptr + 0, 4);
    std::memcpy(&baseAlpha, ptr + 8, 8);
    std::memcpy(&vibeRolling, ptr + 16, 8);
    std::memcpy(&nowRt, ptr + 24, 8);
    std::memcpy(&isWarming, ptr + 32, 4);

    if (std::isnan(baseAlpha)) baseAlpha = 0.01;
    if (std::isnan(vibeRolling)) vibeRolling = 0.0;

    jshort* samples = (jshort*)env->GetPrimitiveArrayCritical(buffer, nullptr);
    int maxAmp = 0;
    for (int i = 0; i < readCount; i++) maxAmp = std::max(maxAmp, std::abs((int)samples[i]));
    env->ReleasePrimitiveArrayCritical(buffer, samples, JNI_ABORT);

    double db = (maxAmp > 0) ? (20.0 * std::log10((double)maxAmp)) : 0.0;
    if (std::isnan(db)) db = 0.0;

    FastPathConfig& cfg = g_fastPathConfigs[FASTPATH_ACOUSTIC];
    double vibeFactor = (vibeRolling > 0.5) ? std::max(0.01, 1.0 - ((vibeRolling - 0.5) / 1.0)) : 1.0;
    double alpha = (isWarming) ? 0.0 : (baseAlpha * vibeFactor);

    if (!std::isnan(db)) {
        if (cfg.baseline < 0 || std::isnan(cfg.baseline)) {
            cfg.baseline = db;
        } else if (alpha > 0.0) {
            cfg.baseline = (cfg.baseline * (1.0 - alpha)) + (db * alpha);
        }
    }

    int32_t isSpike = 0;
    if ((db - cfg.baseline) > cfg.threshold && db >= cfg.minThreshold) {
        if (nowRt - cfg.lastTriggerRt > cfg.debounceMs) {
            cfg.lastTriggerRt = nowRt;
            isSpike = 1;
        }
    }

    // Aligned Outputs
    std::memcpy(ptr + 128, &maxAmp, 4);
    std::memcpy(ptr + 136, &db, 8);
    std::memcpy(ptr + 144, &isSpike, 4);
    std::memcpy(ptr + 152, &cfg.lastTriggerRt, 8);
    int32_t isMemThrottled = (g_lastMemLevel > 0) ? 1 : 0;
    std::memcpy(ptr + 160, &isMemThrottled, 4);

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n23(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 256) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Aligned Inputs
    double dist, maxRange, currentIdx;
    int64_t nowRt, stationaryDuration;
    int32_t isStationary, isHighLoad, rawNear, isFlickering;

    std::memcpy(&dist, ptr + 0, 8);
    std::memcpy(&maxRange, ptr + 8, 8);
    std::memcpy(&nowRt, ptr + 16, 8);
    std::memcpy(&isStationary, ptr + 24, 4);
    std::memcpy(&stationaryDuration, ptr + 32, 8);
    std::memcpy(&isHighLoad, ptr + 40, 4);
    std::memcpy(&currentIdx, ptr + 48, 8);
    std::memcpy(&rawNear, ptr + 56, 4);
    std::memcpy(&isFlickering, ptr + 60, 4);

    if (std::isnan(dist)) dist = maxRange + 1.0;
    if (std::isnan(currentIdx)) currentIdx = 0.0;

    int32_t newValue = (dist < maxRange);
    double rawIdx = std::max(0.0, std::min(1.0, 1.0 - (dist / maxRange)));
    double nextIdx = (currentIdx * (1.0 - PROXIMITY_EMA_ALPHA)) + (rawIdx * PROXIMITY_EMA_ALPHA);
    if (std::isnan(nextIdx)) nextIdx = currentIdx;

    int32_t nextRawNear = rawNear;
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
    std::memcpy(ptr + 128, &nextIdx, 8);
    std::memcpy(ptr + 136, &nextRawNear, 4);
    std::memcpy(ptr + 144, &debounceMs, 8);
    int32_t isMemThrottled = (g_lastMemLevel > 0) ? 1 : 0;
    std::memcpy(ptr + 152, &isMemThrottled, 4);

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_gps19_app_JdHardwareManager_n24(JNIEnv* env, jclass clazz) {
    if (g_sharedBufferPtr == nullptr || g_sharedBufferSize < 128) return -1;
    uint8_t* ptr = (uint8_t*)g_sharedBufferPtr;

    // Aligned Inputs
    double heapMb, memPressure, memCritical, hysteresis;
    std::memcpy(&heapMb, ptr + 0, 8);
    std::memcpy(&memPressure, ptr + 8, 8);
    std::memcpy(&memCritical, ptr + 16, 8);
    std::memcpy(&hysteresis, ptr + 24, 8);

    if (std::isnan(heapMb)) return 0;

    int32_t currentMem = (heapMb >= memCritical) ? 2 : (heapMb >= memPressure ? 1 : 0);
    if (currentMem < g_lastMemLevel && heapMb > ((g_lastMemLevel == 2 ? memCritical : memPressure) - hysteresis)) {
        currentMem = g_lastMemLevel;
    }

    std::memcpy(ptr + 64, &currentMem, 4);
    int32_t needsFlush = (currentMem != 0 && currentMem != g_lastMemLevel);
    std::memcpy(ptr + 68, &needsFlush, 4);
    g_lastMemLevel = currentMem;

    return 0;
}

}
