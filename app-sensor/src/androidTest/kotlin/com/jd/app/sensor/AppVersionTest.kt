package com.jd.app.sensor

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jd.core.ui.NO_VALUE
import com.jd.core.ui.getAppVersionName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies Chapter 126.2: the shown version comes from the package and is never invented (ISSUE-NOV5.0-O6). */
@RunWith(AndroidJUnit4::class)
class AppVersionTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun readsTheInstalledVersion() {
        val version = getAppVersionName(context)

        assertEquals(context.packageManager.getPackageInfo(context.packageName, 0).versionName, version)
        assertTrue("Unexpected version $version", Regex("Nov\\d+\\.\\d+").matches(version))
    }

    @Test
    fun anUnknownPackageShowsNoVersion() {
        val missingPackage = object : ContextWrapper(context) {
            override fun getPackageName() = "com.jd.app.no.such.package"
        }

        assertEquals(NO_VALUE, getAppVersionName(missingPackage))
    }
}
