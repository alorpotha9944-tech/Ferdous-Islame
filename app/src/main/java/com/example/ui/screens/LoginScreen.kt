package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: GameViewModel,
    onEnterGame: () -> Unit
) {
    val profile = viewModel.playerProfile.collectAsState().value ?: return
    val coroutineScope = rememberCoroutineScope()

    var driverName by remember { mutableStateOf(profile.username) }
    var selectedAvatarIdx by remember { mutableStateOf(profile.avatarId) }

    // Floating speed grid background lines
    val infiniteTransition = rememberInfiniteTransition()
    val scrollOffset = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF04030A), Color(0xFF100E21))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic futuristic horizon speed trails drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2f

            // Draw diagonal grid perspective lines extending outwards
            for (i in -4..4) {
                val startX = w / 2f
                val endX = w / 2f + (i * w * 0.3f)
                drawLine(
                    color = Color(0xFFFF007F).copy(alpha = 0.15f),
                    start = Offset(startX, cy),
                    end = Offset(endX, h),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Draw horizontal racing scrolling bands
            val bandOffset = scrollOffset.value % 250f
            for (j in 0..6) {
                val lineY = cy + (j * 110f) + bandOffset
                if (lineY < h) {
                    val opacity = (lineY - cy) / (h - cy) * 0.3f
                    drawLine(
                        color = Color(0xFF00FFC2).copy(alpha = opacity),
                        start = Offset(0f, lineY),
                        end = Offset(w, lineY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neon Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF007F).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color(0xFFFF007F),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "APEX RACER",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )

            Text(
                text = "NATIVE KOTLIN PERSPECTIVE RACER",
                color = Color(0xFF00FFC2),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Profile Identity setup block
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_identity_card")
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PILOT IDENTIFICATION",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // TextField Username Customizer
                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FFC2),
                            unfocusedBorderColor = Color.Gray.copy(0.3f),
                            focusedLabelColor = Color(0xFF00FFC2),
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_name_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Launch button
                    Button(
                        onClick = {
                            if (driverName.isNotBlank()) {
                                coroutineScope.launch {
                                    viewModel.updatePlayerProfile(
                                        profile.copy(
                                            username = driverName,
                                            avatarId = selectedAvatarIdx
                                        )
                                    )
                                    GameAudioSynth.playLevelUp()
                                    onEnterGame()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("launch_engine_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("LAUNCH ENGINE", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
