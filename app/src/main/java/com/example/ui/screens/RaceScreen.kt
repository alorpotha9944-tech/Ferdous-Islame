package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioSynth
import com.example.game.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun RaceScreen(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit
) {
    val config = viewModel.selectedTrackConfig ?: return
    val activeCar = viewModel.playerProfile.collectAsState().value?.selectedCarId ?: "car_comet"
    val profile = viewModel.playerProfile.collectAsState().value ?: return
    val unlockedCarsList = viewModel.unlockedCars.collectAsState().value
    val selectedCarEntity = unlockedCarsList.find { it.carId == activeCar } ?: return

    // Instantiate and manage GameEngine
    val engine = remember {
        GameEngine(
            trackConfig = config,
            isPoliceChaseMode = viewModel.isPoliceChaseSelected,
            activeCar = selectedCarEntity,
            playerProfile = profile,
            onRaceFinished = { coinsWon, xpEarned, timeMs, isVic, missions ->
                viewModel.finalizeRace(coinsWon, xpEarned, timeMs, isVic, missions)
            }
        )
    }

    // Connect engine ref to ViewModel for access
    LaunchedEffect(engine) {
        viewModel.activeEngine = engine
        engine.startRace()
    }

    // Main Game Engine loop (60 FPS tick)
    LaunchedEffect(engine.isRunning, engine.isPaused, engine.trackCompleted, engine.isCrashEnding) {
        var lastTime = System.nanoTime()
        while (engine.isRunning && !engine.isPaused && !engine.trackCompleted && !engine.isCrashEnding) {
            val currentTime = System.nanoTime()
            val dt = (currentTime - lastTime) / 1_000_000_000f
            lastTime = currentTime

            // Limit dt to avoid massive physics jumps
            val clampedDt = min(0.033f, dt)

            engine.tick(clampedDt)
            // Procedural RPM Sound simulation based on speed
            GameAudioSynth.playEngineRev(engine.currentSpeedKmh)

            // Sirens
            if (viewModel.isPoliceChaseSelected && engine.isSirenFlashing && engine.policeDistanceWarning > 0.5f) {
                GameAudioSynth.playSirenSwell()
            }

            delay(16) // tick around 60 FPS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        // 1. Core 3D Road Canvas
        RaceCanvas(engine = engine)

        // 2. Head-Up Display (HUD)
        GameHUD(engine = engine)

        // 3. Touch Controls Layout
        GameControls(engine = engine, controlType = profile.controlType)

        // 4. Pre-Race Countdown Overlay
        if (engine.countdownTime > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                val scale = remember { Animatable(0f) }
                LaunchedEffect(engine.countdownTime) {
                    GameAudioSynth.playCountdownBeep()
                    scale.snapTo(0f)
                    scale.animateTo(
                        targetValue = 1.2f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                }

                Text(
                    text = "${engine.countdownTime}",
                    color = Color(0xFF00FFC2),
                    fontSize = 110.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // 5. Post-Race Finish Card Overlay
        if (engine.trackCompleted || engine.isCrashEnding) {
            PostRaceDialog(
                engine = engine,
                onRestart = {
                    engine.resetRace()
                    engine.startRace()
                },
                onExit = onBackToMenu
            )
        }

        // 6. Pause Dialog Overlay
        if (engine.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151421)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .width(320.dp)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GAME PAUSED",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { engine.togglePause() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onBackToMenu,
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color.Gray)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("EXIT RACE", color = Color.White)
                        }
                    }
                }
            }
        }

        // Top Header Control (Pause and Title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { engine.togglePause() },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = config.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun RaceCanvas(engine: GameEngine) {
    val player = engine.player
    val segments = engine.segments
    val skyColorStart = engine.skyColorStart
    val skyColorEnd = engine.skyColorEnd
    val weather = engine.weather

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("race_canvas")
    ) {
        val width = size.width
        val height = size.height

        val centerX = width / 2f
        val centerY = height / 2.2f // horizon height

        // 1. Draw Sky (Time-of-day shifting linear gradient)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(skyColorStart, skyColorEnd),
                start = Offset(0f, 0f),
                end = Offset(0f, centerY)
            ),
            size = Size(width, centerY)
        )

        // Draw parallax mountains or cyber grid elements on horizon based on curvature
        val horizonOffset = -(player.z * 0.002f) % width
        drawParallaxHorizon(centerX, centerY, width, horizonOffset, engine.trackConfig.id)

        // 2. Draw 3D Road Segments (Painter's algorithm: Back-to-front projection)
        val playerSegIndex = (player.z / engine.trackConfig.segmentLength).toInt()
        val cameraDepth = 0.85f // perspective focal length factor
        val roadWidth = engine.trackConfig.roadWidth

        val drawDistance = 140 // visible segments
        var maxAnypointY = height // keeps track of clip occlusion

        var xCumulativeCurve = 0f
        var dxCumulative = 0f

        // Loop from back to front of visible segments
        for (i in (playerSegIndex + drawDistance) downTo playerSegIndex) {
            val segment = segments[i % segments.size]

            // Calculate camera offset
            val isLoopWrap = i >= segments.size
            val wrapOffset = if (isLoopWrap) segments.size * engine.trackConfig.segmentLength else 0

            val cameraZ = player.z
            val cameraX = player.x * roadWidth
            val cameraY = 1500f // camera hover height

            // Projection math
            val p1z = segment.p1.z + wrapOffset - cameraZ
            if (p1z <= 0) continue // Behind camera

            val scale1 = cameraDepth / p1z
            val screenX1 = centerX + (segment.p1.x - cameraX) * scale1
            val screenY1 = centerY - (segment.p1.y - cameraY) * scale1
            val w1 = roadWidth * scale1

            val p2z = segment.p2.z + wrapOffset - cameraZ
            if (p2z <= 0) continue

            val scale2 = cameraDepth / p2z
            val screenX2 = centerX + (segment.p2.x - cameraX) * scale2
            val screenY2 = centerY - (segment.p2.y - cameraY) * scale2
            val w2 = roadWidth * scale2

            // Vertical occlusion culling
            if (screenY1 >= maxAnypointY) continue
            // Occult horizon culling limit if hill goes down
            if (screenY1 < centerY) continue

            // Render Segment road, rumbles, grass
            drawSegmentRow(
                screenX1 = screenX1, screenY1 = screenY1, w1 = w1,
                screenX2 = screenX2, screenY2 = screenY2, w2 = w2,
                width = width,
                colorRoad = segment.colorRoad,
                colorGrass = segment.colorGrass,
                colorRumble = segment.colorRumble,
                colorStrip = segment.colorStrip
            )

            // Save vertical bounds for next iteration
            segment.clip = screenY1

            // 3. Render Items & Obstacles on Segment
            for (item in segment.items) {
                if (item.isCollected) continue

                // item offset on road
                val itemX = screenX1 + (item.offset * w1 * 0.5f)
                val itemY = screenY1
                val itemScale = scale1 * 1300f // scaled proportional size

                if (itemScale > 3f) {
                    drawTrackItem(itemX, itemY, itemScale, item.type, i)
                }
            }

            // 4. Render Traffic Vehicles on this segment
            for (tc in engine.trafficCars) {
                val tcSegIndex = (tc.z / engine.trackConfig.segmentLength).toInt()
                if (tcSegIndex == segment.index) {
                    val scale = cameraDepth / (tc.z - player.z)
                    if (scale > 0) {
                        val carX = centerX + (tc.x * roadWidth - cameraX) * scale
                        val carY = centerY - (0f - cameraY) * scale
                        val carSize = scale * 1500f
                        if (carSize > 4f) {
                            drawSpriteVehicle(carX, carY, carSize, tc.getColor(), isBraking = false, hasSiren = false)
                        }
                    }
                }
            }

            // 5. Render AI Opponent Racers on this segment
            for (ai in engine.aiCars) {
                val aiSegIndex = (ai.z / engine.trackConfig.segmentLength).toInt()
                if (aiSegIndex == segment.index) {
                    val scale = cameraDepth / (ai.z - player.z)
                    if (scale > 0) {
                        val carX = centerX + (ai.x * roadWidth - cameraX) * scale
                        val carY = centerY - (0f - cameraY) * scale
                        val carSize = scale * 1500f
                        if (carSize > 4f) {
                            drawSpriteVehicle(carX, carY, carSize, ai.getColor(), isBraking = false, hasSiren = false)
                        }
                    }
                }
            }

            // 6. Render Police cars
            for (cop in engine.policeCars) {
                val copSegIndex = (cop.z / engine.trackConfig.segmentLength).toInt()
                if (copSegIndex == segment.index) {
                    val scale = cameraDepth / (cop.z - player.z)
                    if (scale > 0) {
                        val carX = centerX + (cop.x * roadWidth - cameraX) * scale
                        val carY = centerY - (0f - cameraY) * scale
                        val carSize = scale * 1600f
                        if (carSize > 4f) {
                            drawSpriteVehicle(carX, carY, carSize, Color.DarkGray, isBraking = false, hasSiren = true, sirenFlash = engine.isSirenFlashing)
                        }
                    }
                }
            }
        }

        // 7. Draw Player's own car (centered at bottom of screen with physics response)
        val playerScale = 14f
        val playerScreenX = centerX + (player.driftAngle * 3f) // moves visually left/right when drifting
        val playerScreenY = height - 140.dp.toPx()
        val playerVelocityRoll = player.steeringInput * 12f // slight visual body roll on steering

        // Render shadow below player car
        drawOval(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(playerScreenX - 85.dp.toPx(), playerScreenY + 45.dp.toPx()),
            size = Size(170.dp.toPx(), 25.dp.toPx())
        )

        // Draw Player vector chassis
        drawPlayerVehicle(
            centerX = playerScreenX,
            centerY = playerScreenY,
            scale = playerScale,
            steeringRoll = playerVelocityRoll,
            isNitroActive = player.isNitroActive,
            isBraking = player.isBraking,
            isDrifting = player.isDrifting,
            damagePercent = player.currentHealth
        )

        // 8. Draw Weather Effects overlay (Rain/Fog)
        if (weather == Weather.RAIN) {
            drawRainOverlay(width, height, player.speed)
        } else if (weather == Weather.FOG) {
            drawFogOverlay(width, height)
        }
    }
}

