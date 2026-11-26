package com.syntaxbhavesh.rudra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cyan = Color(0xFF53D8FF)
private val night = Color(0xFF050B16)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = night, surface = Color(0xFF101D31))) {
                RudraScreen(
                    onSpeak = { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "rudra") },
                    onListen = { success, failure -> listen(success, failure) },
                    onLaunch = { launchApp(it) }
                )
            }
        }
    }

    private fun listen(success: (String) -> Unit, failure: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { failure("Speech recognition is unavailable."); return }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(e: Int) { failure("I couldn't hear that. Please try again.") }
                override fun onResults(b: Bundle?) {
                    val words = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (words.isNullOrBlank()) failure("No speech detected.") else success(words)
                }
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
            sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            })
        }
    }

    private fun launchApp(name: String): Boolean {
        val intent = when (name.lowercase()) {
            "youtube" -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            "whatsapp" -> packageManager.getLaunchIntentForPackage("com.whatsapp")
            "chrome" -> packageManager.getLaunchIntentForPackage("com.android.chrome")
            "settings" -> Intent(android.provider.Settings.ACTION_SETTINGS)
            else -> null
        }
        if (intent == null) { tts?.speak("$name is not installed or cannot be opened.", TextToSpeech.QUEUE_FLUSH, null, "rudra"); return false }
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun RudraScreen(
    onSpeak: (String) -> Unit,
    onListen: ((String) -> Unit, (String) -> Unit) -> Unit,
    onLaunch: (String) -> Boolean
) {
    var message by remember { mutableStateOf("Your personal assistant is ready.") }
    var status by remember { mutableStateOf("SYSTEM READY") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            status = "LISTENING"
            onListen({ command ->
                status = "PROCESSING"
                val c = command.lowercase()
                val reply = when {
                    "youtube" in c -> { onLaunch("youtube"); "Opening YouTube." }
                    "whatsapp" in c -> { onLaunch("whatsapp"); "Opening WhatsApp." }
                    "chrome" in c -> { onLaunch("chrome"); "Opening Chrome." }
                    "settings" in c -> { onLaunch("settings"); "Opening Settings." }
                    "time" in c -> "The time is ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."
                    "help" in c -> "I can open YouTube, WhatsApp, Chrome and Settings, and tell you the time."
                    else -> "I heard: $command. AI connection is coming in the next phase."
                }
                message = reply
                status = "SYSTEM READY"
                onSpeak(reply)
            }, { message = it; status = "SYSTEM READY" })
        } else message = "Microphone permission is required for voice commands."
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1B32), night)))) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R.U.D.R.A.", color = cyan, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text("RESPONSIVE UTILITY • DIALOGUE • ROBOTIC ASSISTANT", color = Color.LightGray, fontSize = 8.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), color = Color.White, fontSize = 42.sp)
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(210.dp).border(2.dp, cyan.copy(alpha = .55f), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(160.dp).background(Color(0xFF123D62), CircleShape).border(2.dp, cyan, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, contentDescription = "RUDRA microphone", tint = Color.White, modifier = Modifier.size(54.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(status, color = cyan, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.height(12.dp))
                Text(message, color = Color.White, textAlign = TextAlign.Center)
            }
            Column {
                Button(
                    onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("TAP TO SPEAK", letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("YOUR VOICE. YOUR INTELLIGENCE. YOUR CONTROL.", color = cyan, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
