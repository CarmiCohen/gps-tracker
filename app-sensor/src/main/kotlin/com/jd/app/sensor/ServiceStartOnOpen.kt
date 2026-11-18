package com.jd.app.sensor

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Opening the Sensor app starts a stopped [SensorService] (ISSUE-NOV12.6-O92), and so does bringing an open console
 * back to the front, as the `STOP SERVICE` dialog promises (ISSUE-NOV18.0-O115: before, only a fresh launch started
 * it, so Home and the launcher icon left it stopped). The first start of a console recreated by a rotation is
 * skipped, so turning the phone does not undo a stop made a moment before; a running service only gets a new start
 * command.
 */
class ServiceStartOnOpen(
    freshLaunch: Boolean,
    private val canStart: () -> Boolean,
    private val start: () -> Unit
) : DefaultLifecycleObserver {

    private var skipNextStart = !freshLaunch

    override fun onStart(owner: LifecycleOwner) {
        if (skipNextStart) {
            skipNextStart = false
            return
        }
        if (canStart()) start()
    }
}
