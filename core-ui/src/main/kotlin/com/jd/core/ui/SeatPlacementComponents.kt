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
import com.jd.core.shared.SIT_LEARN_OCCUPIED_MS
import com.jd.core.shared.SIT_LEARN_OCCUPIED_WAIT_MS
import com.jd.core.shared.SIT_LEARN_STEPS
import com.jd.core.shared.SIT_LEARN_STEP_COUNT
import com.jd.core.shared.SIT_LEARN_STEP_MS
import com.jd.core.shared.SeatPlacement
import com.jd.core.shared.SeatType
import com.jd.core.shared.SitExampleReport
import com.jd.core.shared.SitLearnRequest
import com.jd.core.shared.sitExamplesSummary
import com.jd.core.shared.sitLearnInstruction
import com.jd.core.shared.sitLearnWaitText
import com.jd.core.shared.sitOccupiedInstruction
import com.jd.core.shared.sitOverlapWarning
import com.jd.core.shared.activePlacement
import com.jd.core.shared.DRIVE_QUESTION_TITLE
import com.jd.core.shared.carSeatPlacements
import com.jd.core.shared.driveQuestionText
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
 * placement cannot be deleted. Back closes it (OverlayHeader). Part 2: each placement lists the examples the Sensor
 * learned for it ([sitExamples], asked for on opening), and LEARN guides a learning round ([SitLearnDialog]).
 */
@Composable
fun SeatPlacementOverlay(
    settings: AlertSettings,
    onUpdateAlertSettings: (AlertSettings) -> Unit,
    onClose: () -> Unit,
    sitExamples: SitExampleReport? = null,
    onSitLearn: (SitLearnRequest) -> Unit = {}
) {
    // The placement being edited: null closed, id 0 a new one.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var learningId by rememberSaveable { mutableStateOf<Long?>(null) }
    val active = settings.activePlacement()
    // The Sensor answers with its examples (ISSUE-NOV26.0-O163 part 2).
    LaunchedEffect(Unit) { onSitLearn(SitLearnRequest(placementId = active.id)) }

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
                    examplesText = sitExamples?.let { sitExamplesSummary(it.book.of(placement.id), it.phoneModel) } ?: SIT_EXAMPLES_NOT_RECEIVED,
                    overlapWarning = sitOverlapWarning(sitExamples?.book?.of(placement.id)),
                    onChoose = { onUpdateAlertSettings(settings.withActivePlacement(placement.id)) },
                    onLearn = { learningId = placement.id },
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

    learningId?.let { id ->
        val placement = settings.seatPlacements.firstOrNull { it.id == id }
        if (placement == null) SideEffect { learningId = null }
        else SitLearnDialog(
            placement = placement,
            report = sitExamples,
            onSitLearn = onSitLearn,
            onClose = { learningId = null }
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
    examplesText: String,
    overlapWarning: String?,
    onChoose: () -> Unit,
    onLearn: () -> Unit,
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
            Text(examplesText, color = Slate500, fontSize = 11.sp)
            overlapWarning?.let { Text(it, color = BrandGold, fontSize = 11.sp) }
        }
        // One action per line: four in a row do not fit the S21FE's large font (ISSUE-NOV25.11-O141).
        Column(horizontalAlignment = Alignment.End) {
            TextButton(onClick = onLearn) { Text("LEARN", color = StatusGreen, fontSize = 12.sp) }
            TextButton(onClick = onEdit) { Text("EDIT", color = BrandGold, fontSize = 12.sp) }
            if (canDelete) TextButton(onClick = onDelete) { Text("DELETE", color = StatusRed, fontSize = 12.sp) }
        }
    }
}

/** A placement's examples line before the Sensor answered: offline, or a Sensor before Nov26.25. */
const val SIT_EXAMPLES_NOT_RECEIVED = "Examples: not received from the Sensor"

/** The learning round's phases (ISSUE-NOV26.0-O163 part 2). */
private enum class LearnPhase { START, STEP, DONE }

