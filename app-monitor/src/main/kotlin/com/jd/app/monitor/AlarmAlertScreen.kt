package com.jd.app.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jd.core.ui.StatusRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Tracker battery low since 14:03:12", or "... (cleared)" once the condition no longer holds. */
fun Alarm.description(trackerId: String?): String {
    val since = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(raisedAtMs))
    val tracker = trackerId?.let { " $it" } ?: ""
    return "${type.title.replaceFirst("Tracker", "Tracker$tracker")} since $since${if (isActive) "" else " (cleared)"}"
}

/** The full-screen alarm. It stays until [onAcknowledge], which silences the siren. */
@Composable
fun AlarmAlertScreen(pending: List<Alarm>, trackerId: String?, onAcknowledge: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = StatusRed) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("ALARM", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(24.dp))
            pending.forEach { alarm ->
                Text(
                    alarm.description(trackerId),
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onAcknowledge,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = StatusRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACKNOWLEDGE ALARM", fontWeight = FontWeight.Bold)
            }
        }
    }
}
