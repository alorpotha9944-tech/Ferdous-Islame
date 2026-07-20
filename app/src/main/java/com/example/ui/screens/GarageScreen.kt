package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth
import com.example.data.UnlockedCar

// Define car configs containing prices and specs for display
data class CarDisplaySpec(
    val id: String,
    val name: String,
    val price: Int,
    val baseSpeed: Int,
    val baseAccel: Int,
    val description: String,
    val hexColor: Color
)

val CarSpecsList = listOf(
    CarDisplaySpec("car_comet", "Retro Comet", 0, 190, 70, "The balanced legendary tuner hatchback. Excellent handling.", Color(0xFF00FFC2)),
    CarDisplaySpec("car_neon_gt", "Neon Midnight GT", 4000, 220, 85, "Ultra low-gravity chassis for deep drifts on rainy neon highways.", Color(0xFFFF007F)),
    CarDisplaySpec("car_desert_rogue", "Desert Rogue V8", 9000, 240, 100, "V8 supercharged engine. High chassis stability for dune leaps.", Color(0xFF00E5FF)),
    CarDisplaySpec("car_apex_concept", "Apex Concept", 20000, 270, 120, "Ultimate experimental carbon hypercar. Max top velocity.", Color(0xFFFFB300))
)

@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile = viewModel.playerProfile.collectAsState().value ?: return
    val unlockedCars = viewModel.unlockedCars.collectAsState().value

    // Currently focused car in garage display
    var selectedCarIdInList by remember { mutableStateOf(profile.selectedCarId) }
    val focusedSpec = CarSpecsList.find { it.id == selectedCarIdInList } ?: CarSpecsList[0]
    val focusedEntity = unlockedCars.find { it.carId == selectedCarIdInList }

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
                    text = "THE GARAGE",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                // Gold Coins display
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

            // 1. Car Selector Horizontal Swipe Rail
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(CarSpecsList) { spec ->
                    val isUnlocked = unlockedCars.any { it.carId == spec.id && it.isUnlocked }
                    val isActive = profile.selectedCarId == spec.id
                    val isFocused = selectedCarIdInList == spec.id

                    CarSelectorCard(
                        spec = spec,
                        isUnlocked = isUnlocked,
                        isActive = isActive,
                        isFocused = isFocused,
                        onClick = {
                            selectedCarIdInList = spec.id
                            GameAudioSynth.playCoin()
                        }
                    )
                }
            }

            // 2. Focused Car Specs details & Upgrade Panels
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Car Details Card
                item {
                    CarDetailsShowcase(spec = focusedSpec, isUnlocked = focusedEntity != null)
                }

                // Unlock or Equip Actions
                item {
                    if (focusedEntity != null) {
                        if (profile.selectedCarId != selectedCarIdInList) {
                            Button(
                                onClick = { viewModel.selectCar(selectedCarIdInList) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("equip_car_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SELECT RACER", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2E24)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ACTIVE RACER EQUIPPED",
                                    color = Color(0xFF00FFC2),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    } else {
                        // Locked Buy Option
                        Button(
                            onClick = {
                                viewModel.buyCar(focusedSpec.id, focusedSpec.price)
                            },
                            enabled = profile.coins >= focusedSpec.price,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("buy_car_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BUY RACER FOR ${focusedSpec.price} COINS",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Tuning upgrade meters
                if (focusedEntity != null) {
                    item {
                        Text(
                            text = "PERFORMANCE TUNING",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        UpgradeTuningRow(
                            label = "ENGINE (Top Velocity)",
                            level = focusedEntity.engineLevel,
                            icon = Icons.Default.Speed,
                            price = focusedEntity.engineLevel * 800,
                            coinsAvailable = profile.coins,
                            onUpgrade = { viewModel.upgradeCarPart(focusedEntity.carId, "engine") }
                        )
                    }

                    item {
                        UpgradeTuningRow(
                            label = "TURBO (Acceleration Boost)",
                            level = focusedEntity.turboLevel,
                            icon = Icons.Default.OfflineBolt,
                            price = focusedEntity.turboLevel * 800,
                            coinsAvailable = profile.coins,
                            onUpgrade = { viewModel.upgradeCarPart(focusedEntity.carId, "turbo") }
                        )
                    }

                    item {
                        UpgradeTuningRow(
                            label = "TIRE GRIP (Slip Control)",
                            level = focusedEntity.tiresLevel,
                            icon = Icons.Default.Build,
                            price = focusedEntity.tiresLevel * 800,
                            coinsAvailable = profile.coins,
                            onUpgrade = { viewModel.upgradeCarPart(focusedEntity.carId, "tires") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CarSelectorCard(
    spec: CarDisplaySpec,
    isUnlocked: Boolean,
    isActive: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isFocused) Color(0xFF00FFC2) else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
            .testTag("car_card_${spec.id}"),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor), width = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Silhouette Avatar
            Box(
                modifier = Modifier
                    .size(100.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(spec.hexColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                // Vector decorative car icon
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = spec.hexColor,
                    modifier = Modifier.size(44.dp)
                )

                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = spec.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isActive) {
                Text("EQUIPPED", color = Color(0xFF00FFC2), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            } else if (!isUnlocked) {
                Text("${spec.price} COINS", color = Color(0xFFFFD54F), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            } else {
                Text("UNLOCKED", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun CarDetailsShowcase(spec: CarDisplaySpec, isUnlocked: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = spec.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(spec.hexColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = spec.description,
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Spec speed stat bar
            StatBarIndicator(label = "BASE SPEED", statValue = spec.baseSpeed, maxValue = 300, color = Color(0xFF00FFC2))
            Spacer(modifier = Modifier.height(10.dp))
            StatBarIndicator(label = "BASE ACCELERATION", statValue = spec.baseAccel, maxValue = 150, color = Color(0xFFFF007F))
        }
    }
}

@Composable
fun StatBarIndicator(label: String, statValue: Int, maxValue: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("$statValue / $maxValue", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(statValue.toFloat() / maxValue.toFloat())
                    .background(color)
            )
        }
    }
}

@Composable
fun UpgradeTuningRow(
    label: String,
    level: Int,
    icon: ImageVector,
    price: Int,
    coinsAvailable: Int,
    onUpgrade: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B192E)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    // Upgrade dots 1-5
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..5) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (i <= level) Color(0xFF00FFC2) else Color.Gray.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            if (level < 5) {
                Button(
                    onClick = {
                        onUpgrade()
                        GameAudioSynth.playLevelUp()
                    },
                    enabled = coinsAvailable >= price,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$price", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2E24)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "MAX",
                        color = Color(0xFF00FFC2),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