/**
 * The guided learning round of [placement] (ISSUE-NOV26.0-O163 part 2): each step of [SIT_LEARN_STEPS] asks the Sensor
 * to record its next sit check as the step's label and waits up to [SIT_LEARN_STEP_MS] for the example to come back in
 * [report]; then the next step starts. The last step (Nov26.26) asks for [SIT_LEARN_OCCUPIED_MS] seated and waits up to
 * [SIT_LEARN_OCCUPIED_WAIT_MS] for the placement's occupied envelope. START OVER deletes the placement's examples
 * first. Closing it ends the Sensor's waiting step, which holds its sit and vibration alerts meanwhile.
 */
@Composable
fun SitLearnDialog(
    placement: SeatPlacement,
    report: SitExampleReport?,
    onSitLearn: (SitLearnRequest) -> Unit,
    onClose: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf(LearnPhase.START) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var stepAttempt by rememberSaveable { mutableIntStateOf(0) } // A new attempt restarts the step's wait.
    var isTimedOut by rememberSaveable { mutableStateOf(false) }
    var lastFelt by rememberSaveable { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableIntStateOf((SIT_LEARN_STEP_MS / 1000).toInt()) }
    val examples = report?.book?.of(placement.id)
    val label = SIT_LEARN_STEPS.getOrNull(stepIndex) // Null in the seated step, the last.
    val stepMs = if (label == null) SIT_LEARN_OCCUPIED_WAIT_MS else SIT_LEARN_STEP_MS
    // The newest example of the step's label (or the occupied envelope) when the step began, set before the step's
    // request; only a newer one is the step's (a deleted round's old examples stay in [report] until the Sensor's answer).
    var newestAtStart by rememberSaveable { mutableStateOf<Long?>(null) }

    val beginStep: (Int) -> Unit = { index ->
        val stepLabel = SIT_LEARN_STEPS.getOrNull(index)
        newestAtStart = if (stepLabel != null) examples?.newest(stepLabel)?.atMs else examples?.occupied?.atMs
        stepIndex = index
        stepAttempt++
    }
    val close: () -> Unit = {
        if (phase == LearnPhase.STEP) onSitLearn(SitLearnRequest(placementId = placement.id)) // Ends the Sensor's wait.
        onClose()
    }
    val start: (Boolean) -> Unit = { clear ->
        if (clear) onSitLearn(SitLearnRequest(placementId = placement.id, clear = true))
        lastFelt = null
        beginStep(0)
        phase = LearnPhase.STEP
    }
    val nextStep: () -> Unit = {
        if (stepIndex + 1 >= SIT_LEARN_STEP_COUNT) {
            phase = LearnPhase.DONE
            onSitLearn(SitLearnRequest(placementId = placement.id)) // Ends the wait and brings the newest examples.
        } else {
            beginStep(stepIndex + 1)
        }
    }

    if (phase == LearnPhase.STEP) {
        LaunchedEffect(stepIndex, stepAttempt) {
            isTimedOut = false
            onSitLearn(SitLearnRequest(placementId = placement.id, label = label, occupied = label == null))
            for (left in (stepMs / 1000).toInt() downTo 1) {
                secondsLeft = left
                kotlinx.coroutines.delay(1_000L)
            }
            secondsLeft = 0
            isTimedOut = true
        }
        // The step's example, or the seated step's envelope, came back from the Sensor.
        val newest = label?.let { examples?.newest(it) }
        val occupied = examples?.occupied?.takeIf { label == null }
        LaunchedEffect(newest?.atMs, occupied?.atMs) {
            val newAtMs = newest?.atMs ?: occupied?.atMs ?: return@LaunchedEffect
            if (newAtMs <= (newestAtStart ?: Long.MIN_VALUE)) return@LaunchedEffect
            lastFelt = if (newest != null) "${newest.label.singular.replaceFirstChar { it.uppercase() }} felt: ${newest.features.describe()}"
            else "Seated ${SIT_LEARN_OCCUPIED_MS / 60_000} min: ${occupied!!.describe(placement.type)}"
            nextStep()
        }
    }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("Learn: ${placement.name}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                when (phase) {
                    LearnPhase.START -> {
                        Text("Hide the Sensor phone where it stays, then follow $SIT_LEARN_STEP_COUNT steps here: sit down and stand up 3 times, a bump, then ${SIT_LEARN_OCCUPIED_MS / 60_000} min seated. The Sensor sends no sit or vibration alert during the round.")
                        Spacer(Modifier.height(8.dp))
                        Text(sitExamplesSummary(examples, report?.phoneModel), color = Slate500, fontSize = 12.sp)
                        if (report == null) Text(SIT_EXAMPLES_NOT_RECEIVED, color = BrandGold, fontSize = 12.sp)
                    }
                    LearnPhase.STEP -> {
                        Text("Step ${stepIndex + 1} of $SIT_LEARN_STEP_COUNT", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(label?.let { sitLearnInstruction(it, placement.type) } ?: sitOccupiedInstruction(placement.type))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            sitLearnWaitText(label == null, isTimedOut, secondsLeft),
                            color = if (isTimedOut) BrandGold else Slate500, fontSize = 12.sp
                        )
                        lastFelt?.let { Text(it, color = StatusGreen, fontSize = 12.sp) }
                    }
                    LearnPhase.DONE -> {
                        Text("Round finished.")
                        lastFelt?.let { Text(it, color = StatusGreen, fontSize = 12.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text(sitExamplesSummary(examples, report?.phoneModel), color = Slate500, fontSize = 12.sp)
                        sitOverlapWarning(examples)?.let { Text(it, color = BrandGold, fontSize = 12.sp) }
                        Text("The examples change no detection yet: the fixed rule still decides.", color = Slate500, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            when (phase) {
                LearnPhase.START -> Button(onClick = { start(false) }) { Text("START") }
                LearnPhase.STEP -> Row {
                    if (isTimedOut) TextButton(onClick = { stepAttempt++ }) { Text("TRY AGAIN") }
                    TextButton(onClick = nextStep) { Text("SKIP") }
                }
                LearnPhase.DONE -> Button(onClick = onClose) { Text("CLOSE") }
            }
        },
        dismissButton = {
            when (phase) {
                LearnPhase.START -> Row {
                    if (!examples?.examples.isNullOrEmpty()) TextButton(onClick = { start(true) }) { Text("START OVER", color = StatusRed) }
                    TextButton(onClick = close) { Text("CANCEL") }
                }
                LearnPhase.STEP -> TextButton(onClick = close) { Text("STOP") }
                LearnPhase.DONE -> Unit
            }
        }
    )
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
            // Scrolls: in landscape the S21FE's large font cut the type list after Bed (ISSUE-NOV26.29-O165).
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

/** The after-drive question's button that keeps the placement in use. */
const val DRIVE_QUESTION_KEEP = "KEEP"

/** ...and the one that opens the SEAT PLACEMENT screen, to add a car seat or choose another. */
const val DRIVE_QUESTION_OPEN = "SEAT PLACEMENT"

/**
 * The Monitor's question after the Sensor was driven and parked (ISSUE-NOV26.0-O163 part 1): one button per car seat
 * placement switches to it ([onChoose]), SEAT PLACEMENT opens the screen, KEEP leaves it. Nothing switches by itself;
 * Back or a tap outside keeps the placement as well.
 */
@Composable
fun DriveQuestionDialog(
    settings: AlertSettings,
    parkedSinceText: String,
    onChoose: (SeatPlacement) -> Unit,
    onOpenPlacements: () -> Unit,
    onKeep: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(DRIVE_QUESTION_TITLE) },
        text = {
            Column {
                Text(driveQuestionText(settings, parkedSinceText))
                Spacer(Modifier.height(8.dp))
                // One choice per line, for the S21FE's large font (ISSUE-NOV25.11-O141).
                settings.carSeatPlacements().forEach { placement ->
                    Button(
                        onClick = { onChoose(placement) },
                        colors = ButtonDefaults.buttonColors(containerColor = MonitorBrown),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Use ${placement.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onOpenPlacements) { Text(DRIVE_QUESTION_OPEN) } },
        dismissButton = { TextButton(onClick = onKeep) { Text(DRIVE_QUESTION_KEEP) } }
    )
}
