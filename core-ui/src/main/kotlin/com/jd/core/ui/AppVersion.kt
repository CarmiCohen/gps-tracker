package com.jd.core.ui

import android.content.Context
import android.content.pm.PackageManager

/** The version of the installed app. It lives only in the build file, so no code can go stale. */
fun getAppVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: NO_VALUE
} catch (_: PackageManager.NameNotFoundException) {
    NO_VALUE
}
