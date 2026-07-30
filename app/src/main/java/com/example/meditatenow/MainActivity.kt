package com.example.meditatenow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.meditatenow.ui.theme.MeditateNowTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.util.Locale
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
 * Displays the countdown timer and Start/Pause/Resume/End controls
 * for a single meditation session.
 */
@Composable
fun TimerDisplay(modifier: Modifier = Modifier) {
    var hasStarted by remember { mutableStateOf(false) } // Keeps track of whether timer has been started yet
    var isRunning by remember { mutableStateOf(false) } // Keeps track of whether timer is running right now
    var secondsRemaining by remember { mutableIntStateOf(600) }
    val minutes = secondsRemaining / 60

    // Restarts whenever isRunning changes; counts down while running and time remains
    LaunchedEffect(isRunning) {
        while (isRunning && secondsRemaining > 0) {
            delay(1000.milliseconds)
            secondsRemaining--
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer text
        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, secondsRemaining % 60),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )
        // Start/pause/resume button
        Button(
            onClick = {
                isRunning = !isRunning
                hasStarted = true
            }
        ) {
            Text(
                text = when {
                    isRunning -> "Pause"
                    !hasStarted -> "Start"
                    else -> "Resume"
                }
            )
        }
        // End button
        Button(
            // End must always stop and reset, regardless of current state, so isRunning
            // is force-set to false here rather than toggled like in the Start/Pause button
            onClick = {
                isRunning = false
                hasStarted = false
                secondsRemaining = 600
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "End")
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