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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun TimerDisplay(modifier: Modifier = Modifier) {
    var secondsRemaining by remember { mutableIntStateOf(600) }
    val minutes = secondsRemaining / 60

    // Coroutine to decrement timer by one second until finished
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000.milliseconds)
            secondsRemaining--
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, secondsRemaining % 60 ),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerDisplayPreview() {
    MeditateNowTheme {
        TimerDisplay()
    }
}