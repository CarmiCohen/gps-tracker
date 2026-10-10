package com.gps19.app

import com.gps19.core.engine.TimeProvider
import com.gps19.core.engine.SIREN_RESUME_COOLDOWN_MS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * SirenLockoutUseCase: Handles system-wide siren lockout/cooldown logic reactively.
 * Decouples domain cooldown policy from audio generation components.
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to property-based 
 *   TimeProvider API and fixed property invocation errors.
 */
@Singleton
class SirenLockoutUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private val _silencedUntilRt = MutableStateFlow(0L)
    val silencedUntilRt: StateFlow<Long> = _silencedUntilRt.asStateFlow()

    fun getSilencedUntilRt(): Long = _silencedUntilRt.value

    /**
     * Returns true if the siren is currently locked out by a cooldown or manual silence.
     */
    fun isLockedOut(): Boolean {
        return timeProvider.elapsedRealtime < _silencedUntilRt.value
    }

    /**
     * Sets a siren lockout for the specified duration.
     * Use [SIREN_RESUME_COOLDOWN_MS] for auto-stop cooldowns.
     * Use [SILENCE_TIMEOUT_MS] for manual user silences.
     */
    fun setSilence(durationMs: Long) {
        if (durationMs > 0) {
            val nowRt = timeProvider.elapsedRealtime
            val newSilenceRt = nowRt + durationMs
            synchronized(this) {
                if (newSilenceRt > _silencedUntilRt.value) {
                    _silencedUntilRt.value = newSilenceRt
                    Timber.d("Siren Lockout engaged until RT: $newSilenceRt (Duration: ${durationMs}ms)")
                }
            }
        }
    }

    /**
     * Immediately clears any active siren lockout.
     */
    fun clearSilence() {
        _silencedUntilRt.value = 0L
        Timber.d("Siren Lockout cleared manually")
    }
}
