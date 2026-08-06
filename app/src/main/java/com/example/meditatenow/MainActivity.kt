package com.example.meditatenow

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
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
import androidx.compose.material3.Switch
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.meditatenow.ui.theme.MeditateNowTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the app's session lifecycle; RUNNING and PAUSED
 * states toggle back and forth within each phase before advancing:
 * 1. IDLE, where no session is currently running.
 * 2. (Optional) COUNTDOWN_RUNNING, where the optional
 * pre-session countdown is currently running.
 * 3. (Optional) COUNTDOWN_PAUSED, where the optional
 * pre-session countdown is currently paused.
 * 4. SESSION_RUNNING, where the main timer session is
 * currently running.
 * 5. SESSION_PAUSED, where the main timer session is
 * currently paused.
 * 6. SESSION_FINISHED, where the main timer session is
 * finished and the in-app completion dialog and optional
 * system completion notification are displayed.
 */
enum class SessionState {
    IDLE, COUNTDOWN_RUNNING, COUNTDOWN_PAUSED, SESSION_RUNNING, SESSION_PAUSED, SESSION_FINISHED
}

val SOUNDS = listOf(
    "Small Bell" to R.raw.small_bell,
    "Temple Bell" to R.raw.temple_bell,
    "Tibetan Bell" to R.raw.tibetan_bell
)

const val NOTIFICATION_CHANNEL_ID = "session_complete_v2"

/**
 * Plays the given sound resource once, releasing MediaPlayer
 * automatically when playback completes.
 */
fun playSound(context: Context, soundResId: Int): MediaPlayer {
    val mediaPlayer = MediaPlayer.create(context, soundResId)
    mediaPlayer.setOnCompletionListener { player -> player.release() }
    mediaPlayer.start()
    return mediaPlayer
}

/**
 * Sends a session completion system notification if the user has
 * granted notification permissions. If not, it does nothing.
 */
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun sendCompletionNotification(context: Context) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Session complete")
        .setContentText("Your meditation session has finished.").setAutoCancel(true)
        .setContentIntent(pendingIntent).build()

    val notificationManager = NotificationManagerCompat.from(context)
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        notificationManager.notify(1, notification)
    }
}

/**
 * Displays a list of sound options as radio buttons, calling [onSelect]
 * with the tapped option's resource ID.
 */
@Composable
fun SoundPicker(selectedResId: Int, onSelect: (Int) -> Unit) {
    SOUNDS.forEach { (name, resId) ->
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedResId == resId, onClick = {
                    onSelect(resId)
                })
            Text(text = name)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, "Session Complete", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
        }

        val notificationManager = this.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
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
 * a session configuration dialog (length, optional start sound, completion sound,
 * optional pre-session countdown), an in-app completion dialog when the countdown
 * reaches zero, and a system notification if app is backgrounded.
 */