fun DrawScope.drawParallaxHorizon(centerX: Float, centerY: Float, width: Float, offset: Float, trackId: String) {
    // Render beautiful retro-looking mountains/buildings silhouettes
    val colorSihouette = if (trackId == "track_midnight") Color(0xFF0F0B1E) else Color(0x3B352E4D)

    val path = Path()
    path.moveTo(0f, centerY)

    // Generate 5 peaks
    val peakCount = 6
    val peakWidth = width / peakCount
    for (i in -1..peakCount + 1) {
        val startX = i * peakWidth + offset
        val midX = startX + peakWidth / 2f
        val endX = startX + peakWidth

        val heightFactor = if (trackId == "track_midnight") {
            // Skyscraper steps
            val h = 60f + (i * 243 % 90f)
            path.lineTo(startX, centerY - h)
            path.lineTo(midX, centerY - h)
            path.lineTo(midX, centerY)
            path.lineTo(endX, centerY)
        } else {
            // Hills/Mountains
            val h = 40f + (i * 123 % 70f)
            path.quadraticTo(midX, centerY - h, endX, centerY)
        }
    }
    path.close()
    drawPath(path, color = colorSihouette)
}

fun DrawScope.drawSegmentRow(
    screenX1: Float, screenY1: Float, w1: Float,
    screenX2: Float, screenY2: Float, w2: Float,
    width: Float,
    colorRoad: Color,
    colorGrass: Color,
    colorRumble: Color,
    colorStrip: Color
) {
    // 1. Draw Grass background
    // Since segment sweeps full width, draw polygons covering from segment edge to screen limits
    val grassPath = Path()
    grassPath.moveTo(0f, screenY2)
    grassPath.lineTo(width, screenY2)
    grassPath.lineTo(width, screenY1)
    grassPath.lineTo(0f, screenY1)
    grassPath.close()
    drawPath(grassPath, color = colorGrass)

    // 2. Draw Rumble curbs on road edges
    val rumbleW1 = w1 * 0.08f
    val rumbleW2 = w2 * 0.08f

    val rumbleLeftPath = Path()
    rumbleLeftPath.moveTo(screenX2 - w2 / 2f - rumbleW2, screenY2)
    rumbleLeftPath.lineTo(screenX2 - w2 / 2f, screenY2)
    rumbleLeftPath.lineTo(screenX1 - w1 / 2f, screenY1)
    rumbleLeftPath.lineTo(screenX1 - w1 / 2f - rumbleW1, screenY1)
    rumbleLeftPath.close()
    drawPath(rumbleLeftPath, color = colorRumble)

    val rumbleRightPath = Path()
    rumbleRightPath.moveTo(screenX2 + w2 / 2f, screenY2)
    rumbleRightPath.lineTo(screenX2 + w2 / 2f + rumbleW2, screenY2)
    rumbleRightPath.lineTo(screenX1 + w1 / 2f + rumbleW1, screenY1)
    rumbleRightPath.lineTo(screenX1 + w1 / 2f, screenY1)
    rumbleRightPath.close()
    drawPath(rumbleRightPath, color = colorRumble)

    // 3. Draw Road polygon itself
    val roadPath = Path()
    roadPath.moveTo(screenX2 - w2 / 2f, screenY2)
    roadPath.lineTo(screenX2 + w2 / 2f, screenY2)
    roadPath.lineTo(screenX1 + w1 / 2f, screenY1)
    roadPath.lineTo(screenX1 - w1 / 2f, screenY1)
    roadPath.close()
    drawPath(roadPath, color = colorRoad)

    // 4. Draw Center Lane dashed markings
    if (colorStrip.alpha > 0f) {
        val stripW1 = w1 * 0.02f
        val stripW2 = w2 * 0.02f

        val laneStripPath = Path()
        laneStripPath.moveTo(screenX2 - stripW2 / 2f, screenY2)
        laneStripPath.lineTo(screenX2 + stripW2 / 2f, screenY2)
        laneStripPath.lineTo(screenX1 + stripW1 / 2f, screenY1)
        laneStripPath.lineTo(screenX1 - stripW1 / 2f, screenY1)
        laneStripPath.close()
        drawPath(laneStripPath, color = colorStrip)
    }
}

