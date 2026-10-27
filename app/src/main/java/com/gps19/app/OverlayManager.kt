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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.*

/**
 * OverlayManager: Unified HUD management for GPS Tracker.
 * Oct.27.0:
 * - GAP-OCT26.5-1: Passed onMainEvent to GlobalStatusBar for conflation feedback.
 * Oct.26.4:
 * - ISSUE-OCT26.4-F1: Enforced R-ID 612 (Diagnostic Alignment). Moved version 
 *   tag to BottomEnd and enforced Ltr direction to resolve RTL-flip overlap 
 *   with map scale bar (which is now moved to the left).
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
    val isTracker = session.appMode == "tracker"
    
    val isSystemReady = session.isSystemReady(spatial.homePoints.size)
    val systemIssuesCount = session.systemIssuesCount(spatial.homePoints.size)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map Overlays
        if (isMapVisible && !isAnyOverlayOpen && session.hydrationLevel >= 4) {
            // Force Ltr to synchronize Compose coordinates with OSMDroid Map coordinates
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MapSpecificOverlays(
                    mapState = mapState,
                    isLandscape = isLandscape,
                    onMainEvent = onMainEvent
                )
            }
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
                                onEvent = onMainEvent,
                                modifier = Modifier.pointerInput(Unit) { 
                                    detectTapGestures(onTap = { 
                                        if (!isTracker) onMainEvent(UiEvent.SetRedScreenVisible(true)) 
                                    }) 
                                }
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().zIndex(10f)) {
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
                                    onEvent = onMainEvent,
                                    modifier = Modifier.pointerInput(Unit) { 
                                        detectTapGestures(onTap = { 
                                            if (!isTracker) onMainEvent(UiEvent.SetRedScreenVisible(true)) 
                                        }) 
                                    }
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
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Oct.26.4: Enforced R-ID 612. Diagnostic artifacts and version name 
            // MUST be at BottomEnd to ensure forensic parity across all variants.
            DiagnosticOverlay()
            
            Text(
                text = BuildConfig.VERSION_NAME,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

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
