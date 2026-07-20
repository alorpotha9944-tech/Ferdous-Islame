package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth
import kotlin.math.max

@Composable
fun DashboardScreen(viewModel: GameViewModel) {
    val profile = viewModel.playerProfile.collectAsState().value ?: return

    // Background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0915), Color(0xFF141324))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile Status Bar Header
            item {
                Spacer(modifier = Modifier.height(20.dp))
                DashboardHeader(profile = profile, viewModel = viewModel)
            }

            // 2. Daily Rewards banner (Cooling Timestamp logic)
            item {
                DailyRewardsCard(profile = profile, viewModel = viewModel)
            }

            // 3. Grid of Main Game Modules
            item {
                Text(
                    text = "RACE PORTAL",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModuleBox(
                        title = "DRIVE",
                        subtitle = "Select track & race",
                        icon = Icons.Default.DirectionsCar,
                        colorAccent = Color(0xFF00FFC2),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.navigateTo("track_select")
                            GameAudioSynth.playCoin()
                        }
                    )
                    ModuleBox(
                        title = "GARAGE",
                        subtitle = "Tune & buy cars",
                        icon = Icons.Default.Build,
                        colorAccent = Color(0xFFFF007F),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.navigateTo("garage")
                            GameAudioSynth.playCoin()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModuleBox(
                        title = "MISSIONS",
                        subtitle = "Claim bonuses",
                        icon = Icons.Default.Assignment,
                        colorAccent = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.navigateTo("leaderboard") // unified leaderboard & missions list
                            GameAudioSynth.playCoin()
                        }
                    )
                    ModuleBox(
                        title = "SETTINGS",
                        subtitle = "Audio & Controls",
                        icon = Icons.Default.Settings,
                        colorAccent = Color(0xFFFBC02D),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.navigateTo("settings")
                            GameAudioSynth.playCoin()
                        }
                    )
                }
            }

            // 4. Show Current Active Car Snapshot
            item {
                ActiveCarMiniBanner(profile = profile, viewModel = viewModel)
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun DashboardHeader(profile: com.example.data.PlayerProfile, viewModel: GameViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_header")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left profile circle and username
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF007F).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFF007F), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.username,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "LEVEL ${profile.level} PILOT",
                            color = Color(0xFF00FFC2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Coins Card indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141324)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${profile.coins}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // XP level progress slider
            val currentXP = profile.xp
            val neededXP = viewModel.xpNeededForNextLevel(profile.level)
            val progressFactor = currentXP.toFloat() / neededXP.toFloat()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("XP PROGRESS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("$currentXP / $neededXP XP", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFactor)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFF007F), Color(0xFF00FFC2)))
                        )
                )
            }
        }
    }
}

@Composable
fun DailyRewardsCard(profile: com.example.data.PlayerProfile, viewModel: GameViewModel) {
    val now = System.currentTimeMillis()
    val coolingTimeMs = 24 * 60 * 60 * 1000L
    val isAvailable = now - profile.lastDailyRewardClaimed >= coolingTimeMs

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) Color(0xFF0F2C24) else Color(0xFF1A1A26)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_rewards_card")
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isAvailable) Color(0xFF00FFC2).copy(0.12f) else Color.Gray.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = if (isAvailable) Color(0xFF00FFC2) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "DAILY REWARD",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isAvailable) "Claim 250 coins immediately!" else "Next claim available tomorrow",
                        color = if (isAvailable) Color(0xFF00FFC2) else Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            if (isAvailable) {
                Button(
                    onClick = { viewModel.claimDailyReward() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CLAIM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                // Calculate estimated time remaining
                val diffMs = coolingTimeMs - (now - profile.lastDailyRewardClaimed)
                val hours = (diffMs / (60 * 60 * 1000L))
                val min = (diffMs / (60 * 1000L)) % 60

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.25f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = String.format("%02dh %02dm", max(0, hours), max(0, min)),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleBox(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colorAccent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = colorAccent, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ActiveCarMiniBanner(profile: com.example.data.PlayerProfile, viewModel: GameViewModel) {
    val cars = viewModel.unlockedCars.collectAsState().value
    val activeCar = cars.find { it.carId == profile.selectedCarId } ?: return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A28)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.navigateTo("garage")
                GameAudioSynth.playCoin()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ACTIVE RACER", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activeCar.carName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                Row {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f)), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "SPEED Lvl ${activeCar.engineLevel}",
                            color = Color(0xFF00FFC2),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f)), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "BOOST Lvl ${activeCar.turboLevel}",
                            color = Color(0xFFFF007F),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal decorative neon line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF007F), Color(0xFF00FFC2), Color(0xFF00E5FF))
                        )
                    )
            )
        }
    }
}
