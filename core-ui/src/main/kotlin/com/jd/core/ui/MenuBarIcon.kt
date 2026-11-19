package com.jd.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Height, and least width, of a menu bar icon's tap area; was 28 dp square, too small to hit the right icon (Nov19.5). */
val MENU_ICON_TAP = 40.dp

/** The drawn icon inside the tap area; was 15 dp. */
val MENU_ICON_SIZE = 20.dp

/**
 * One icon of the menu bar in both apps (Setup, Map, Events, Info, Graphs and the close X). The bars give every icon
 * an equal share of the width beside the motion label (`Modifier.weight(1f)`), and the whole share is the tap area, so
 * the icons spread over the bar on a wide phone and still get about 38 dp each on the narrow S21FE (Nov19.5, user request).
 */
@Composable
fun MenuBarIcon(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(MENU_ICON_TAP)
            .widthIn(min = MENU_ICON_TAP)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(MENU_ICON_SIZE))
    }
}