fun DrawScope.drawTrackItem(x: Float, y: Float, scale: Float, type: TrackItemType, index: Int) {
    when (type) {
        TrackItemType.COIN -> {
            // Glowing golden spinning coin
            val bobbingOffset = sin(index + System.currentTimeMillis() / 150f) * (scale * 0.15f)
            val coinRadius = scale * 0.35f
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = coinRadius,
                center = Offset(x, y - scale * 0.5f + bobbingOffset)
            )
            // Coin inner ring
            drawCircle(
                color = Color(0xFFFF8F00),
                radius = coinRadius * 0.6f,
                center = Offset(x, y - scale * 0.5f + bobbingOffset),
                style = Stroke(width = scale * 0.08f)
            )
        }
        TrackItemType.NITRO -> {
            // Glowing neon blue cylinder canister
            val rectWidth = scale * 0.4f
            val rectHeight = scale * 0.7f
            val cy = y - rectHeight / 2f
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(x - rectWidth / 2f, cy),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            // Glowing energy bolt icon on cylinder
            val lightningPath = Path()
            lightningPath.moveTo(x, cy + rectHeight * 0.2f)
            lightningPath.lineTo(x - rectWidth * 0.2f, cy + rectHeight * 0.55f)
            lightningPath.lineTo(x + rectWidth * 0.1f, cy + rectHeight * 0.55f)
            lightningPath.lineTo(x - rectWidth * 0.05f, cy + rectHeight * 0.85f)
            lightningPath.lineTo(x + rectWidth * 0.25f, cy + rectHeight * 0.45f)
            lightningPath.lineTo(x - rectWidth * 0.05f, cy + rectHeight * 0.45f)
            lightningPath.close()
            drawPath(lightningPath, color = Color.White)
        }
        TrackItemType.REPAIR -> {
            // Glowing white and green cross repair kit box
            val size = scale * 0.6f
            val rx = x - size / 2f
            val ry = y - size
            drawRoundRect(
                color = Color(0xFF2ECC71),
                topLeft = Offset(rx, ry),
                size = Size(size, size),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Cross
            drawRect(Color.White, topLeft = Offset(x - size * 0.12f, ry + size * 0.2f), size = Size(size * 0.24f, size * 0.6f))
            drawRect(Color.White, topLeft = Offset(x - size * 0.3f, ry + size * 0.38f), size = Size(size * 0.6f, size * 0.24f))
        }
        TrackItemType.ROCK -> {
            // Solid dark gray geometric rock
            val path = Path()
            path.moveTo(x - scale * 0.5f, y)
            path.lineTo(x - scale * 0.3f, y - scale * 0.45f)
            path.lineTo(x + scale * 0.2f, y - scale * 0.5f)
            path.lineTo(x + scale * 0.45f, y - scale * 0.2f)
            path.lineTo(x + scale * 0.5f, y)
            path.close()
            drawPath(path, color = Color(0xFF5D6D7E))
            // Facet highlight
            val pathFac = Path()
            pathFac.moveTo(x - scale * 0.3f, y - scale * 0.45f)
            pathFac.lineTo(x + scale * 0.2f, y - scale * 0.5f)
            pathFac.lineTo(x, y)
            pathFac.close()
            drawPath(pathFac, color = Color(0xFF85929E))
        }
        TrackItemType.CONE -> {
            // Striped orange cones
            val path = Path()
            path.moveTo(x - scale * 0.35f, y)
            path.lineTo(x + scale * 0.35f, y)
            path.lineTo(x + scale * 0.1f, y - scale * 0.7f)
            path.lineTo(x - scale * 0.1f, y - scale * 0.7f)
            path.close()
            drawPath(path, color = Color(0xFFFF5722))

            // White stripes
            val stripePath = Path()
            stripePath.moveTo(x - scale * 0.18f, y - scale * 0.25f)
            stripePath.lineTo(x + scale * 0.18f, y - scale * 0.25f)
            stripePath.lineTo(x + scale * 0.13f, y - scale * 0.45f)
            stripePath.lineTo(x - scale * 0.13f, y - scale * 0.45f)
            stripePath.close()
            drawPath(stripePath, color = Color.White)
        }
        TrackItemType.NEON_SIGN -> {
            // Neon pink light-barrier post (Cyberpunk)
            val w = scale * 0.6f
            val h = scale * 0.8f
            drawRoundRect(
                color = Color(0xFFFF007F),
                topLeft = Offset(x - w / 2f, y - h),
                size = Size(w, h),
                style = Stroke(width = scale * 0.08f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            // Glow center
            drawRect(Color.White, topLeft = Offset(x - w * 0.4f, y - h * 0.6f), size = Size(w * 0.8f, scale * 0.1f))
        }
        TrackItemType.POLICE_BLOCK -> {
            // Barrier strip spiked police block
            val w = scale * 1.1f
            val h = scale * 0.2f
            drawRect(Color(0xFFE53935), topLeft = Offset(x - w / 2f, y - h), size = Size(w, h))
            // Draw hazard stripes
            for (i in 0..4) {
                val sx = x - w / 2f + (i * w * 0.2f)
                val strPath = Path()
                strPath.moveTo(sx, y)
                strPath.lineTo(sx + w * 0.08f, y)
                strPath.lineTo(sx + w * 0.15f, y - h)
                strPath.lineTo(sx + w * 0.07f, y - h)
                strPath.close()
                drawPath(strPath, color = Color(0xFFFBC02D))
            }
        }
    }
}

fun DrawScope.drawSpriteVehicle(x: Float, y: Float, scale: Float, bodyColor: Color, isBraking: Boolean, hasSiren: Boolean, sirenFlash: Boolean = false) {
    val h = scale * 0.45f
    val w = scale * 0.7f

    // Car Shadow
    drawOval(Color.Black.copy(0.45f), topLeft = Offset(x - w * 0.6f, y - h * 0.15f), size = Size(w * 1.2f, h * 0.3f))

    // Rear bumper and chassis base
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(x - w / 2f, y - h * 0.7f),
        size = Size(w, h * 0.6f),
        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
    )

    // Rear Cabin top (windshield screen)
    val cabW = w * 0.7f
    val cabH = h * 0.35f
    drawRoundRect(
        color = Color(0xFF1B2631),
        topLeft = Offset(x - cabW / 2f, y - h * 0.95f),
        size = Size(cabW, cabH),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Rear Wheels
    drawRoundRect(Color.Black, topLeft = Offset(x - w * 0.48f, y - h * 0.35f), size = Size(w * 0.18f, h * 0.35f), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
    drawRoundRect(Color.Black, topLeft = Offset(x + w * 0.3f, y - h * 0.35f), size = Size(w * 0.18f, h * 0.35f), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))

    // Taillights
    val lightW = w * 0.15f
    val lightColor = if (isBraking) Color(0xFFFF1744) else Color(0xFFD32F2F)
    drawRect(lightColor, topLeft = Offset(x - w * 0.42f, y - h * 0.6f), size = Size(lightW, h * 0.15f))
    drawRect(lightColor, topLeft = Offset(x + w * 0.27f, y - h * 0.6f), size = Size(lightW, h * 0.15f))

    // Police Sirens
    if (hasSiren) {
        val sColor = if (sirenFlash) Color(0xFF00E5FF) else Color(0xFFFF1744)
        drawCircle(
            color = sColor,
            radius = scale * 0.08f,
            center = Offset(x, y - h * 1.05f)
        )
    }
}

fun DrawScope.drawPlayerVehicle(
    centerX: Float,
    centerY: Float,
    scale: Float,
    steeringRoll: Float,
    isNitroActive: Boolean,
    isBraking: Boolean,
    isDrifting: Boolean,
    damagePercent: Float
) {
    val h = scale * 4.5f
    val w = scale * 7.5f

    // 1. Draw Nitro Thruster exhaust flame
    if (isNitroActive) {
        val flamePath = Path()
        flamePath.moveTo(centerX - scale * 1.5f, centerY + h * 0.4f)
        flamePath.quadraticTo(centerX, centerY + h * 1.8f, centerX + scale * 1.5f, centerY + h * 0.4f)
        flamePath.lineTo(centerX, centerY + h * 0.6f)
        flamePath.close()
        drawPath(
            flamePath,
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFF00FFFF), Color(0x0000FFFF)),
                center = Offset(centerX, centerY + h * 0.9f)
            )
        )
    }

    // Wrap canvas rotation to handle chassis roll / tilt on steering
    rotate(degrees = steeringRoll, pivot = Offset(centerX, centerY + h * 0.5f)) {
        // Player body base color
        val baseCarColor = Color(0xFF111115) // Sleek titanium dark grey
        val accentNeonColor = Color(0xFFFF007F) // Cyberpunk neon pink trim

        // Left/Right rear wide fenders
        drawRoundRect(
            color = baseCarColor,
            topLeft = Offset(centerX - w / 2f, centerY - h * 0.3f),
            size = Size(w, h * 0.7f),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )

        // Cabin cockpit dome
        val cabW = w * 0.62f
        val cabH = h * 0.45f
        drawRoundRect(
            color = Color(0xFF0D1B2A),
            topLeft = Offset(centerX - cabW / 2f, centerY - h * 0.65f),
            size = Size(cabW, cabH),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        // Windshield glare reflection
        drawOval(
            color = Color.White.copy(0.12f),
            topLeft = Offset(centerX - cabW * 0.28f, centerY - h * 0.62f),
            size = Size(cabW * 0.3f, cabH * 0.5f)
        )

        // Rear Wing/Spoiler
        val wingW = w * 1.05f
        val wingH = h * 0.14f
        drawRect(
            color = accentNeonColor,
            topLeft = Offset(centerX - wingW / 2f, centerY - h * 0.78f),
            size = Size(wingW, wingH)
        )
        // Spoiler columns
        drawRect(baseCarColor, topLeft = Offset(centerX - w * 0.35f, centerY - h * 0.7f), size = Size(scale * 0.6f, h * 0.2f))
        drawRect(baseCarColor, topLeft = Offset(centerX + w * 0.3f, centerY - h * 0.7f), size = Size(scale * 0.6f, h * 0.2f))

        // Rear tires
        drawRoundRect(Color.Black, topLeft = Offset(centerX - w * 0.46f, centerY + h * 0.1f), size = Size(w * 0.18f, h * 0.45f), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))
        drawRoundRect(Color.Black, topLeft = Offset(centerX + w * 0.28f, centerY + h * 0.1f), size = Size(w * 0.18f, h * 0.45f), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))

        // Taillights (Glossy LED bar)
        val lightColor = if (isBraking) Color(0xFFE53935) else Color(0x99FF0055)
        drawRoundRect(
            color = lightColor,
            topLeft = Offset(centerX - w * 0.4f, centerY - h * 0.12f),
            size = Size(w * 0.8f, h * 0.15f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Exhaust pipes
        drawCircle(Color.DarkGray, radius = scale * 0.25f, center = Offset(centerX - scale * 0.9f, centerY + h * 0.35f))
        drawCircle(Color.DarkGray, radius = scale * 0.25f, center = Offset(centerX + scale * 0.9f, centerY + h * 0.35f))

        // If drifting, draw exhaust tire marks sparks
        if (isDrifting) {
            drawCircle(Color(0xFFFFB300), radius = scale * 0.18f, center = Offset(centerX - w * 0.38f, centerY + h * 0.6f))
            drawCircle(Color(0xFFFFB300), radius = scale * 0.18f, center = Offset(centerX + w * 0.38f, centerY + h * 0.6f))
        }

        // Crack / Dent damage indicators on car based on health
        if (damagePercent < 80f) {
            // Draw visual wire scratches
            drawLine(Color.DarkGray, start = Offset(centerX - w * 0.3f, centerY - h * 0.1f), end = Offset(centerX - w * 0.1f, centerY + h * 0.2f), strokeWidth = 2.dp.toPx())
        }
        if (damagePercent < 45f) {
            // Serious dents / sparks
            drawLine(Color.Red, start = Offset(centerX + w * 0.1f, centerY - h * 0.3f), end = Offset(centerX + w * 0.35f, centerY - h * 0.1f), strokeWidth = 3.dp.toPx())
        }
    }
}

