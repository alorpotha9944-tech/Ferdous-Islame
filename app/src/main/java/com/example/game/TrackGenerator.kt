package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.math.sin

object TrackGenerator {

    fun generateTrack(config: TrackConfig): List<RoadSegment> {
        val segments = ArrayList<RoadSegment>(config.totalSegments)
        val segmentLength = config.segmentLength.toFloat()

        // Define color schemes based on the track and time of day
        val isNeonMidnight = config.id == "track_midnight"
        val isDesert = config.id == "track_desert"

        val colorGrassEven = when {
            isNeonMidnight -> Color(0xFF07050E)
            isDesert -> Color(0xFFCBB677) // Sand light
            else -> Color(0xFF194411) // Grass light
        }
        val colorGrassOdd = when {
            isNeonMidnight -> Color(0xFF030107)
            isDesert -> Color(0xFFBCA15D) // Sand dark
            else -> Color(0xFF10330A) // Grass dark
        }

        val colorRoadEven = when {
            isNeonMidnight -> Color(0xFF120E1E)
            isDesert -> Color(0xFF423B33)
            else -> Color(0xFF383838)
        }
        val colorRoadOdd = when {
            isNeonMidnight -> Color(0xFF0E0B1A)
            isDesert -> Color(0xFF3B352E)
            else -> Color(0xFF303030)
        }

        val colorRumbleEven = when {
            isNeonMidnight -> Color(0xFFFF0060) // Neon Magenta
            isDesert -> Color(0xFFFFFFFF)
            else -> Color(0xFFE53935)
        }
        val colorRumbleOdd = when {
            isNeonMidnight -> Color(0xFF00FFC2) // Neon Teal
            isDesert -> Color(0xFFB0BEC5)
            else -> Color(0xFFFFFFFF)
        }

        val colorStripEven = when {
            isNeonMidnight -> Color(0xFF00FFC2)
            else -> Color(0xFFFFFFFF)
        }
        val colorStripOdd = when {
            isNeonMidnight -> Color(0x00000000) // Translucent odd strips
            else -> Color(0x00000000)
        }

        var currentX = 0f
        var currentY = 0f

        for (i in 0 until config.totalSegments) {
            // Curvature math (using sin waves to generate smooth sweeps)
            var curve = 0f
            if (i > 100 && i < 250) curve = 2f // Curve right
            if (i > 300 && i < 450) curve = -3f // Curve left
            if (i > 600 && i < 800) curve = 1.5f
            if (i > 850 && i < 950) curve = -4f // Sharp curve left
            if (i > 1000 && i < 1200) {
                // Complex S-curve
                curve = sin(i / 30f) * 3.5f
            }
            if (isNeonMidnight && i > 1200 && i < 1700) {
                // Extreme neon midnight curves
                curve = sin(i / 20f) * 5f
            }

            // Elevation hills (up and down)
            var hill = 0f
            if (i > 150 && i < 300) {
                hill = sin((i - 150) / 47.7f) * 60f // Smooth roll hill
            }
            if (isDesert && i > 400 && i < 700) {
                // Giant dunes (massive leaps!)
                hill = sin((i - 400) / 20f) * 120f
            }
            if (i > 900 && i < 1100) {
                hill = sin((i - 900) / 30f) * -50f // Dip down
            }

            // Alternate colors every 3 segments (classic retro look)
            val isEven = (i / 3) % 2 == 0
            val colorG = if (isEven) colorGrassEven else colorGrassOdd
            val colorR = if (isEven) colorRoadEven else colorRoadOdd
            val colorRb = if (isEven) colorRumbleEven else colorRumbleOdd
            val colorS = if (isEven) colorStripEven else colorStripOdd

            val p1 = Point3D(currentX, currentY, i * segmentLength)

            // Update world geometry accumulate curve and hill for next segment start point
            currentX += curve * 2.5f
            currentY += hill * 0.4f

            val p2 = Point3D(currentX, currentY, (i + 1) * segmentLength)

            val segment = RoadSegment(
                index = i,
                p1 = p1,
                p2 = p2,
                curve = curve,
                hill = hill,
                colorRoad = colorR,
                colorGrass = colorG,
                colorRumble = colorRb,
                colorStrip = colorS
            )

            // Spawn items (Coins, Nitro, Repairs, Obstacles)
            // Skip the first 100 segments (starting line) and the last 50 segments (finish line)
            if (i in 100..(config.totalSegments - 50)) {
                val randVal = (i * 12431 % 100) / 100f // Deterministic pseudo-randomness

                when {
                    // Coins
                    randVal < 0.12f -> {
                        // Spawn a line of 3 coins on a lane offset
                        val offset = if ((i / 5) % 2 == 0) -0.5f else 0.5f
                        segment.items.add(TrackItem(TrackItemType.COIN, offset))
                    }
                    // Nitro Boosts
                    randVal in 0.12f..0.15f -> {
                        val offset = if ((i / 13) % 3 == 0) -0.7f else if ((i / 13) % 3 == 1) 0.0f else 0.7f
                        segment.items.add(TrackItem(TrackItemType.NITRO, offset))
                    }
                    // Repair Kits (very important due to damage)
                    randVal in 0.15f..0.17f -> {
                        val offset = if ((i / 19) % 2 == 0) -0.3f else 0.3f
                        segment.items.add(TrackItem(TrackItemType.REPAIR, offset))
                    }
                    // Traffic Cones
                    randVal in 0.20f..0.25f -> {
                        val offset = if ((i * 7) % 2 == 0) -0.6f else 0.6f
                        segment.items.add(TrackItem(TrackItemType.CONE, offset))
                    }
                    // Road Obstacles (Rocks or Neon Barriers)
                    randVal in 0.25f..0.29f -> {
                        val offset = if ((i * 11) % 3 == 0) -0.4f else if ((i * 11) % 3 == 1) 0.4f else 0.0f
                        val type = if (isNeonMidnight) TrackItemType.NEON_SIGN else TrackItemType.ROCK
                        segment.items.add(TrackItem(type, offset))
                    }
                    // Police roadblock spikes (spawns in police chase mode or randomly)
                    randVal in 0.30f..0.31f -> {
                        val offset = if ((i * 13) % 2 == 0) -0.2f else 0.2f
                        segment.items.add(TrackItem(TrackItemType.POLICE_BLOCK, offset))
                    }
                }
            }

            segments.add(segment)
        }

        return segments
    }
}
