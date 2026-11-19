package com.jd.app.monitor

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    /**
     * A push that comes during a run would start [MonitorMessagingService] inside the test process, where no test has
     * built the Hilt component, and crash the run (ISSUE-NOV12.3-O90). The service is off for the run's length.
     * An androidTest manifest cannot remove it: that manifest goes into the test APK, the service lives in the app APK.
     * Android starts no service before this call, so no push slips in first.
     */
    override fun onCreate(arguments: Bundle?) {
        setMessagingService(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
        super.onCreate(arguments)
    }

    /** Back to the manifest's state (on), so the installed Monitor receives pushes again after the run. */
    override fun finish(resultCode: Int, results: Bundle?) {
        setMessagingService(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        super.finish(resultCode, results)
    }

    private fun setMessagingService(state: Int) {
        val app = targetContext
        app.packageManager.setComponentEnabledSetting(
            ComponentName(app, MonitorMessagingService::class.java), state, PackageManager.DONT_KILL_APP
        )
    }
}
