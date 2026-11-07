package com.jd.app.sensor

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 142 (ISSUE-NOV7.19-O44): PermissionGuard re-checks permissions on resume. */
class PermissionGuardTest {

    private val dummyContext: Context = object : ContextWrapper(null) {}

    @Test
    fun requiredPermissionsOnApi26() {
        val permissions = getRequiredSensorPermissions(Build.VERSION_CODES.O)
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertFalse(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun requiredPermissionsOnApi33IncludesNotification() {
        val permissions = getRequiredSensorPermissions(Build.VERSION_CODES.TIRAMISU)
        assertEquals(3, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertFalse(permissions.contains(Manifest.permission.FOREGROUND_SERVICE_LOCATION))
    }

    @Test
    fun requiredPermissionsOnApi34IncludesForegroundServiceLocation() {
        val permissions = getRequiredSensorPermissions(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        assertEquals(4, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE_LOCATION))
    }

    @Test
    fun checkSensorPermissionsReturnsTrueWhenAllGranted() {
        val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE)
        val granted = setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE)

        val result = checkSensorPermissions(dummyContext, permissions) { _, perm ->
            if (granted.contains(perm)) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        }

        assertTrue(result)
    }

    @Test
    fun checkSensorPermissionsReturnsFalseWhenAnyDenied() {
        val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE)
        val granted = setOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val result = checkSensorPermissions(dummyContext, permissions) { _, perm ->
            if (granted.contains(perm)) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        }

        assertFalse(result)
    }
}
