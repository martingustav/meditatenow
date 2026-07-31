package com.example.meditatenow

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.meditatenow.ui.theme.MeditateNowTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

val COMPLETION_SOUNDS = listOf(
    "Small Bell" to R.raw.small_bell,
    "Temple Bell" to R.raw.temple_bell,
    "Tibetan Bell" to R.raw.tibetan_bell
)

/**
 * Plays the given sound resource once, releasing MediaPlayer
 * automatically when playback completes.
 */
fun playCompletionSound(context: Context, soundResId: Int): MediaPlayer {
    val mediaPlayer = MediaPlayer.create(context, soundResId)
    mediaPlayer.setOnCompletionListener { player -> player.release() }
    mediaPlayer.start()
    return mediaPlayer
}

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

    // Current context which passes to sound player
    val context = LocalContext.current

    // Tracks the currently playing preview sound (from the picker), so it can be stopped if the user picks another or closes the dialog
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var completionSoundResId by remember { mutableIntStateOf(COMPLETION_SOUNDS[0].second) } // User-configured completion sound
    var tempSoundResId by remember(showDialog) { mutableIntStateOf(completionSoundResId) } // Temporary variable for picking completion sound

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

    /**
     * Stops and releases the current media player unless it's null,
     * then sets the current media player to null.
     */
    fun stopMediaPlayer() {
        currentPlayer?.stop()
        currentPlayer?.release()
        currentPlayer = null
    }

    // Restarts whenever isRunning changes; counts down while running and time remains
    LaunchedEffect(isRunning) {
        while (isRunning && secondsRemaining > 0) {
            delay(1000.milliseconds)
            secondsRemaining--
        }

        if (secondsRemaining == 0) {
            resetSession(true)
            playCompletionSound(context, completionSoundResId)
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
            Text("Configure Session")
        }

        if (showDialog) {
            AlertDialog(onDismissRequest = {
                showDialog = false
                stopMediaPlayer()
            }, title = { Text("Configure Session") }, text = {
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
                    Text("Completion Sound")
                    COMPLETION_SOUNDS.forEach { (name, resId) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempSoundResId == resId, onClick = {
                                    stopMediaPlayer()
                                    tempSoundResId = resId
                                    currentPlayer = playCompletionSound(context, resId)
                                })
                            Text(text = name)
                        }
                    }
                }
            }, confirmButton = {
                Button(onClick = {
                    sessionLengthSeconds = toTotalSeconds(tempMinutes, tempSeconds)
                    secondsRemaining = sessionLengthSeconds
                    showDialog = false
                    completionSoundResId = tempSoundResId
                    stopMediaPlayer()
                }) {
                    Text("Confirm")
                }
            }, dismissButton = {
                Button(onClick = {
                    showDialog = false
                    stopMediaPlayer()
                }) {
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