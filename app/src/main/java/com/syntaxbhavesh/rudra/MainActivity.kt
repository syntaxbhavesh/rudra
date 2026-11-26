package com.syntaxbhavesh.rudra

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

private val cyan = Color(0xFF53D8FF)
private val night = Color(0xFF050B16)
private val panel = Color(0xFF102039)

data class ChatMessage(val id: String = UUID.randomUUID().toString(), val role: String, val text: String)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var wakeWordListener: WakeWordListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { result -> if (result == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = night, surface = panel)) {
                RudraScreen(
                    onSpeak = { speak(it) },
                    onListen = { success, failure -> listen(success, failure) },
                    onWakeWordStart = { onWake, onState -> startWakeWord(onWake, onState) },
                    onStopListening = { stopAllListening() },
                    initialMessages = loadChatHistory(),
                    onSaveMessages = { saveChatHistory(it) },
                    onLocalCommand = { localCommand(it) },
                    onAskShiva = { history, onChunk, onComplete -> askShiva(history, onChunk, onComplete) }
                )
            }
        }
    }

    private fun loadChatHistory(): List<ChatMessage> {
        val raw = getSharedPreferences("rudra_chat", MODE_PRIVATE).getString("messages", null)
            ?: return listOf(ChatMessage(role = "assistant", text = "RUDRA online. Ask me anything, or tap the microphone to speak."))
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.getJSONObject(index)
                val role = item.optString("role")
                val text = item.optString("text")
                if ((role == "user" || role == "assistant") && text.isNotBlank()) {
                    ChatMessage(id = item.optString("id").ifBlank { UUID.randomUUID().toString() }, role = role, text = text)
                } else null
            }.takeLast(200).ifEmpty {
                listOf(ChatMessage(role = "assistant", text = "RUDRA online. Ask me anything, or tap the microphone to speak."))
            }
        }.getOrElse {
            listOf(ChatMessage(role = "assistant", text = "RUDRA online. Your saved chat could not be read, so a fresh conversation is ready."))
        }
    }

    private fun saveChatHistory(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.filter { it.text.isNotBlank() }.takeLast(200).forEach { message ->
            array.put(JSONObject().put("id", message.id).put("role", message.role).put("text", message.text))
        }
        getSharedPreferences("rudra_chat", MODE_PRIVATE).edit().putString("messages", array.toString()).apply()
    }

    private fun speak(text: String) {
        if (text.isNotBlank()) tts?.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "rudra-response")
    }

    private fun listen(success: (String) -> Unit, failure: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { failure("Speech recognition is unavailable on this device."); return }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { failure("I couldn't hear that. Please try again.") }
                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (words.isNullOrBlank()) failure("No speech detected.") else success(words)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            })
        }
    }

    private fun startWakeWord(onWake: () -> Unit, onState: (String) -> Unit) {
        stopAllListening()
        wakeWordListener = WakeWordListener(this, onWake, onState).also { it.start() }
    }

    private fun stopAllListening() {
        wakeWordListener?.stop()
        wakeWordListener = null
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null
    }

    private fun launchApp(name: String): Boolean {
        val intent = when (name.lowercase()) {
            "youtube" -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            "whatsapp" -> packageManager.getLaunchIntentForPackage("com.whatsapp")
            "chrome" -> packageManager.getLaunchIntentForPackage("com.android.chrome")
            "settings" -> Intent(android.provider.Settings.ACTION_SETTINGS)
            else -> null
        }
        if (intent == null) return false
        return try { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (_: Exception) { false }
    }

    private fun localCommand(raw: String): String? {
        val command = raw.trim().lowercase(Locale.getDefault())
        return when {
            command == "time" || command.contains("what time is it") -> "The time is ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."
            command == "date" || command.contains("what is the date") -> "Today is ${SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())}."
            command == "help" || command == "what can you do" -> "I can answer questions using Shiva AI, speak replies, and open YouTube, WhatsApp, Chrome or Android Settings."
            command.startsWith("open youtube") -> if (launchApp("youtube")) "Opening YouTube." else "YouTube could not be opened on this device."
            command.startsWith("open whatsapp") -> if (launchApp("whatsapp")) "Opening WhatsApp." else "WhatsApp could not be opened on this device."
            command.startsWith("open chrome") -> if (launchApp("chrome")) "Opening Chrome." else "Chrome could not be opened on this device."
            command.startsWith("open settings") -> if (launchApp("settings")) "Opening Android Settings." else "Settings could not be opened."
            else -> null
        }
    }

    private fun askShiva(history: List<ChatMessage>, onChunk: (String) -> Unit, onComplete: (String?) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            var connection: HttpURLConnection? = null
            try {
                val endpoint = BuildConfig.SHIVA_API_BASE_URL.trimEnd('/') + "/api/chat"
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 90000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "text/event-stream")
                }
                val messages = JSONArray()
                history.filter { it.text.isNotBlank() && (it.role == "user" || it.role == "assistant") }.forEach { message ->
                    val parts = JSONArray().put(JSONObject().put("type", "text").put("text", message.text))
                    messages.put(JSONObject().put("id", message.id).put("role", message.role).put("parts", parts))
                }
                val body = JSONObject().put("messages", messages)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                if (code !in 200..299) {
                    val errorBody = reader.use { it.readText() }
                    val json = runCatching { JSONObject(errorBody) }.getOrNull()
                    val detail = json?.optString("detail")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("error")?.takeIf { it.isNotBlank() }
                        ?: "Shiva AI returned HTTP $code."
                    throw IllegalStateException(detail)
                }

                val answer = StringBuilder()
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (line.startsWith("data:")) {
                            val payload = line.removePrefix("data:").trim()
                            if (payload.isNotEmpty() && payload != "[DONE]") {
                                val event = runCatching { JSONObject(payload) }.getOrNull()
                                if (event?.optString("type") == "text-delta") {
                                    val delta = event.optString("delta")
                                    if (delta.isNotEmpty()) {
                                        answer.append(delta)
                                        val current = answer.toString()
                                        mainHandler.post { onChunk(current) }
                                    }
                                } else if (event?.optString("type") == "error") {
                                    throw IllegalStateException(event.optString("error", "Shiva AI stream error."))
                                }
                            }
                        }
                    }
                }
                val finalText = answer.toString().trim()
                if (finalText.isBlank()) throw IllegalStateException("Shiva AI returned an empty response.")
                mainHandler.post { onComplete(finalText) }
            } catch (error: Exception) {
                val message = when (error) {
                    is java.net.SocketTimeoutException -> "Shiva AI took too long to respond. Please try again."
                    else -> error.message ?: "Could not connect to Shiva AI."
                }
                mainHandler.post { onComplete("Connection error: $message") }
            } finally { connection?.disconnect() }
        }
    }

    override fun onStop() {
        stopAllListening()
        super.onStop()
    }

    override fun onDestroy() {
        stopAllListening()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun RudraScreen(
    onSpeak: (String) -> Unit,
    onListen: ((String) -> Unit, (String) -> Unit) -> Unit,
    onWakeWordStart: (() -> Unit, (String) -> Unit) -> Unit,
    onStopListening: () -> Unit,
    initialMessages: List<ChatMessage>,
    onSaveMessages: (List<ChatMessage>) -> Unit,
    onLocalCommand: (String) -> String?,
    onAskShiva: (List<ChatMessage>, (String) -> Unit, (String?) -> Unit) -> Unit
) {
    var messages by remember { mutableStateOf(initialMessages) }
    var input by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("SHIVA AI CONNECTED") }
    var sending by remember { mutableStateOf(false) }
    var wakeEnabled by remember { mutableStateOf(false) }
    var listeningStatus by remember { mutableStateOf("Wake word off") }
    var pendingWakeEnable by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (pendingWakeEnable) {
                pendingWakeEnable = false
                wakeEnabled = true
                onWakeWordStart({
                    wakeEnabled = false
                    listeningStatus = "Wake word detected — speak now"
                    onListen({ spoken ->
                        status = "PROCESSING"
                        submitMessage(spoken, messages, onLocalCommand, onAskShiva, onSpeak,
                            { messages = it }, { sending = it }, { status = it })
                    }, { error ->
                        messages = messages + ChatMessage(role = "assistant", text = error)
                        listeningStatus = "Wake word on — waiting"
                        if (wakeEnabled) onWakeWordStart({}, { listeningStatus = it })
                    })
                }, { listeningStatus = it })
            } else {
            status = "LISTENING"
            onListen({ spoken ->
                status = "PROCESSING"
                submitMessage(spoken, messages, onLocalCommand, onAskShiva, onSpeak,
                    { messages = it }, { sending = it }, { status = it })
            }, { messages = messages + ChatMessage(role = "assistant", text = it); status = "SHIVA AI CONNECTED" })
            }
        } else {
            pendingWakeEnable = false
            wakeEnabled = false
            listeningStatus = "Microphone permission denied"
            messages = messages + ChatMessage(role = "assistant", text = "Microphone permission is needed for voice input. You can still type messages.")
        }
    }

    LaunchedEffect(messages) {
        onSaveMessages(messages)
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear conversation?") },
            text = { Text("This removes the chat saved on this device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    messages = listOf(ChatMessage(role = "assistant", text = "RUDRA online. Your conversation has been cleared."))
                    showClearDialog = false
                }) { Text("Clear chat", color = Color(0xFFFF8A80)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
            containerColor = panel,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFD3DFEC)
        )
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1B32), night)))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("R.U.D.R.A.", color = cyan, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("RESPONSIVE UTILITY • DIALOGUE • ROBOTIC ASSISTANT", color = Color.LightGray, fontSize = 7.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("AI LINK", color = Color.LightGray, fontSize = 9.sp)
                        Text(status, color = if (sending || status == "LISTENING") Color(0xFFFFC857) else cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { if (!sending) showClearDialog = true }, enabled = !sending) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear conversation", tint = Color(0xFF9CB6D4))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (sending) Color(0xFFFFC857) else cyan, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("SHIVA INTELLIGENCE CORE", color = Color(0xFF9CB6D4), fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(10.dp))

            Surface(
                color = if (wakeEnabled) Color(0xFF183B35) else Color(0xFF13243A),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (wakeEnabled) Color(0xFF43D9A3) else Color(0xFF34516F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(9.dp).background(if (wakeEnabled) Color(0xFF43D9A3) else Color(0xFF7890AA), CircleShape))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (wakeEnabled) "MICROPHONE ACTIVE" else "WAKE WORD DISABLED",
                            color = if (wakeEnabled) Color(0xFF7CF0C1) else Color(0xFFB7C7D9),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(listeningStatus, color = Color(0xFFB7C7D9), fontSize = 9.sp)
                    }
                    Switch(checked = wakeEnabled, onCheckedChange = { enabled ->
                        if (enabled && !sending) {
                            pendingWakeEnable = true
                            permission.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (!enabled) {
                            pendingWakeEnable = false
                            wakeEnabled = false
                            listeningStatus = "Wake word off"
                            onStopListening()
                        }
                    })
                }
            }
            Text("Only active while RUDRA is on screen. Audio may be processed by your device’s configured speech-recognition provider.",
                color = Color(0xFF8199B5), fontSize = 9.sp, lineHeight = 13.sp,
                modifier = Modifier.padding(top = 5.dp, bottom = 5.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isUser = message.role == "user"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                        Surface(
                            color = if (isUser) Color(0xFF164B70) else panel,
                            shape = RoundedCornerShape(
                                topStart = 18.dp, topEnd = 18.dp,
                                bottomStart = if (isUser) 18.dp else 5.dp,
                                bottomEnd = if (isUser) 5.dp else 18.dp
                            ),
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Column(Modifier.padding(13.dp)) {
                                Text(if (isUser) "YOU" else "RUDRA", color = if (isUser) Color(0xFFBDEBFF) else cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(message.text.ifBlank { "Thinking…" }, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }

            if (sending) Text("SHIVA IS THINKING…", color = cyan, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 7.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { if (!sending) { wakeEnabled = false; pendingWakeEnable = false; onStopListening(); permission.launch(Manifest.permission.RECORD_AUDIO) } },
                    enabled = !sending,
                    modifier = Modifier.size(50.dp).background(Color(0xFF123D62), CircleShape).border(1.dp, cyan.copy(alpha = .6f), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Speak", tint = cyan)
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message RUDRA…", color = Color(0xFF91A7C1)) },
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val text = input.trim()
                        if (text.isNotEmpty() && !sending) {
                            input = ""
                            submitMessage(text, messages, onLocalCommand, onAskShiva, onSpeak,
                                { messages = it }, { sending = it }, { status = it })
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = cyan, unfocusedBorderColor = Color(0xFF34516F), cursorColor = cyan
                    )
                )
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty() && !sending) {
                            input = ""
                            submitMessage(text, messages, onLocalCommand, onAskShiva, onSpeak,
                                { messages = it }, { sending = it }, { status = it })
                        }
                    },
                    enabled = input.isNotBlank() && !sending,
                    modifier = Modifier.size(50.dp).background(if (input.isNotBlank() && !sending) cyan else Color(0xFF26394F), CircleShape)
                ) { Icon(Icons.Default.Send, contentDescription = "Send message", tint = night) }
            }
            Spacer(Modifier.height(5.dp))
            Text("VOICE + CHAT • POWERED BY SHIVA AI", color = Color(0xFF8DA7C5), fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun submitMessage(
    text: String,
    current: List<ChatMessage>,
    localCommand: (String) -> String?,
    askShiva: (List<ChatMessage>, (String) -> Unit, (String?) -> Unit) -> Unit,
    speak: (String) -> Unit,
    updateMessages: (List<ChatMessage>) -> Unit,
    updateSending: (Boolean) -> Unit,
    updateStatus: (String) -> Unit
) {
    val history = current + ChatMessage(role = "user", text = text)
    val localReply = localCommand(text)
    if (localReply != null) {
        updateMessages(history + ChatMessage(role = "assistant", text = localReply))
        updateStatus("SYSTEM READY")
        speak(localReply)
        return
    }
    val placeholder = ChatMessage(role = "assistant", text = "")
    updateMessages(history + placeholder)
    updateSending(true)
    updateStatus("CONTACTING SHIVA")
    askShiva(history, { partial ->
        updateMessages(history + placeholder.copy(text = partial))
    }) { result ->
        val finalReply = result ?: "Shiva AI could not complete the response."
        updateMessages(history + placeholder.copy(text = finalReply))
        updateSending(false)
        updateStatus(if (result == null || finalReply.startsWith("Connection error:")) "CONNECTION ISSUE" else "SHIVA AI CONNECTED")
        if (result != null && !finalReply.startsWith("Connection error:")) speak(finalReply)
    }
}
