package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MinimapView(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    val segments = engine.segments
    val segmentLength = engine.trackConfig.segmentLength.toFloat()
    val roadWidth = engine.trackConfig.roadWidth
    val player = engine.player

    // Generate or cache the procedural city map
    val cityMap = remember(segments) {
        ProceduralCityMap(
            trackSegments = segments,
            segmentLength = segmentLength,
            roadWidth = roadWidth
        )
    }

    // Zoom states: 1 = Wide, 2 = Medium (Default), 3 = Close
    var zoomLevel by remember { mutableStateOf(2) }
    val zoomScale = when (zoomLevel) {
        1 -> 25000f // wide view
        2 -> 12000f // standard view
        else -> 6000f // close-up view
    }

    // Capture to offscreen Render Texture
    val textureSize = 320f
    val renderTexture = remember { ImageBitmap(textureSize.toInt(), textureSize.toInt()) }
    val composeCanvas = remember { Canvas(renderTexture) }
    val drawScope = remember { CanvasDrawScope() }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // Helper to interpolate track center X at any Z distance
    fun getTrackCenterX(z: Float): Float {
        if (segments.isEmpty()) return 0f
        val segIndex = (z / segmentLength).toInt()
        val wrappedIndex = segIndex % segments.size
        val segment = segments[wrappedIndex]
        val t = (z % segmentLength) / segmentLength
        val nextSegment = segments[(wrappedIndex + 1) % segments.size]
        return segment.p1.x + (nextSegment.p1.x - segment.p1.x) * t
    }

    // Compute player's current global position on the 2D map plane
    val playerWorldX = getTrackCenterX(player.z) + player.x * roadWidth
    val playerWorldZ = player.z

    // Compute player current heading (rotation angle in degrees)
    val playerSegIndex = (player.z / segmentLength).toInt()
    val nextIndex = (playerSegIndex + 2) % segments.size
    val dx = segments[nextIndex].p1.x - segments[playerSegIndex].p1.x
    val dz = segmentLength * 2f
    val playerHeadingRad = atan2(dx, dz)
    val playerHeadingDeg = Math.toDegrees(playerHeadingRad.toDouble()).toFloat()

    // Render loop side effect (updates our Render Texture)
    LaunchedEffect(playerWorldX, playerWorldZ, zoomLevel, engine.totalTimeElapsed) {
        drawScope.draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = composeCanvas,
            size = Size(textureSize, textureSize)
        ) {
            val center = textureSize / 2f
            val scale = textureSize / zoomScale

            // 1. Clear Texture with deep cyberpunk radar grid background
            drawRect(Color(0xFF0C0D14))

            // Map coordinate converter lambda: World (X, Z) -> Screen Pixel (X, Y)
            fun worldToScreen(wx: Float, wz: Float): Offset {
                val rx = wx - playerWorldX
                val rz = wz - playerWorldZ
                val sx = center + rx * scale
                val sy = center - rz * scale
                return Offset(sx, sy)
            }

            // Draw radial sonar lines & cyber grid lines on texture
            val gridColor = Color(0x1800FFC2)
            for (r in 1..4) {
                drawCircle(
                    color = gridColor,
                    radius = (zoomScale / 8f * r) * scale,
                    style = Stroke(width = 1f)
                )
            }
            // Vertical and Horizontal crosshairs
            drawLine(gridColor, Offset(0f, center), Offset(textureSize, center), strokeWidth = 1f)
            drawLine(gridColor, Offset(center, 0f), Offset(center, textureSize), strokeWidth = 1f)

            // 2. Render River Shoreline (Beaches) and River Path
            for (beach in cityMap.beaches) {
                val beachPath = Path()
                if (beach.points.isNotEmpty()) {
                    val pStart = worldToScreen(beach.points.first().x, beach.points.first().y)
                    beachPath.moveTo(pStart.x, pStart.y)
                    for (i in 1 until beach.points.size) {
                        val p = worldToScreen(beach.points[i].x, beach.points[i].y)
                        beachPath.lineTo(p.x, p.y)
                    }
                    drawPath(
                        path = beachPath,
                        color = Color(0x33F4E0A2), // Translucent Sandy Beach Shore
                        style = Stroke(width = beach.width * scale, cap = StrokeCap.Round)
                    )
                }
            }

            for (river in cityMap.rivers) {
                val riverPath = Path()
                if (river.points.isNotEmpty()) {
                    val pStart = worldToScreen(river.points.first().x, river.points.first().y)
                    riverPath.moveTo(pStart.x, pStart.y)
                    for (i in 1 until river.points.size) {
                        val p = worldToScreen(river.points[i].x, river.points[i].y)
                        riverPath.lineTo(p.x, p.y)
                    }
                    drawPath(
                        path = riverPath,
                        color = Color(0x6600E5FF), // Glowing Neon River Blue
                        style = Stroke(width = river.width * scale, cap = StrokeCap.Round)
                    )
                }
            }

            // 3. Render Forests
            for (forest in cityMap.forests) {
                val fCenter = worldToScreen(forest.center.x, forest.center.y)
                drawCircle(
                    color = Color(0x222ECC71), // Forest Area Green
                    radius = forest.radius * scale
                )
                // Small tree detail vector cross
                drawCircle(
                    color = Color(0x4427AE60),
                    radius = forest.radius * 0.75f * scale,
                    style = Stroke(width = 1.5f)
                )
            }

            // 4. Render Roads Grid & Highways
            for (road in cityMap.roads) {
                val pStart = worldToScreen(road.start.x, road.start.y)
                val pEnd = worldToScreen(road.end.x, road.end.y)
                
                // If road crosses river, it acts as a Bridge
                val crossesRiver = cityMap.getDistanceToRiver((road.start.x + road.end.x) / 2f, (road.start.y + road.end.y) / 2f) < 800f
                val roadColor = if (crossesRiver) Color(0xFF94A3B8) else if (road.isHighway) Color(0xFF475569) else Color(0xFF1E293B)
                val roadThickness = (if (road.isHighway) 500f else 250f) * scale

                drawLine(
                    color = roadColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = roadThickness,
                    cap = StrokeCap.Round
                )

                // If bridge, draw structural bridge cross-frames
                if (crossesRiver) {
                    drawLine(
                        color = Color.White.copy(0.4f),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = roadThickness * 1.3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                    )
                }
            }

            // 5. Render Parking Areas
            for (park in cityMap.parkingAreas) {
                val pPos = worldToScreen(park.rect.x, park.rect.y)
                drawRoundRect(
                    color = Color(0x22475569),
                    topLeft = pPos,
                    size = Size(park.size.width * scale, park.size.height * scale),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0x4494A3B8),
                    topLeft = pPos,
                    size = Size(park.size.width * scale, park.size.height * scale),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 1f)
                )
            }

            // 6. Render Buildings Block Rectangles
            for (bld in cityMap.buildings) {
                val bPos = worldToScreen(bld.rect.x, bld.rect.y)
                // Draw filled structure
                drawRect(
                    color = bld.color,
                    topLeft = bPos,
                    size = Size(bld.size.width * scale, bld.size.height * scale)
                )
                // Draw holographic cyber neon outline
                drawRect(
                    color = Color(0xFF00FFC2).copy(alpha = 0.25f),
                    topLeft = bPos,
                    size = Size(bld.size.width * scale, bld.size.height * scale),
                    style = Stroke(width = 1.2f)
                )
            }

            // 7. Render Gas Stations
            for (gas in cityMap.gasStations) {
                val gPos = worldToScreen(gas.position.x, gas.position.y)
                drawCircle(
                    color = Color(0xFF2ECC71),
                    radius = 220f * scale
                )
                drawCircle(
                    color = Color.White,
                    radius = 220f * scale,
                    style = Stroke(width = 1.5f)
                )
            }

            // 8. Render Main Race Track Circuit Loop
            val trackPath = Path()
            if (segments.isNotEmpty()) {
                val firstX = getTrackCenterX(segments.first().p1.z)
                val pStart = worldToScreen(firstX, segments.first().p1.z)
                trackPath.moveTo(pStart.x, pStart.y)
                
                // Sample segments dynamically to keep drawing extremely lightweight
                val step = when (zoomLevel) {
                    1 -> 15 // wide view can sample less
                    2 -> 8
                    else -> 4 // close view samples more
                }

                for (i in step until segments.size step step) {
                    val seg = segments[i]
                    val sx = getTrackCenterX(seg.p1.z)
                    val p = worldToScreen(sx, seg.p1.z)
                    
                    // Detect if tunnel (passes through forest areas)
                    val isTunnel = cityMap.getDistanceToRiver(sx, seg.p1.z) > 4000f && cityMap.getDistanceToTrack(sx, seg.p1.z) < 100f && (i in 400..550 || i in 900..1050)
                    
                    if (isTunnel) {
                        // Lift pen and break path slightly or we draw the path dashed
                        drawCircle(Color.DarkGray, radius = 400f * scale, center = p)
                    }
                    trackPath.lineTo(p.x, p.y)
                }
                
                // Draw glowing main neon race line
                drawPath(
                    path = trackPath,
                    color = Color(0xFFFF007F), // Cybersecurity neon pink
                    style = Stroke(width = 800f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Inner speed-strip
                drawPath(
                    path = trackPath,
                    color = Color.White,
                    style = Stroke(width = 150f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 9. Render Nearby Traffic Entities on Road
            for (tc in engine.trafficCars) {
                val tcX = getTrackCenterX(tc.z) + tc.x * roadWidth
                val dist = kotlin.math.abs(tc.z - player.z)
                if (dist < zoomScale * 0.7f) {
                    val tcScreen = worldToScreen(tcX, tc.z)
                    drawCircle(
                        color = Color(0xFFF1C40F), // Yellow traffic dots
                        radius = 180f * scale
                    )
                }
            }

            // 10. Render Opponent AI Cars
            for (ai in engine.aiCars) {
                val aiX = getTrackCenterX(ai.z) + ai.x * roadWidth
                val dist = kotlin.math.abs(ai.z - player.z)
                if (dist < zoomScale * 0.7f) {
                    val aiScreen = worldToScreen(aiX, ai.z)
                    drawCircle(
                        color = Color(0xFFFF5722), // Orange AI opponent racer dots
                        radius = 240f * scale
                    )
                    // Outer accent ring
                    drawCircle(
                        color = Color.White,
                        radius = 320f * scale,
                        style = Stroke(width = 1f)
                    )
                }
            }

            // 11. Render Chasing Police Cars
            for (cop in engine.policeCars) {
                val copX = getTrackCenterX(cop.z) + cop.x * roadWidth
                val dist = kotlin.math.abs(cop.z - player.z)
                if (dist < zoomScale * 0.7f) {
                    val copScreen = worldToScreen(copX, cop.z)
                    // Flashing red/blue strobe cop radar indicator
                    val copColor = if (engine.isSirenFlashing) Color(0xFFE74C3C) else Color(0xFF2980B9)
                    drawCircle(
                        color = copColor,
                        radius = 260f * scale
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 350f * scale,
                        style = Stroke(width = 1.2f)
                    )
                }
            }

            // 12. Render Player (Centered on screen)
            // Pulse radar sweep circle
            val pulseRad = (1000f + (engine.totalTimeElapsed % 1000f) / 1000f * 2000f) * scale
            drawCircle(
                color = Color(0x3300FFC2),
                radius = pulseRad,
                center = Offset(center, center),
                style = Stroke(width = 2f)
            )

            // Draw player car custom rotating arrow head
            rotate(degrees = playerHeadingDeg, pivot = Offset(center, center)) {
                val playerArrow = Path().apply {
                    moveTo(center, center - 12f) // Nose pointing up
                    lineTo(center - 7f, center + 9f) // Bottom left
                    lineTo(center, center + 5f) // Tail center recess
                    lineTo(center + 7f, center + 9f) // Bottom right
                    close()
                }
                drawPath(
                    path = playerArrow,
                    color = Color(0xFF00FFC2) // Glowing cyan pointer
                )
                drawPath(
                    path = playerArrow,
                    color = Color.White,
                    style = Stroke(width = 2f, join = StrokeJoin.Round)
                )
            }
        }
    }

    // Modern HUD Container Bezel Card for the circular radar map
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color(0xFF00FFC2), Color(0xFFFF007F)))),
        modifier = modifier
            .width(170.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = Color(0xFF00FFC2),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "GPS LOCK: OK",
                        color = Color(0xFF00FFC2),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                val compassHeading = when {
                    playerHeadingDeg in -22.5f..22.5f -> "N"
                    playerHeadingDeg in 22.5f..67.5f -> "NE"
                    playerHeadingDeg in 67.5f..112.5f -> "E"
                    playerHeadingDeg in 112.5f..157.5f -> "SE"
                    playerHeadingDeg in -67.5f..-22.5f -> "NW"
                    playerHeadingDeg in -112.5f..-67.5f -> "W"
                    playerHeadingDeg in -157.5f..-112.5f -> "SW"
                    else -> "S"
                }
                Text(
                    text = compassHeading,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(Color(0xFFFF007F).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // The Circular Render Texture View
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFF334155), CircleShape)
                    .background(Color(0xFF0C0D14)),
                contentAlignment = Alignment.Center
            ) {
                // Secondary orthographic camera captured frame drawn to Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = renderTexture,
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                    )
                    
                    // Draw dynamic compass overlay points around bezel
                    val radAngle = Math.toRadians(playerHeadingDeg.toDouble())
                    val cosA = cos(radAngle).toFloat()
                    val sinA = sin(radAngle).toFloat()
                    
                    // North Indicator tick mark on dial
                    drawCircle(
                        color = Color(0xFFFF007F),
                        radius = 3.dp.toPx(),
                        center = Offset(size.width / 2f + sinA * (size.width / 2f - 6f), size.height / 2f - cosA * (size.height / 2f - 6f))
                    )
                }

                // Center crosshairs ring decorative overlay
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .border(0.5.dp, Color(0x3300FFC2).copy(alpha = 0.15f), CircleShape)
                )

                // Tactical radar sweeping visual line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.width / 2f
                    val sweepAngleRad = (engine.totalTimeElapsed * 0.004f) % (2f * Math.PI)
                    val endX = radius + radius * cos(sweepAngleRad).toFloat()
                    val endY = radius + radius * sin(sweepAngleRad).toFloat()
                    drawLine(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x7700FFC2), Color(0x0000FFC2)),
                            center = center,
                            radius = radius
                        ),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Zoom Buttons Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom out button
                IconButton(
                    onClick = { if (zoomLevel > 1) zoomLevel-- },
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(0.08f), RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Range Scale text
                Text(
                    text = when (zoomLevel) {
                        1 -> "R: 25KM"
                        2 -> "R: 12KM"
                        else -> "R: 6KM"
                    },
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Zoom in button
                IconButton(
                    onClick = { if (zoomLevel < 3) zoomLevel++ },
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(0.08f), RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
