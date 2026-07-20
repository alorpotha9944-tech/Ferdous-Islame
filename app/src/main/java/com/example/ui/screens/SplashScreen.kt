package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onLoadingFinished: () -> Unit) {
    var loadingStepText by remember { mutableStateOf("SYNCHRONIZING PERSPECTIVE GRAPHICS...") }

    // Rotate bolt icon infinitely
    val infiniteTransition = rememberInfiniteTransition()
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(Unit) {
        GameAudioSynth.playLevelUp()
        delay(900)
        loadingStepText = "TUNE ENGINE VALVES (CYLINDERS 8)..."
        delay(800)
        loadingStepText = "PRESSURIZING NITRO CHARGERS..."
        delay(900)
        onLoadingFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF04030A), Color(0xFF141324))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spinning electric loader
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FFC2).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = Color(0xFF00FFC2),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "APEX RACER",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            CircularProgressIndicator(
                color = Color(0xFFFF007F),
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = loadingStepText,
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
