package com.gps19.app.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gps19.core.ui.RevolutionTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MonitorActivity: The primary UI for the Monitor (Watcher).
 * Corrected: Implements First-Run Pairing Wizard (Point 3).
 */
class MonitorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dependency Injection for DataStore (Simplified for skeleton)
        val dataStore = MonitorDataStore(this)

        setContent {
            RevolutionTheme {
                val trackerId by dataStore.targetTrackerId.collectAsStateWithLifecycle(initialValue = null)
                
                if (trackerId == null) {
                    // Point 3: Pairing Wizard for first-run users
                    PairingWizard(onPair = { id -> 
                        lifecycleScope.launch { dataStore.saveTargetTrackerId(id) }
                    })
                } else {
                    MonitorScreen(trackerId!!)
                }
            }
        }
    }
}

@Composable
fun PairingWizard(onPair: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Watcher", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter Tracker ID") }
        )
        Button(onClick = { onPair(text) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Pair Device")
        }
    }
}

@Composable
fun MonitorScreen(trackerId: String) {
    var isMapInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        isMapInitialized = true
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(text = "Tracking: $trackerId", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isMapInitialized) {
                Text(text = "Map Active")
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
