package com.github.olga_yakovleva.rhvoice.compose.blind_navigator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.assistantapp.sendFrameToGemini2AI
import com.example.assistantapp.sendMessageToGeminiAI
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun BlindModeScreen() {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current
    LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var currentMode by remember { mutableStateOf("navigation") }
    var overlayText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isAssistantMode by remember { mutableStateOf(false) }
    var sessionStarted by remember { mutableStateOf(true) } // Start session immediately
    var analysisResult by remember { mutableStateOf("") }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var lastSpokenIndex by remember { mutableStateOf(0) }
    var lastProcessedTimestamp by remember { mutableStateOf(0L) }
    val frameInterval = 12000 // Process a frame every 6.5 seconds
    var navigationPaused by remember { mutableStateOf(false) }
    var isMicActive by remember { mutableStateOf(false) }
    var chatResponse by remember { mutableStateOf("") }
    var isReadingMode by remember { mutableStateOf(false) }
    var readingModeResult by remember { mutableStateOf("") }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    LaunchedEffect(context) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(1.5f) // Increase the speech rate
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.value?.stop()
            tts.value?.shutdown()
            speechRecognizer.destroy()
        }
    }

    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    coroutineScope.launch {
                        chatResponse = sendMessageToGeminiAI(spokenText, analysisResult)
                        tts.value?.speak(chatResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // Restart listening on end of speech, if navigation is paused
                if (navigationPaused) {
                    speechRecognizer.startListening(speechIntent)
                }
            }
            override fun onError(error: Int) {
                // Restart listening on error, if navigation is paused
                if (navigationPaused) {
                    speechRecognizer.startListening(speechIntent)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // Effect to handle microphone activation when navigation is paused
    LaunchedEffect(navigationPaused) {
        if (navigationPaused) {
            isMicActive = true
            speechRecognizer.startListening(speechIntent)
        } else {
            isMicActive = false
            speechRecognizer.stopListening()
            // Clear chatResponse to display the analysis result when resuming navigation
            chatResponse = ""
        }
    }

    if (hasPermission) {
        if (sessionStarted) {
            if (isReadingMode) {
                ReadingModeCamera(
                    onImageCaptured = { bitmap: Bitmap ->
                        capturedImage = bitmap
                        coroutineScope.launch {
                            readingModeResult = ""
                            sendFrameToGemini2AI(bitmap, { partialResult ->
                                readingModeResult += partialResult
                                tts.value?.speak(partialResult, TextToSpeech.QUEUE_ADD, null, null)
                            }, { error ->
                                // Handle error
                            })
                        }
                    },

                    cameraExecutor = cameraExecutor
                )
            } else if (!navigationPaused) {
                CameraPreviewWithAnalysis { imageProxy ->
                    val currentTimestamp = System.currentTimeMillis()
                    if (currentTimestamp - lastProcessedTimestamp >= frameInterval) {
                        coroutineScope.launch {
                            val bitmap = imageProxy.toBitmap()
                            if (bitmap != null) {
                                sendFrameToGeminiAI(bitmap, { partialResult ->
                                    analysisResult += " $partialResult"
                                    val newText = analysisResult.substring(lastSpokenIndex)
                                    tts.value?.speak(newText, TextToSpeech.QUEUE_ADD, null, null)
                                    lastSpokenIndex = analysisResult.length
                                }, { error ->
                                    // Handle error here
                                })
                                lastProcessedTimestamp = currentTimestamp
                            }
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }
            }
        }
    } else {
        ActivityCompat.requestPermissions(
            (context as Activity),
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            1
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isReadingMode) {
                            navigationPaused = !navigationPaused
                            isAssistantMode = navigationPaused
                            if (navigationPaused) {
                                tts.value?.stop()
                                currentMode = "assistant"
                                overlayText = ""
                                tts.value?.speak("Kömekçi rejimi işjeňleşdirildi.", TextToSpeech.QUEUE_FLUSH, null, null)
                            } else {
                                tts.value?.stop()
                                currentMode = "navigation"
                                overlayText = ""
                                chatResponse = ""
                                tts.value?.speak("Kömekçi rejimi ýapyldy.", TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        }
                    },
                    onLongPress = {
                        if (!isAssistantMode) {
                            isReadingMode = !isReadingMode
                            if (isReadingMode) {
                                tts.value?.stop()
                                currentMode = "reading"
                                overlayText = ""
                                navigationPaused = true
                                tts.value?.speak("Okamak rejimine girýäris", TextToSpeech.QUEUE_FLUSH, null, null)
                            } else {
                                tts.value?.stop()
                                currentMode = "navigation"
                                overlayText = ""
                                readingModeResult = ""
                                navigationPaused = false
                                tts.value?.speak("Okamak rejiminden çykýarys", TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        } else {
                            // Exit assistant mode and enter navigation mode
                            tts.value?.stop()
                            isAssistantMode = false
                            navigationPaused = false
                            isReadingMode = false
                            currentMode = "navigation"
                            overlayText = ""
                            chatResponse = ""
                            tts.value?.speak("Kömekçi rejiminden çykyp, nawigasiýa rejimine girýäris", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                )
            }
    ) {
        // Status overlay at top
        ModernStatusBar(
            currentMode = currentMode,
            isReadingMode = isReadingMode,
            isAssistantMode = isAssistantMode,
            isMicActive = isMicActive
        )

        // AI Response overlay in center
        if (sessionStarted) {
            AIResponseOverlay(
                currentMode = currentMode,
                navigationResponse = analysisResult,
                response = analysisResult,
                chatResponse = chatResponse,
                readingModeResult = readingModeResult,
                tts = tts.value,
                lastSpokenIndex = lastSpokenIndex
            )
        }

        // Modern side indicators
        ModernSideIndicators(
            isReadingMode = isReadingMode,
            isMicActive = isMicActive,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
fun ModernStatusBar(
    currentMode: String,
    isReadingMode: Boolean,
    isAssistantMode: Boolean,
    isMicActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modeColor = when {
                    isReadingMode -> Color(0xFF4CAF50)
                    isAssistantMode -> Color(0xFF2196F3)
                    else -> Color(0xFFFF9800)
                }
                
                val modeIcon = when {
                    isReadingMode -> Icons.Default.Book
                    isAssistantMode -> Icons.Default.RecordVoiceOver
                    else -> Icons.Default.Navigation
                }
                
                val modeText = when {
                    isReadingMode -> "Okamak"
                    isAssistantMode -> "Kömekçi"
                    else -> "Nawigasıýa"
                }
                
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape,
                    color = modeColor.copy(alpha = pulseAlpha)
                ) {}
                
                Icon(
                    imageVector = modeIcon,
                    contentDescription = null,
                    tint = modeColor,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = modeText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Instructions text
            Text(
                text = when {
                    isReadingMode -> "Tekst okamak üçin ekrany uzak basyň"
                    isAssistantMode -> "Sorag bermek üçin gürle"
                    else -> "2 gezek basyň - Kömekçi, uzak basyň - Okamak"
                },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ModernSideIndicators(
    isReadingMode: Boolean,
    isMicActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val readingPulse by infiniteTransition.animateFloat(
        initialValue = if (isReadingMode) 0.8f else 1.0f,
        targetValue = if (isReadingMode) 1.2f else 1.0f,
        animationSpec = if (isReadingMode) {
            infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        }
    )
    
    val micPulse by infiniteTransition.animateFloat(
        initialValue = if (isMicActive) 0.8f else 1.0f,
        targetValue = if (isMicActive) 1.2f else 1.0f,
        animationSpec = if (isMicActive) {
            infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        }
    )
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Reading mode indicator
        Surface(
            modifier = Modifier
                .size(64.dp)
                .scale(if (isReadingMode) readingPulse else 1f)
                .alpha(if (isReadingMode) 1f else 0.4f),
            shape = CircleShape,
            color = if (isReadingMode) {
                Color(0xFF4CAF50).copy(alpha = 0.9f)
            } else {
                Color.White.copy(alpha = 0.3f)
            },
            shadowElevation = if (isReadingMode) 12.dp else 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "Okamak rejimi",
                    tint = if (isReadingMode) Color.White else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // Mic indicator  
        Surface(
            modifier = Modifier
                .size(64.dp)
                .scale(if (isMicActive) micPulse else 1f)
                .alpha(if (isMicActive) 1f else 0.4f),
            shape = CircleShape,
            color = if (isMicActive) {
                Color(0xFF2196F3).copy(alpha = 0.9f)
            } else {
                Color.White.copy(alpha = 0.3f)
            },
            shadowElevation = if (isMicActive) 12.dp else 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mikrofon",
                    tint = if (isMicActive) Color.White else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}



@Composable
fun ReadingModeCamera(
    onImageCaptured: (Bitmap) -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)

        // Capture image once when reading mode is activated
        val outputOptions = ImageCapture.OutputFileOptions.Builder(createTempFile(context.toString())).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: return
                    val bitmap = BitmapFactory.decodeFile(savedUri.path)
                    onImageCaptured(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle error
                }
            }
        )
    }

    AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
}

