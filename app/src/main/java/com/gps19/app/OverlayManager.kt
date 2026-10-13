package com.gps19.app

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.*

/**
 * OverlayManager: Unified HUD management for GPS Tracker.
 * Oct.13.1:
 * - SIMP-OCT12.16-S1: Unified HUD Overlay. Merged HeaderBar, GlobalStatusBar, 
 *   and map overlays into a single layout to prevent Z-order collisions.
 */

@Composable
fun UnifiedHUDOverlay(
    stateProvider: UiStateProvider,
    onMainEvent: (UiEvent) -> Unit,
    onSaveTrail: () -> Unit = {},
    onLoadTrail: () -> Unit = {}
) {
    val nav by stateProvider.navigation.collectAsStateWithLifecycle()
    val session by stateProvider.session.collectAsStateWithLifecycle()
    val spatial by stateProvider.spatial.collectAsStateWithLifecycle()
    val mapState by stateProvider.mapViewState.collectAsStateWithLifecycle()
    
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isMapVisible = nav.isMapVisible
    val isAnyOverlayOpen = nav.isSettingsOpen || nav.isLogVisible || nav.isRibbonsVisible || nav.isGnssDetailVisible || nav.isPhoneSetupVisible
    
    val isSystemReady = session.isSystemReady(spatial.homePoints.size)
    val systemIssuesCount = session.systemIssuesCount(spatial.homePoints.size)

    val onDashboard = {
        if (isMapVisible) onMainEvent(UiEvent.ToggleMap(false))
        if (nav.isLogVisible) onMainEvent(UiEvent.ToggleLog(false))
        if (nav.isSettingsOpen) onMainEvent(UiEvent.ToggleSettings(false))
        if (nav.isRibbonsVisible) onMainEvent(UiEvent.ToggleRibbons(false))
        if (nav.isGnssDetailVisible) onMainEvent(UiEvent.ToggleGnssDetail(false))
        if (nav.isPhoneSetupVisible) onMainEvent(UiEvent.TogglePhoneSetup(false))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map Overlays (Lower Z-Order than Header/Status if overlapping, but here they are managed together)
        if (isMapVisible && !isAnyOverlayOpen && session.hydrationLevel >= 4) {
            MapSpecificOverlays(
                mapState = mapState,
                isLandscape = isLandscape,
                onMainEvent = onMainEvent,
                onSaveTrail = onSaveTrail,
                onLoadTrail = onLoadTrail
            )
        }

        // 2. Global Header and Status Bar
        if (!isAnyOverlayOpen && session.hydrationLevel >= 4) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    HeaderBar(
                        isLogVisible = nav.isLogVisible, isSettingsOpen = nav.isSettingsOpen, isRibbonsVisible = nav.isRibbonsVisible, isMapVisible = nav.isMapVisible, isPhoneSetupVisible = nav.isPhoneSetupVisible,
                        requiresExtraTopPadding = session.permissions.requiresExtraTopPadding, isSystemReady = isSystemReady, systemIssuesCount = systemIssuesCount,
                        onDashboard = onDashboard, onS = { onMainEvent(UiEvent.ToggleSettings(!nav.isSettingsOpen)) }, onL = { onMainEvent(UiEvent.ToggleLog(!nav.isLogVisible)) }, onM = { onMainEvent(UiEvent.ToggleMap(!nav.isMapVisible)) }, onR = { onMainEvent(UiEvent.ToggleRibbons(!nav.isRibbonsVisible)) }, onEvent = onMainEvent
                    )
                    if (session.hydrationLevel >= 5) {
                        Column(modifier = Modifier.weight(1f)) {
                            GlobalStatusBar(
                                stateProvider = stateProvider,
                                modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onMainEvent(UiEvent.SetRedScreenVisible(true)) }) }
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().zIndex(10f)) {
                    Surface(color = if (isMapVisible) Color.Transparent else MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            HeaderBar(
                                isLogVisible = nav.isLogVisible, isSettingsOpen = nav.isSettingsOpen, isRibbonsVisible = nav.isRibbonsVisible, isMapVisible = nav.isMapVisible, isPhoneSetupVisible = nav.isPhoneSetupVisible,
                                requiresExtraTopPadding = session.permissions.requiresExtraTopPadding, isSystemReady = isSystemReady, systemIssuesCount = systemIssuesCount,
                                onDashboard = onDashboard, onS = { onMainEvent(UiEvent.ToggleSettings(!nav.isSettingsOpen)) }, onL = { onMainEvent(UiEvent.ToggleLog(!nav.isLogVisible)) }, onM = { onMainEvent(UiEvent.ToggleMap(!nav.isMapVisible)) }, onR = { onMainEvent(UiEvent.ToggleRibbons(!nav.isRibbonsVisible)) }, onEvent = onMainEvent
                            )
                            if (session.hydrationLevel >= 5) {
                                GlobalStatusBar(
                                    stateProvider = stateProvider,
                                    modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onMainEvent(UiEvent.SetRedScreenVisible(true)) }) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapSpecificOverlays(
    mapState: MapViewState,
    isLandscape: Boolean,
    onMainEvent: (UiEvent) -> Unit,
    onSaveTrail: () -> Unit,
    onLoadTrail: () -> Unit
) {
    val toggleTopPadding = if (isLandscape) 12.dp else 110.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Version Tag
        Text(
            text = BuildConfig.VERSION_NAME,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 6.dp, bottom = 44.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        if (mapState.showSettingsButton) {
            MapSettingsToggle(
                isMapButtonsVisible = mapState.isMapButtonsVisible,
                onToggle = { onMainEvent(UiEvent.SetMapButtonsVisible(!mapState.isMapButtonsVisible)) },
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = toggleTopPadding)
            )
        }

        if (mapState.isAnchorLocked) {
            AnchorLockedBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = toggleTopPadding + 8.dp)
            )
        }

        if (mapState.showToolsOverlay && mapState.isMapButtonsVisible) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).fillMaxHeight(0.85f).width(140.dp)) {
                    MapToolsOverlay(
                        state = mapState,
                        onClear = { onMainEvent(UiEvent.ClearTrails) },
                        onSave = onSaveTrail,
                        onLoad = onLoadTrail,
                        onEvent = onMainEvent
                    )
                }
            }
        }

        if (mapState.trackerLocPending && mapState.trackerLocPendingReason != LocationPendingReason.NONE) {
            Box(modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 82.dp)
                .background(Amber500.copy(alpha = 0.95f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "UNCERTAINTY: ${mapState.trackerLocPendingReason.name.replace("_", " ")}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
