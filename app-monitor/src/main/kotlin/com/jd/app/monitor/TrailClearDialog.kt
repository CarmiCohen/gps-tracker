package com.jd.app.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** The tag of the dialog's row count line, for tests. */
const val TRAIL_CLEAR_COUNT_TAG = "trail_clear_count"

/**
 * The map's CLEAR (ISSUE-NOV27.1-O167): how much to keep, whose trail, and the rows that go before the user confirms.
 * [countRows] is asked again on every change; Clear stays off until the count is known and something would go.
 */
@Composable
fun TrailClearDialog(
    countRows: suspend (TrailClearChoice) -> TrailClearCount,
    onClear: (TrailClearChoice) -> Unit,
    onDismiss: () -> Unit
) {
    var keep by rememberSaveable { mutableStateOf(TrailClearChoice().keep) }
    var scope by rememberSaveable { mutableStateOf(TrailClearChoice().scope) }
    val choice = TrailClearChoice(keep, scope)
    val count by produceState<TrailClearCount?>(null, choice) { value = null; value = countRows(choice) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear the trails?") },
        text = {
            // Scrolls: seven choices do not fit a landscape screen or a large font.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Keep the last", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    TrailKeep.entries.forEach { option -> ChoiceRow(option.label, option == keep) { keep = option } }
                }
                Text("Trails to clear", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Column(Modifier.selectableGroup()) {
                    TrailScope.entries.forEach { option -> ChoiceRow(option.label, option == scope) { scope = option } }
                }
                Text(
                    count?.text ?: "Counting rows...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp).testTag(TRAIL_CLEAR_COUNT_TAG)
                )
                Text("SAVE the trails first to keep a copy.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = { onClear(choice) },
                enabled = (count?.deleted ?: 0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
            ) { Text("Clear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect, role = Role.RadioButton).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
