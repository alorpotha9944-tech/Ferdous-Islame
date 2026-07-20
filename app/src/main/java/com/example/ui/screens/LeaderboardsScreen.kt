package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth
import com.example.data.MissionEntity

// Mock Leaderboard records for high immersion
data class MockRacer(val rank: Int, val name: String, val car: String, val time: String, val isSelf: Boolean = false)

val MockRecordsTrack1 = listOf(
    MockRacer(1, "Apex Phantom", "Apex Concept", "00:48.24"),
    MockRacer(2, "Shadow GT", "Desert Rogue V8", "00:52.12"),
    MockRacer(3, "Neon Midnight", "Neon Midnight GT", "00:55.40"),
    MockRacer(4, "Driver (You)", "Retro Comet", "--:--.--", isSelf = true) // This will pull highscore dynamically!
)

@Composable
fun LeaderboardsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile = viewModel.playerProfile.collectAsState().value ?: return
    val missions = viewModel.missions.collectAsState().value

    // Toggle Tab: 0 = Missions, 1 = Lap Times Leaderboards
    var selectedTab by remember { mutableStateOf(0) }

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
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    text = "ACCOMPLISHMENTS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                // Coins display
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A28)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${profile.coins}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Tab Controls
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1B192E),
                contentColor = Color(0xFFFF007F),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFFF007F)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; GameAudioSynth.playCoin() },
                    text = { Text("MISSIONS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (selectedTab == 0) Color.White else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; GameAudioSynth.playCoin() },
                    text = { Text("LAP RECORDS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (selectedTab == 1) Color.White else Color.Gray) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Viewport Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (selectedTab == 0) {
                    // MISSIONS VIEW
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(missions) { mission ->
                            MissionItemCard(
                                mission = mission,
                                onClaim = { viewModel.claimMissionReward(mission.missionId) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                } else {
                    // HIGH SCORES LEADERBOARD VIEW
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Personal Record Header Card
                        item {
                            Text(
                                text = "PERSONAL BEST LAP TIMES",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                        }

                        item {
                            PersonalRecordRow(
                                trackName = "City Center Circuit",
                                timeMs = profile.bestTimeTrack1,
                                colorAccent = Color(0xFF00FFC2)
                            )
                        }

                        item {
                            PersonalRecordRow(
                                trackName = "Desert Highway",
                                timeMs = profile.bestTimeTrack2,
                                colorAccent = Color(0xFFFFB300)
                            )
                        }

                        item {
                            PersonalRecordRow(
                                trackName = "Neon Midnight",
                                timeMs = profile.bestTimeTrack3,
                                colorAccent = Color(0xFFFF007F)
                            )
                        }

                        // Mock Leaderboards Group
                        item {
                            Text(
                                text = "GLOBAL CHAMPIONSHIP RANKINGS",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                        // Render Mock Rankings list (City Center example)
                        items(MockRecordsTrack1) { racer ->
                            val displayTime = if (racer.isSelf) {
                                if (profile.bestTimeTrack1 > 0) {
                                    val sec = (profile.bestTimeTrack1 / 1000) % 60
                                    val min = (profile.bestTimeTrack1 / 60000)
                                    val ms = (profile.bestTimeTrack1 % 1000) / 10
                                    String.format("%02d:%02d.%02d", min, sec, ms)
                                } else "--:--.--"
                            } else racer.time

                            GlobalRacerRow(racer = racer, timeString = displayTime)
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MissionItemCard(
    mission: MissionEntity,
    onClaim: () -> Unit
) {
    val statusColor = if (mission.isRewardClaimed) Color.Gray else if (mission.isCompleted) Color(0xFF00FFC2) else Color(0xFFFF007F)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mission_item_${mission.missionId}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mission.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mission.description,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Progress Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(mission.progress)
                                .background(statusColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mission.isCompleted) "COMPLETED" else "${(mission.progress * 100).toInt()}%",
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action section (Reward claim button or claim confirmation text)
            Column(horizontalAlignment = Alignment.End) {
                if (mission.isRewardClaimed) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.2f)), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "CLAIMED",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (mission.isCompleted) {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CLAIM", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${mission.rewardCoins}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalRecordRow(trackName: String, timeMs: Long, colorAccent: Color) {
    val formattedTime = if (timeMs > 0) {
        val sec = (timeMs / 1000) % 60
        val min = (timeMs / 60000)
        val ms = (timeMs % 1000) / 10
        String.format("%02d:%02d.%02d", min, sec, ms)
    } else "--:--.--"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colorAccent)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = trackName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = formattedTime,
                color = if (timeMs > 0) Color.White else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun GlobalRacerRow(racer: MockRacer, timeString: String) {
    val bg = if (racer.isSelf) Color(0xFFFF007F).copy(0.08f) else Color.Transparent
    val borderCol = if (racer.isSelf) Color(0xFFFF007F) else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131122)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(8.dp)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderCol), width = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank Number
                Text(
                    text = "#${racer.rank}",
                    color = when (racer.rank) {
                        1 -> Color(0xFFFFD54F)
                        2 -> Color(0xFFB0BEC5)
                        3 -> Color(0xFFFF8F00)
                        else -> Color.Gray
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(32.dp)
                )

                Column {
                    Text(
                        text = racer.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = racer.car,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }

            Text(
                text = timeString,
                color = if (racer.isSelf && timeString == "--:--.--") Color.Gray else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
