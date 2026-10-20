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
 * Oct.20.32:
 * - HUD Header Scrim (#1426): Replaced Transparent surface with Slate900 scrim 
 *   to ensure header buttons are visible over map content.
 * - Version Alignment (#1425): Reduced bottom padding and shifted version 
 *   tag to BottomStart to match user requested alignment and visibility.
 * Oct.14.8:
 * - SIMP-OCT13.21-S1: Pure Event HUD.
 */

@Composable
fun UnifiedHUDOverlay(
    stateProvider: UiStateProvider,
    onMainEvent: (UiEvent) -> Unit
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map Overlays (Lower Z-Order than Header/Status if overlapping)
        if (isMapVisible && !isAnyOverlayOpen && session.hydrationLevel >= 4) {
            MapSpecificOverlays(
                mapState = mapState,
                isLandscape = isLandscape,
                onMainEvent = onMainEvent
            )
        }

        // 2. Global Header and Status Bar
        if (!isAnyOverlayOpen && session.hydrationLevel >= 4) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    HeaderBar(
                        isLogVisible = nav.isLogVisible, isSettingsOpen = nav.isSettingsOpen, isRibbonsVisible = nav.isRibbonsVisible, isMapVisible = nav.isMapVisible, isPhoneSetupVisible = nav.isPhoneSetupVisible,
                        requiresExtraTopPadding = session.permissions.requiresExtraTopPadding, isSystemReady = isSystemReady, systemIssuesCount = systemIssuesCount,
                        onEvent = onMainEvent
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
                    // Issue #1426: Added Slate900 scrim for contrast
                    Surface(
                        color = if (isMapVisible) Slate900.copy(alpha = 0.6f) else MaterialTheme.colorScheme.background, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            HeaderBar(
                                isLogVisible = nav.isLogVisible, isSettingsOpen = nav.isSettingsOpen, isRibbonsVisible = nav.isRibbonsVisible, isMapVisible = nav.isMapVisible, isPhoneSetupVisible = nav.isPhoneSetupVisible,
                                requiresExtraTopPadding = session.permissions.requiresExtraTopPadding, isSystemReady = isSystemReady, systemIssuesCount = systemIssuesCount,
                                onEvent = onMainEvent
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
    onMainEvent: (UiEvent) -> Unit
) {
    val toggleTopPadding = if (isLandscape) 12.dp else 110.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Issue #1425: Version Tag alignment fix
        Text(
            text = BuildConfig.VERSION_NAME,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomStart) // Shifted to BottomStart per screenshot
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 8.dp, bottom = 4.dp) // Reduced bottom padding to pin to bottom
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
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
