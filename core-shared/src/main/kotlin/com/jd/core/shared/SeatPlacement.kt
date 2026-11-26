package com.jd.core.shared

import kotlinx.serialization.Serializable

/**
 * Where the Sensor phone is hidden for sit detection (ISSUE-NOV26.0-O163 part 1): the key is where the phone is placed,
 * not where it is, so a car parked at home still uses its car seat placement (user 2026-11-26). The Monitor keeps the
 * list and the active one ([AlertSettings.seatPlacements], [AlertSettings.activePlacementId]); the Sensor receives
 * the active one in [SensorBehaviour.placement]. Learned examples per placement come with O163 part 2.
 */
@Serializable
data class SeatPlacement(val id: Long, val name: String, val type: SeatType) {
    /** `Office chair (chair)`, or `Chair` when the name is the type's own label. */
    val label: String get() = if (name.equals(type.label, ignoreCase = true)) name else "$name (${type.label.lowercase()})"
}

/** The kind of seat: the fixed sit rule comes from a chair cushion (ISSUE-NOV25.22-O150); a car seat differs most. */
@Serializable
enum class SeatType(val label: String) { CHAIR("Chair"), SOFA("Sofa"), CAR_SEAT("Car seat") }

/** The only placement until the user adds one: the chair cushion the fixed rule was measured on. */
val DEFAULT_SEAT_PLACEMENT = SeatPlacement(id = 1L, name = SeatType.CHAIR.label, type = SeatType.CHAIR)

/** A placement name fits one line of the Monitor's list and the Sensor's page. */
const val SEAT_PLACEMENT_NAME_MAX = 24

/** Why [name] cannot name a placement among [placements] (ignoring the one with [exceptId]), or null when it can. */
fun seatPlacementNameError(name: String, placements: List<SeatPlacement>, exceptId: Long? = null): String? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> "Enter a name"
        trimmed.length > SEAT_PLACEMENT_NAME_MAX -> "At most $SEAT_PLACEMENT_NAME_MAX characters"
        placements.any { it.id != exceptId && it.name.equals(trimmed, ignoreCase = true) } -> "This name is already used"
        else -> null
    }
}

/** The active placement; the first one when the active id is missing (a list edited elsewhere). */
fun AlertSettings.activePlacement(): SeatPlacement =
    seatPlacements.firstOrNull { it.id == activePlacementId } ?: seatPlacements.firstOrNull() ?: DEFAULT_SEAT_PLACEMENT

/** These settings with [id] active; unchanged for an unknown id. */
fun AlertSettings.withActivePlacement(id: Long): AlertSettings =
    if (seatPlacements.any { it.id == id }) copy(activePlacementId = id) else this

/**
 * A new placement, made active (the user adds one when hiding the phone there); unchanged when the name is refused.
 * Its id is the add time [nowMs], so a deleted placement's id is never reused (O163 part 2 keys learned examples by id).
 */
fun AlertSettings.withPlacementAdded(name: String, type: SeatType, nowMs: Long): AlertSettings {
    if (seatPlacementNameError(name, seatPlacements) != null) return this
    val id = maxOf(nowMs, (seatPlacements.maxOfOrNull { it.id } ?: 0L) + 1L)
    return copy(seatPlacements = seatPlacements + SeatPlacement(id, name.trim(), type), activePlacementId = id)
}

/** [id] renamed and retyped; unchanged when the name is refused. */
fun AlertSettings.withPlacementEdited(id: Long, name: String, type: SeatType): AlertSettings {
    if (seatPlacementNameError(name, seatPlacements, exceptId = id) != null) return this
    return copy(seatPlacements = seatPlacements.map { if (it.id == id) it.copy(name = name.trim(), type = type) else it })
}

/** [id] removed; the last placement stays, and a removed active one passes to the first left. */
fun AlertSettings.withPlacementDeleted(id: Long): AlertSettings {
    if (seatPlacements.size <= 1) return this
    val left = seatPlacements.filterNot { it.id == id }
    return copy(seatPlacements = left, activePlacementId = if (activePlacementId == id) left.first().id else activePlacementId)
}
