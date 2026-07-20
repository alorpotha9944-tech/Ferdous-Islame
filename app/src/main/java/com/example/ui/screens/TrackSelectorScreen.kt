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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth
import com.example.game.*

// Local representation of tracks
data class TrackDisplayInfo(
    val id: String,
    val name: String,
    val length: String,
    val complexity: String,
    val weather: Weather,
    val timeOfDay: TimeOfDay,
    val desc: String,
    val accentColor: Color,
    val config: TrackConfig
)

val TracksList = listOf(
    TrackDisplayInfo(
        id = "track_city",
        name = "City Center Circuit",
        length = "3.2 KM",
        complexity = "EASY",
        weather = Weather.CLEAR,
        timeOfDay = TimeOfDay.SUNSET,
        desc = "Race through gridiron neon skyscrapers during sunset. Smooth straights and light traffic.",
        accentColor = Color(0xFF00FFC2),
        config = TrackConfig("track_city", "City Center", totalSegments = 1200, difficultyMultiplier = 1.0f, initialWeather = Weather.CLEAR, initialTimeOfDay = TimeOfDay.SUNSET)
    ),
    TrackDisplayInfo(
        id = "track_desert",
        name = "Desert Highway",
        length = "4.5 KM",
        complexity = "MEDIUM",
        weather = Weather.FOG,
        timeOfDay = TimeOfDay.DAY,
        desc = "Massive straightaways with extreme desert dune elevation jumps. Watch out for fog banks!",
        accentColor = Color(0xFFFFB300),
        config = TrackConfig("track_desert", "Desert Highway", totalSegments = 1600, difficultyMultiplier = 1.2f, initialWeather = Weather.FOG, initialTimeOfDay = TimeOfDay.DAY)
    ),
    TrackDisplayInfo(
        id = "track_midnight",
        name = "Neon Midnight S-Curves",
        length = "5.4 KM",
        complexity = "HARD",
        weather = Weather.RAIN,
        timeOfDay = TimeOfDay.NIGHT,
        desc = "Rain-slicked asphalt under neon signs. Advanced complex s-curves. Ideal for drifting.",
        accentColor = Color(0xFFFF007F),
        config = TrackConfig("track_midnight", "Neon Midnight", totalSegments = 2000, difficultyMultiplier = 1.5f, initialWeather = Weather.RAIN, initialTimeOfDay = TimeOfDay.NIGHT)
    )
)

@Composable
fun TrackSelectorScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var selectedTrackId by remember { mutableStateOf(TracksList[0].id) }
    val focusedTrack = TracksList.find { it.id == selectedTrackId } ?: TracksList[0]

    // Local mode selector: 0 = Arcade Race, 1 = Free Drive, 2 = Police Chase
    var selectedMode by remember { mutableStateOf(0) }

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
                    text = "SELECT EVENT",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.width(44.dp)) // symmetry spacer
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Title section
                item {
                    Text(
                        text = "1. SELECT TRACK",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                // 2. Track choices
                items(TracksList) { track ->
                    TrackRowCard(
                        track = track,
                        isSelected = track.id == selectedTrackId,
                        onClick = {
                            selectedTrackId = track.id
                            GameAudioSynth.playCoin()
                        }
                    )
                }

                // 3. Mode title
                item {
                    Text(
                        text = "2. SELECT RACING MODE",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 4. Mode Boxes
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModeCard(
                            modeIndex = 0,
                            title = "ARCADE CUP RACE",
                            desc = "Race 3 AI competitors. High coins prize on victory.",
                            icon = Icons.Default.EmojiEvents,
                            iconColor = Color(0xFFFFD54F),
                            isSelected = selectedMode == 0,
                            onClick = { selectedMode = 0; GameAudioSynth.playCoin() }
                        )

                        ModeCard(
                            modeIndex = 1,
                            title = "FREE DRIVE",
                            desc = "Explore curves at your own pace. Practice drift lines.",
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFF00FFC2),
                            isSelected = selectedMode == 1,
                            onClick = { selectedMode = 1; GameAudioSynth.playCoin() }
                        )

                        ModeCard(
                            modeIndex = 2,
                            title = "POLICE CHASE ESCAPE",
                            desc = "Survive flashing police sirens. Evade to gain major XP.",
                            icon = Icons.Default.Campaign,
                            iconColor = Color(0xFFFF007F),
                            isSelected = selectedMode == 2,
                            onClick = { selectedMode = 2; GameAudioSynth.playCoin() }
                        )
                    }
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 5. Massive Launch button
            Button(
                onClick = {
                    viewModel.selectedTrackConfig = focusedTrack.config
                    viewModel.isPoliceChaseSelected = selectedMode == 2
                    viewModel.navigateTo("race")
                    GameAudioSynth.playCountdownGo()
                },
                colors = ButtonDefaults.buttonColors(containerColor = focusedTrack.accentColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 12.dp)
                    .testTag("launch_race_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START DRIVE",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrackRowCard(
    track: TrackDisplayInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) track.accentColor else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("track_row_${track.id}"),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor), width = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = track.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LENGTH: ${track.length} | DEPTH: ${track.complexity}",
                        color = track.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(track.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (track.weather) {
                            Weather.RAIN -> Icons.Default.Cloud
                            Weather.FOG -> Icons.Default.Cloud
                            Weather.CLEAR -> Icons.Default.WbSunny
                        },
                        contentDescription = null,
                        tint = track.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = track.desc,
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ModeCard(
    modeIndex: Int,
    title: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF00FFC2) else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mode_card_$modeIndex"),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor), width = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = desc,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
