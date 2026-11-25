package com.jd.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The title row of every full-screen overlay in both apps (SIMP-NOV10.8-S1, ISSUE-NOV10.7-O74). The title takes the
 * space left by the buttons and wraps, also beside a long title on a narrow phone. It owns the overlay's Back: Back
 * calls [onClose], so Back returns to the map instead of leaving the app (ISSUE-NOV10.8-O78), and no call site needs
 * its own handler. Android's Back is the only way to close an overlay: the close X duplicated it and is gone
 * (ISSUE-NOV25.22-O151).
 */
@Composable
fun OverlayHeader(
    title: String,
    color: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    subtitle: String? = null,
    /** Extra buttons at the end, for example the Events list's Export and Clear. */
    actions: @Composable RowScope.() -> Unit = {}
) {
    BackHandler(onBack = onClose)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = Slate500, fontSize = 10.sp) }
        }
        actions()
    }
}