@Composable
fun TimerDisplay(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(SessionState.IDLE) }

    var sessionLengthSeconds by remember { mutableIntStateOf(600) } // User-configured session length
    var sessionSecondsRemaining by remember { mutableIntStateOf(sessionLengthSeconds) } // Live session countdown

    var showSessionConfigurationDialog by remember { mutableStateOf(false) }

    // Temporary variables for picking session length
    var tempSessionLengthMinutes by remember(showSessionConfigurationDialog) {
        mutableIntStateOf(
            sessionLengthSeconds / 60
        )
    }
    var tempSessionLengthSeconds by remember(showSessionConfigurationDialog) {
        mutableIntStateOf(
            sessionLengthSeconds % 60
        )
    }

    // Current context which passes to sound player
    val context = LocalContext.current

    // Used to check whether the app is foregrounded, to decide whether to send a notification
    val lifecycleOwner = LocalLifecycleOwner.current

    // Tracks the currently playing preview sound (from the picker), so it can be stopped if the user picks another or closes the dialog
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var endSoundResId by remember { mutableIntStateOf(SOUNDS[0].second) } // User-configured completion sound
    var tempEndSoundResId by remember(showSessionConfigurationDialog) {
        mutableIntStateOf(
            endSoundResId
        )
    } // Temporary variable for picking completion sound

    var startSoundEnabled by remember { mutableStateOf(false) } // Toggle for optional session start sound
    var startSoundResId by remember { mutableIntStateOf(SOUNDS[0].second) } // User-configured session start sound
    var tempStartSoundEnabled by remember(showSessionConfigurationDialog) {
        mutableStateOf(
            startSoundEnabled
        )
    } // Temporary variable for session start sound state (enabled/disabled)
    var tempStartSoundResId by remember(showSessionConfigurationDialog) {
        mutableIntStateOf(
            startSoundResId
        )
    } // Temporary variable for picking session start sound

    var countdownEnabled by remember { mutableStateOf(false) } // Toggle for optional session countdown
    var tempCountdownEnabled by remember(showSessionConfigurationDialog) {
        mutableStateOf(
            countdownEnabled
        )
    } // Temporary variable for session countdown state (enabled/disabled)
    var countdownLengthSeconds by remember { mutableIntStateOf(5) } // Session countdown length
    var countdownSecondsRemaining by remember { mutableIntStateOf(countdownLengthSeconds) }
    var tempCountdownLength by remember(showSessionConfigurationDialog) {
        mutableIntStateOf(
            countdownLengthSeconds
        )
    } // Temporary variable for picking session countdown length

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    /**
     * Request notification permission if not granted and
     * if Android API level is above 33 (Android 13).
     */
    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val currentStatus = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            )
            if (currentStatus != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Changes the state to pre-session countdown.
     */
    fun startCountdown() {
        countdownSecondsRemaining = countdownLengthSeconds
        state = SessionState.COUNTDOWN_RUNNING
    }

    /**
     * Changes state to running,
     * plays a start sound if enabled.
     */
    fun startSession() {
        state = SessionState.SESSION_RUNNING

        if (startSoundEnabled) {
            playSound(context, startSoundResId)
        }
    }

    /**
     * Starts a pre-session countdown if enabled,
     * then starts the main session.
     */
    fun start() {
        requestNotificationPermissionIfNeeded()
        if (countdownEnabled) {
            startCountdown()
        } else {
            startSession()
        }
    }

    /**
     * Changes state to finished, plays the end sound,
     * and sends a system notification if the app is
     * in the background.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun finishSession() {
        state = SessionState.SESSION_FINISHED
        playSound(context, endSoundResId)
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast((Lifecycle.State.RESUMED))) {
            sendCompletionNotification(context)
        }
        sessionSecondsRemaining = sessionLengthSeconds
    }

    /**
     * Resets timer state to a fresh, unstarted session.
     */
    fun resetSession() {
        state = SessionState.IDLE
        sessionSecondsRemaining = sessionLengthSeconds
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

    // Restarts whenever state changes
    LaunchedEffect(state) {
        when (state) {
            SessionState.COUNTDOWN_RUNNING -> {
                while (countdownSecondsRemaining > 0) {
                    delay(1000.milliseconds)
                    countdownSecondsRemaining--
                }
                startSession()
            }

            SessionState.SESSION_RUNNING -> {
                while (sessionSecondsRemaining > 0) {
                    delay(1000.milliseconds)
                    sessionSecondsRemaining--
                }
                finishSession()
            }

            else -> {} // Nothing to do for IDLE, PAUSED, or FINISHED states
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = when (state) {
                SessionState.COUNTDOWN_RUNNING, SessionState.COUNTDOWN_PAUSED -> formatTime(
                    countdownSecondsRemaining
                )

                SessionState.IDLE -> formatTime(sessionLengthSeconds)
                else -> formatTime(sessionSecondsRemaining)
            }, fontSize = 64.sp, fontWeight = FontWeight.Bold
        )

        Button(
            onClick = {
                when (state) {
                    SessionState.IDLE -> start()
                    SessionState.SESSION_RUNNING -> state = SessionState.SESSION_PAUSED
                    SessionState.SESSION_PAUSED -> state = SessionState.SESSION_RUNNING
                    SessionState.SESSION_FINISHED -> {} // Dialog is showing; ignore button presses
                    SessionState.COUNTDOWN_RUNNING -> state = SessionState.COUNTDOWN_PAUSED
                    SessionState.COUNTDOWN_PAUSED -> state = SessionState.COUNTDOWN_RUNNING
                }
            }) {
            Text(
                text = when (state) {
                    SessionState.IDLE -> "Start"
                    SessionState.SESSION_RUNNING -> "Pause"
                    SessionState.SESSION_PAUSED -> "Resume"
                    SessionState.SESSION_FINISHED -> "" // Show nothing
                    SessionState.COUNTDOWN_RUNNING -> "Pause"
                    SessionState.COUNTDOWN_PAUSED -> "Resume"
                }
            )
        }

        Button(
            onClick = {
                resetSession()
                stopMediaPlayer()
            }, enabled = state != SessionState.IDLE, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "End")
        }

        Button(
            onClick = { showSessionConfigurationDialog = true },
            enabled = state == SessionState.IDLE
        ) {
            Text("Configure Session")
        }

        if (showSessionConfigurationDialog) {
            AlertDialog(onDismissRequest = {
                showSessionConfigurationDialog = false
                stopMediaPlayer()
            }, title = { Text("Configure Session") }, text = {
                Column {
                    Text("Minutes: $tempSessionLengthMinutes")
                    Slider(
                        value = tempSessionLengthMinutes.toFloat(),
                        onValueChange = { tempSessionLengthMinutes = it.toInt() },
                        valueRange = 0f..60f
                    )
                    Text("Seconds: $tempSessionLengthSeconds")
                    Slider(
                        value = tempSessionLengthSeconds.toFloat(),
                        onValueChange = { tempSessionLengthSeconds = it.toInt() },
                        valueRange = 0f..59f
                    )
                    Text("Countdown")
                    Switch(
                        checked = tempCountdownEnabled,
                        onCheckedChange = { tempCountdownEnabled = it })

                    if (tempCountdownEnabled) {
                        Text("Seconds: $tempCountdownLength")
                        Slider(
                            value = tempCountdownLength.toFloat(),
                            onValueChange = { tempCountdownLength = it.toInt() },
                            valueRange = 0f..30f
                        )
                    }

                    Text("End Sound")
                    SoundPicker(tempEndSoundResId) { resId ->
                        stopMediaPlayer()
                        tempEndSoundResId = resId
                        currentPlayer = playSound(context, resId)
                    }

                    Text("Start Sound")
                    Switch(
                        checked = tempStartSoundEnabled,
                        onCheckedChange = { tempStartSoundEnabled = it })

                    if (tempStartSoundEnabled) {
                        SoundPicker(tempStartSoundResId) { resId ->
                            stopMediaPlayer()
                            tempStartSoundResId = resId
                            currentPlayer = playSound(context, resId)
                        }
                    }
                }
            }, confirmButton = {
                Button(onClick = {
                    sessionLengthSeconds =
                        toTotalSeconds(tempSessionLengthMinutes, tempSessionLengthSeconds)
                    sessionSecondsRemaining = sessionLengthSeconds
                    showSessionConfigurationDialog = false
                    endSoundResId = tempEndSoundResId
                    startSoundResId = tempStartSoundResId
                    startSoundEnabled = tempStartSoundEnabled
                    countdownLengthSeconds = tempCountdownLength
                    countdownEnabled = tempCountdownEnabled
                    stopMediaPlayer()
                }) {
                    Text("Confirm")
                }
            }, dismissButton = {
                Button(onClick = {
                    showSessionConfigurationDialog = false
                    stopMediaPlayer()
                }) {
                    Text("Cancel")
                }
            })
        }

        if (state == SessionState.SESSION_FINISHED) {
            AlertDialog(
                onDismissRequest = { state = SessionState.IDLE },
                text = { Text("Session complete") },
                confirmButton = {
                    Button(onClick = { state = SessionState.IDLE }) {
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