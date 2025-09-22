package com.github.olga_yakovleva.rhvoice.compose.blind_navigator

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainPage(navController: NavHostController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isHelpVisible by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val helpScale by animateFloatAsState(
        targetValue = if (isHelpVisible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.8f)
    )

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1e3c72),
                        Color(0xFF2a5298),
                        Color(0xFF667eea)
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Section
        item {
            Column(
                modifier = Modifier.padding(top = 40.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RHVoice Kömekçi",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kör adamlar üçin nawigasıýa kömekçi",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Main Feature Card - Blind Mode
        item {
            ElevatedCard(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("blindMode")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .scale(helpScale),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4facfe),
                                    Color(0xFF00f2fe)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessibilityNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Körmekci Rejimi",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Başlamak üçin basyň",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Feature Cards Row
        item {
            Text(
                text = "Aýratynlyklar",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                // Voice Recognition Card
                item {
                    FeatureCard(
                        icon = Icons.Default.VolumeUp,
                        title = "Ses Tanyş",
                        description = "Ses bilen dolandyrmak",
                        color = Color(0xFFff6b6b)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tts?.speak(
                            "Ses tanymak funksiýasy. Bu funksiýa arkaly ses buýruklary berip programmany dolandyryp bilersiňiz.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                }

                // Text Reading Card
                item {
                    FeatureCard(
                        icon = Icons.Default.Book,
                        title = "Tekst Okamak",
                        description = "Teksti sesli okamak",
                        color = Color(0xFF4ecdc4)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tts?.speak(
                            "Tekst okamak funksiýasy. Bu funksiýa arkaly tekstleri sesli okap bilersiňiz.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                }

                // Tutorial Card
                item {
                    FeatureCard(
                        icon = Icons.Default.PlayArrow,
                        title = "Öwrediş",
                        description = "Ulanyş görkezmeleri",
                        color = Color(0xFF45b7d1)
                    ) {
                        isHelpVisible = !isHelpVisible
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
        }

        // Help Section
        item {
            AnimatedVisibility(
                visible = isHelpVisible,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ulanyş Görkezmeleri",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF2c3e50),
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        isHelpVisible = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                shape = CircleShape,
                                color = Color(0xFF4facfe).copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Ýap",
                                    tint = Color(0xFF4facfe),
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HelpItem(
                            number = "1",
                            title = "Başlatmak",
                            description = "Iki gezek basyp kömekçi rejime giriň"
                        )

                        HelpItem(
                            number = "2", 
                            title = "Okamak rejimi",
                            description = "Uzak basyp tekst okamak rejimini açyň"
                        )

                        HelpItem(
                            number = "3",
                            title = "Ses kömegi",
                            description = "Gulaklyklaryňyz bilen dolandyryň"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ElevatedCard(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com")
                                )
                                context.startActivity(intent)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4facfe)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📺 YouTube Wideosyny Seret",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Footer spacing
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .size(width = 140.dp, height = 160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF2c3e50),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF7f8c8d),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HelpItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = Color(0xFF4facfe)
        ) {
            Text(
                text = number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.size(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF2c3e50)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFF7f8c8d)
            )
        }
    }
}