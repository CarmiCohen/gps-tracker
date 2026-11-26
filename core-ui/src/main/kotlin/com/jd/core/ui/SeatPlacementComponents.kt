package com.jd.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.SeatPlacement
import com.jd.core.shared.SeatType
import com.jd.core.shared.activePlacement
import com.jd.core.shared.seatPlacementNameError
import com.jd.core.shared.withActivePlacement
import com.jd.core.shared.withPlacementAdded
import com.jd.core.shared.withPlacementDeleted
import com.jd.core.shared.withPlacementEdited

/** The SEAT PLACEMENT screen's title (ISSUE-NOV26.0-O163). */
const val SEAT_PLACEMENT_TITLE = "SEAT PLACEMENT"

/** Under the title: the placement follows where the phone is hidden, never the map (user 2026-11-26). */
const val SEAT_PLACEMENT_NOTE =
    "Choose where the Sensor phone is hidden now. A car parked at home still uses its car seat placement. Sent to the Sensor."

/** The line under `Sit / Seat Occupied Sensor` that opens the screen. */
fun placementLinkText(settings: AlertSettings): String = "Placement: ${settings.activePlacement().label} ›"

/**
 * The Monitor's seat placements (ISSUE-NOV26.0-O163 part 1): tap one to make it active; add, edit or delete. The last
 * placement cannot be deleted. Learning examples per placement comes with O163 part 2. Back closes it (OverlayHeader).
 */
@Composable
fun SeatPlacementOverlay(
    settings: AlertSettings,
    onUpdateAlertSettings: (AlertSettings) -> Unit,
    onClose: () -> Unit
) {
    // The placement being edited: null closed, id 0 a new one.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val active = settings.activePlacement()

    Surface(modifier = Modifier.fillMaxSize(), color = Slate900) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            OverlayHeader(title = SEAT_PLACEMENT_TITLE, color = MonitorBrown, onClose = onClose)
            Spacer(Modifier.height(8.dp))
            Text(SEAT_PLACEMENT_NOTE, color = Slate500, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            settings.seatPlacements.forEach { placement ->
                PlacementRow(
                    placement = placement,
                    isActive = placement.id == active.id,
                    canDelete = settings.seatPlacements.size > 1,
                    onChoose = { onUpdateAlertSettings(settings.withActivePlacement(placement.id)) },
                    onEdit = { editingId = placement.id },
                    onDelete = { pendingDeleteId = placement.id }
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { editingId = 0L },
                colors = ButtonDefaults.buttonColors(containerColor = MonitorBrown),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("ADD PLACEMENT") }
            Spacer(Modifier.height(32.dp))
        }
    }

    editingId?.let { id ->
        val existing = settings.seatPlacements.firstOrNull { it.id == id }
        PlacementEditDialog(
            existing = existing,
            placements = settings.seatPlacements,
            onSave = { name, type ->
                editingId = null
                onUpdateAlertSettings(
                    if (existing == null) settings.withPlacementAdded(name, type, System.currentTimeMillis())
                    else settings.withPlacementEdited(existing.id, name, type)
                )
            },
            onDismiss = { editingId = null }
        )
    }

    pendingDeleteId?.let { id ->
        val placement = settings.seatPlacements.firstOrNull { it.id == id }
        if (placement == null) SideEffect { pendingDeleteId = null }
        else AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete ${placement.name}?") },
            text = {
                Text(
                    if (placement.id == active.id) "It is in use: ${settings.seatPlacements.first { it.id != id }.name} becomes active."
                    else "It is not in use."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId = null
                        onUpdateAlertSettings(settings.withPlacementDeleted(id))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PlacementRow(
    placement: SeatPlacement,
    isActive: Boolean,
    canDelete: Boolean,
    onChoose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) MonitorBrown.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .clickable(onClick = onChoose)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isActive,
            onClick = onChoose,
            colors = RadioButtonDefaults.colors(selectedColor = MonitorBrown)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                placement.name, color = Color.White, fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                placement.type.label + if (isActive) ", in use" else "",
                color = Slate500, fontSize = 11.sp
            )
        }
        TextButton(onClick = onEdit) { Text("EDIT", color = BrandGold, fontSize = 12.sp) }
        if (canDelete) TextButton(onClick = onDelete) { Text("DELETE", color = StatusRed, fontSize = 12.sp) }
    }
}

/** Name and type of a new placement ([existing] null) or an existing one; Save stays off while the name is refused. */
@Composable
private fun PlacementEditDialog(
    existing: SeatPlacement?,
    placements: List<SeatPlacement>,
    onSave: (String, SeatType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: SeatType.CHAIR) }
    val error = seatPlacementNameError(name, placements, exceptId = existing?.id)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add placement" else "Edit placement") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name, for example Driver's seat") },
                    singleLine = true,
                    isError = error != null && name.isNotEmpty(),
                    supportingText = { if (error != null && name.isNotEmpty()) Text(error) }
                )
                Spacer(Modifier.height(8.dp))
                // One type per line: three chips in a row do not fit the S21FE's large font (ISSUE-NOV25.11-O141).
                SeatType.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { type = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = type == option, onClick = { type = option })
                        Text(option.label)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, type) }, enabled = error == null) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
