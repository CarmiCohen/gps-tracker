package com.jd.app.monitor

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** The map's LOAD button (ISSUE-NOV17.7-O107): Material's "upload", which the core icon set lacks. */
val TrailLoadIcon: ImageVector = materialIcon(name = "Filled.Upload") {
    materialPath {
        moveTo(5.0f, 20.0f)
        horizontalLineToRelative(14.0f)
        verticalLineToRelative(-2.0f)
        horizontalLineTo(5.0f)
        close()
        moveTo(5.0f, 10.0f)
        horizontalLineToRelative(4.0f)
        verticalLineToRelative(6.0f)
        horizontalLineToRelative(6.0f)
        verticalLineToRelative(-6.0f)
        horizontalLineToRelative(4.0f)
        lineToRelative(-7.0f, -7.0f)
        close()
    }
}

/** The map's SAVE button (ISSUE-NOV17.7-O107): Material's "save", which the core icon set lacks. */
val TrailSaveIcon: ImageVector = materialIcon(name = "Filled.Save") {
    materialPath {
        moveTo(17.0f, 3.0f)
        horizontalLineTo(5.0f)
        curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
        verticalLineToRelative(14.0f)
        curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
        horizontalLineToRelative(14.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
        verticalLineTo(7.0f)
        close()
        moveTo(12.0f, 19.0f)
        curveToRelative(-1.66f, 0.0f, -3.0f, -1.34f, -3.0f, -3.0f)
        reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
        reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
        reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
        close()
        moveTo(15.0f, 9.0f)
        horizontalLineTo(5.0f)
        verticalLineTo(5.0f)
        horizontalLineToRelative(10.0f)
        close()
    }
}
