package com.example.meditatenow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.meditatenow.ui.theme.MeditateNowTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeditateNowTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimerDisplay(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Displays the countdown timer, session controls (Start/Pause/Resume/End),
 * a session length picker, and a completion dialog when the countdown reaches zero.
 */
@Composable
fun TimerDisplay(modifier: Modifier = Modifier) {
    var hasStarted by remember { mutableStateOf(false) } // Keeps track of whether timer has been started yet
    var isRunning by remember { mutableStateOf(false) } // Keeps track of whether timer is running right now
    var isSessionComplete by remember { mutableStateOf(false) } // Keeps track of whether session is completed

    var sessionLengthSeconds by remember { mutableIntStateOf(600) } // User-configured timer length
    var secondsRemaining by remember { mutableIntStateOf(sessionLengthSeconds) } // Live timer countdown

    var showDialog by remember { mutableStateOf(false) }

    // Temporary variables for picking timer length
    var tempMinutes by remember(showDialog) { mutableIntStateOf(sessionLengthSeconds / 60) }
    var tempSeconds by remember(showDialog) { mutableIntStateOf(sessionLengthSeconds % 60) }

    /**
     * Resets timer state to a fresh, unstarted session.
     * @param sessionComplete set true when resetting because the countdown
     * finished naturally, so the completion dialog is shown; false (default)
     * for a manual End, which resets silently.
     */
    fun resetSession(sessionComplete: Boolean = false) {
        isRunning = false
        hasStarted = false
        isSessionComplete = sessionComplete
        secondsRemaining = sessionLengthSeconds
    }

    // Restarts whenever isRunning changes; counts down while running and time remains
    LaunchedEffect(isRunning) {
        while (isRunning && secondsRemaining > 0) {
            delay(1000.milliseconds)
            secondsRemaining--
        }

        if (secondsRemaining == 0) {
            resetSession(true)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = formatTime(secondsRemaining), fontSize = 64.sp, fontWeight = FontWeight.Bold
        )

        Button(
            onClick = {
                isRunning = !isRunning
                hasStarted = true
                isSessionComplete = false
            }) {
            Text(
                text = when {
                    isRunning -> "Pause"
                    !hasStarted -> "Start"
                    else -> "Resume"
                }
            )
        }

        Button(
            onClick = { resetSession() },
            enabled = hasStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "End")
        }

        Button(
            onClick = { showDialog = true }, enabled = !hasStarted
        ) {
            Text("Set Length")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Set Session Length") },
                text = {
                    Column {
                        Text("Minutes: $tempMinutes")
                        Slider(
                            value = tempMinutes.toFloat(),
                            onValueChange = { tempMinutes = it.toInt() },
                            valueRange = 0f..60f
                        )
                        Text("Seconds: $tempSeconds")
                        Slider(
                            value = tempSeconds.toFloat(),
                            onValueChange = { tempSeconds = it.toInt() },
                            valueRange = 0f..59f
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        sessionLengthSeconds = toTotalSeconds(tempMinutes, tempSeconds)
                        secondsRemaining = sessionLengthSeconds
                        showDialog = false
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                })
        }

        if (isSessionComplete) {
            AlertDialog(
                onDismissRequest = { isSessionComplete = false },
                text = { Text("Session complete") },
                confirmButton = {
                    Button(onClick = { isSessionComplete = false }) {
                        Text("Dismiss")
                    }
                })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimerDisplayPreview() {
    MeditateNowTheme {
        TimerDisplay()
    }
}