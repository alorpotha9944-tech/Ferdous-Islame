package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile = viewModel.playerProfile.collectAsState().value ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070611), Color(0xFF141324))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "SETTINGS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.width(44.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Fields List
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 1. Controls Preferences Selection Group
                Text(
                    text = "STEERING CONTROL INTERFACE",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateControlPreferences(0)
                                    GameAudioSynth.playCoin()
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("D-PAD ARROWS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                Text("Left and Right tap buttons on screen.", color = Color.Gray, fontSize = 11.sp)
                            }
                            RadioButton(
                                selected = profile.controlType == 0,
                                onClick = {
                                    viewModel.updateControlPreferences(0)
                                    GameAudioSynth.playCoin()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF007F))
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(0.15f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateControlPreferences(1)
                                    GameAudioSynth.playCoin()
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SPINNING WHEEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                Text("Drag and spin virtual steering wheel.", color = Color.Gray, fontSize = 11.sp)
                            }
                            RadioButton(
                                selected = profile.controlType == 1,
                                onClick = {
                                    viewModel.updateControlPreferences(1)
                                    GameAudioSynth.playCoin()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF007F))
                            )
                        }
                    }
                }

                // 2. Audio settings
                Text(
                    text = "AUDIO PREFERENCES",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("SOUND EFFECTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            }
                            Switch(
                                checked = profile.soundEffectsEnabled,
                                onCheckedChange = {
                                    viewModel.toggleSFX(it)
                                    GameAudioSynth.playCoin()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFC2), checkedTrackColor = Color(0xFF00FFC2).copy(0.3f))
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(0.15f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("SYNTH MUSIC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            }
                            Switch(
                                checked = profile.musicEnabled,
                                onCheckedChange = {
                                    viewModel.toggleMusic(it)
                                    GameAudioSynth.playCoin()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFC2), checkedTrackColor = Color(0xFF00FFC2).copy(0.3f))
                            )
                        }
                    }
                }

                // 3. Simple guidelines Card
                Text(
                    text = "PILOT GUIDELINES",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "• DRIFTING: Hold SLIDE brake pedal while steering hard at high speed around turns.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• NITRO CHARGE: Evading police or collecting canisters refills nitro energy. Hit the BOLT button to blast ahead!",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Version Indicator footer
            Text(
                text = "APEX RACER V1.0.0 NATIVE BUILD",
                color = Color.Gray.copy(0.45f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
            )
        }
    }
}