fun DrawScope.drawRainOverlay(width: Float, height: Float, speedKmh: Float) {
    // Falling vector diagonal rain lines
    val rand = Random(12412)
    val rainColor = Color(0x8890A4AE)

    val rainLineCount = 35
    for (i in 0 until rainLineCount) {
        val startX = (i * 987 % width)
        val startY = (i * 412 % height)

        // angle slopes forward faster with velocity
        val length = 35.dp.toPx()
        val speedMultiplier = 1f + (speedKmh / 150f)
        val endX = startX - 12.dp.toPx() * speedMultiplier
        val endY = startY + length * speedMultiplier

        drawLine(
            color = rainColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

fun DrawScope.drawFogOverlay(width: Float, height: Float) {
    // Draw semi-transparent gradient fog blanketing the horizon
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xBBBDC3C7), Color(0x00BDC3C7)),
            startY = height / 3f,
            endY = height / 1.5f
        ),
        size = Size(width, height)
    )
}

@Composable
fun GameHUD(engine: GameEngine) {
    val player = engine.player
    val countdown = engine.countdownTime
    val position = engine.currentPositionRank
    val coins = engine.totalCoinsCollectedThisRace
    val sfxState = engine.driftBonusNotification

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // MID LEFT: Real-time Minimap View (Secondary orthographic camera of procedural city map)
        MinimapView(
            engine = engine,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 100.dp)
        )

        // TOP CENTER: Rank Position and Lap Timings
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speedometer Circular Ring
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${engine.currentSpeedKmh}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("KM/H", color = Color(0xFF00FFC2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (player.isNitroActive) "NITRO" else "GEAR 5",
                            color = if (player.isNitroActive) Color(0xFF00E5FF) else Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Drift Bonus Alert Flashers
            AnimatedVisibility(
                visible = sfxState.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (sfxState.contains("DAMAGE") || sfxState.contains("COLLISION")) Color(0xFFD32F2F) else Color(0xFF0D0C1D)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = sfxState,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // TOP LEFT: Position Rankings & Coins
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Leaderboard, contentDescription = "Rank", tint = Color(0xFFFF007F), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (position) {
                            1 -> "1st / 4"
                            2 -> "2nd / 4"
                            3 -> "3rd / 4"
                            else -> "4th / 4"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$coins",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // TOP RIGHT: Damaged Hull and Nitro capacity levels
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Health Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HULL: ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(player.currentHealth / 100f)
                            .background(
                                if (player.currentHealth > 50f) Color(0xFF2ECC71)
                                else if (player.currentHealth > 25f) Color(0xFFF39C12)
                                else Color(0xFFE74C3C)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nitro Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NITRO: ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(player.nitroCapacity / 100f)
                            .background(Color(0xFF00FFC2))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Elapsed Timer
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                val sec = (engine.totalTimeElapsed / 1000) % 60
                val min = (engine.totalTimeElapsed / 60000)
                val ms = (engine.totalTimeElapsed % 1000) / 10
                Text(
                    text = String.format("%02d:%02d.%02d", min, sec, ms),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // BOTTOM LEFT: Siren / Police chase HUD overlays
        if (engine.isPoliceChaseMode && engine.policeDistanceWarning > 0.1f) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (engine.isSirenFlashing) Color(0xFFD32F2F).copy(alpha = 0.6f) else Color(0xFF1976D2).copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 140.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = "Siren", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "POLICE BEHIND: ${(1500f / engine.policeDistanceWarning).roundToInt()}m",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Track Progress Bar (vertical slider along bottom right side)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(16.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(player.trackCompletedPercent)
                    .background(Color(0xFFFF007F))
            )
            // Icon peg
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun GameControls(engine: GameEngine, controlType: Int) {
    val player = engine.player

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // LEFT SIDE: Steering Controllers
        if (controlType == 1) {
            // Interactive 3D Spinning Steering Wheel (Sliding pointer)
            var wheelAngle by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                wheelAngle = (wheelAngle + dragAmount.x).coerceIn(-120f, 120f)
                                player.steeringInput = (wheelAngle / 120f) // normalize input -1 to 1
                            },
                            onDragEnd = {
                                wheelAngle = 0f
                                player.steeringInput = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Draw decorative steering wheel vector representation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    rotate(degrees = wheelAngle, pivot = center) {
                        // Outer Rim
                        drawCircle(
                            color = Color(0xFF00FFC2),
                            radius = size.width * 0.42f,
                            style = Stroke(width = 12.dp.toPx())
                        )
                        // Three spokes
                        drawLine(Color.White, start = center, end = Offset(center.x - size.width * 0.4f, center.y), strokeWidth = 8.dp.toPx())
                        drawLine(Color.White, start = center, end = Offset(center.x + size.width * 0.4f, center.y), strokeWidth = 8.dp.toPx())
                        drawLine(Color.White, start = center, end = Offset(center.x, center.y + size.width * 0.4f), strokeWidth = 8.dp.toPx())
                        // Center hub
                        drawCircle(color = Color.DarkGray, radius = size.width * 0.12f)
                    }
                }
            }
        } else {
            // D-Pad Left / Right arrow buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp)
            ) {
                // Steer Left Button
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(72.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { player.steeringInput = -1.0f },
                                onDrag = { change, _ -> change.consume() },
                                onDragEnd = { player.steeringInput = 0f }
                            )
                        }
                        .testTag("steer_left_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowLeft, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Steer Right Button
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(72.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { player.steeringInput = 1.0f },
                                onDrag = { change, _ -> change.consume() },
                                onDragEnd = { player.steeringInput = 0f }
                            )
                        }
                        .testTag("steer_right_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowRight, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }

        // RIGHT SIDE: Gas / Brake / Nitro Boost Pedals
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Nitro Trigger Button
            if (player.nitroCapacity > 10f) {
                IconButton(
                    onClick = {
                        if (!player.isNitroActive && player.nitroCapacity >= 30f) {
                            player.isNitroActive = true
                            player.nitroTimeRemaining = 3.5f
                            player.nitroCapacity -= 30f
                            GameAudioSynth.playNitro()
                        }
                    },
                    modifier = Modifier
                        .background(Color(0xFF00E5FF).copy(alpha = 0.6f), CircleShape)
                        .size(64.dp)
                        .testTag("nitro_button")
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = "Nitro Boost", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            // Brake / Drift Pedal
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(64.dp)
                    .height(84.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { player.isBraking = true },
                            onDrag = { change, _ -> change.consume() },
                            onDragEnd = { player.isBraking = false }
                        )
                    }
                    .testTag("brake_pedal")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Brake", tint = Color.Red, modifier = Modifier.size(24.dp))
                    Text("SLIDE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Gas Pedal (Go!)
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2).copy(alpha = 0.65f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(72.dp)
                    .height(100.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {},
                            onDrag = { change, _ -> change.consume() },
                            onDragEnd = {}
                        )
                    }
                    .testTag("gas_pedal")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Gas", tint = Color.Black, modifier = Modifier.size(32.dp))
                    Text("DRIVE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun PostRaceDialog(
    engine: GameEngine,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val isVictory = engine.trackCompleted
    val title = if (isVictory) "VICTORY!" else "CRASHED!"
    val colorAccent = if (isVictory) Color(0xFF00FFC2) else Color(0xFFE74C3C)

    // Calculate final coins and stats to display
    val completionSec = (engine.totalTimeElapsed / 1000) % 60
    val completionMin = (engine.totalTimeElapsed / 60000)
    val formattedTime = String.format("%02d:%02d", completionMin, completionSec)

    // Automatically play appropriate sound fanfare upon trigger
    LaunchedEffect(Unit) {
        if (isVictory) {
            GameAudioSynth.playLevelUp()
        } else {
            GameAudioSynth.playCrash()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151421)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .width(340.dp)
                .padding(16.dp)
                .testTag("post_race_dialog"),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Glow Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colorAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVictory) Icons.Default.EmojiEvents else Icons.Default.CarCrash,
                        contentDescription = null,
                        tint = colorAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isVictory) "Ranked #${engine.currentPositionRank} in global cup" else "Engine blew up due to structural damage!",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats rows
                HorizontalDivider(color = Color.Gray.copy(0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                StatRow(label = "TIME", value = formattedTime, icon = Icons.Default.Timer, iconColor = Color.White)
                StatRow(label = "COINS COLLECTED", value = "+${engine.totalCoinsCollectedThisRace}", icon = Icons.Default.MonetizationOn, iconColor = Color(0xFFFFD54F))
                StatRow(label = "DRIFT DISTANCE", value = "${engine.totalDriftDistanceThisRace.roundToInt()}m", icon = Icons.Default.Park, iconColor = Color(0xFFFF007F))
                StatRow(label = "MAX SPEED", value = "${engine.maxSpeedReachedThisRace.roundToInt()} KM/H", icon = Icons.Default.Speed, iconColor = Color(0xFF00FFC2))

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.Gray.copy(0.2f))

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = colorAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PLAY AGAIN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onExit,
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color.Gray)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONTINUE TO MENU", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// Pseudo random generator for falling rain drops inside canvas draw scope
class Random(seed: Int) {
    private var state = seed
    fun nextInt(max: Int): Int {
        state = (state * 1103515245 + 12345) and 0x7fffffff
        return state % max
    }
}
