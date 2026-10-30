package com.gps19.app.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gps19.core.ui.RevolutionTheme
import com.gps19.core.shared.ForensicIdentity
import com.gps19.core.network.ForensicIdentityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MonitorActivity: The primary UI for the Monitor (Watcher).
 * Hardened for Revol30.1: Uses unified ForensicIdentityManager for both peer and self identity.
 */
@AndroidEntryPoint
class MonitorActivity : ComponentActivity() {

    @Inject
    lateinit var identityManager: ForensicIdentityManager
    
    @Inject
    lateinit var dataStore: MonitorDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RevolutionTheme {
                val trackerId by identityManager.targetId.collectAsStateWithLifecycle(initialValue = null)
                val monitorId by identityManager.selfId.collectAsStateWithLifecycle(initialValue = "LOADING...")
                
                LaunchedEffect(Unit) {
                    identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
                }

                if (trackerId == null) {
                    PairingWizard(onPair = { id -> 
                        lifecycleScope.launch { identityManager.setTargetId(id) }
                    })
                } else {
                    MonitorScreen(
                        trackerId = trackerId!!,
                        monitorId = monitorId,
                        onReset = {
                            lifecycleScope.launch { identityManager.clearTargetId() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PairingWizard(onPair: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Watcher", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = text,
            onValueChange = { 
                text = it 
                isError = it.isNotEmpty() && !ForensicIdentity.isValid(it)
            },
            label = { Text("Enter Tracker ID") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("Invalid ID format (Expected 64-char hex)")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = { if (!isError && text.isNotEmpty()) onPair(text) },
            enabled = !isError && text.isNotEmpty(),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Pair Device")
        }
    }
}

@Composable
fun MonitorScreen(trackerId: String, monitorId: String, onReset: () -> Unit) {
    var isMapInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        isMapInitialized = true
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onReset() }
                        )
                    }
            ) {
                Column {
                    Text(text = "Tracking: ${trackerId.take(8)}...", style = MaterialTheme.typography.headlineMedium)
                    Text(text = "My ID: ${monitorId.take(8)}...", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Gray)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isMapInitialized) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Map Active")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Target: $trackerId", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
