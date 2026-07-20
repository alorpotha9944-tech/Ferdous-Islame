package com.example.game

import androidx.compose.ui.graphics.Color

data class Point3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
)

data class Point2D(
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 0f
)

enum class TrackItemType {
    COIN,
    NITRO,
    REPAIR,
    ROCK,
    CONE,
    NEON_SIGN,
    POLICE_BLOCK
}

data class TrackItem(
    val type: TrackItemType,
    val offset: Float, // -1.0f to 1.0f represents road lane, >1.0f or <-1.0f represent offroad
    var isCollected: Boolean = false,
    val width: Float = 0.5f // physical scale size
)

data class RoadSegment(
    val index: Int,
    val p1: Point3D,
    val p2: Point3D,
    val curve: Float,
    val hill: Float,
    var clip: Float = 0f,
    val colorRoad: Color,
    val colorGrass: Color,
    val colorRumble: Color,
    val colorStrip: Color,
    val items: MutableList<TrackItem> = mutableListOf()
)

enum class Weather {
    CLEAR,
    RAIN,
    FOG
}

enum class TimeOfDay {
    SUNSET,
    NIGHT,
    DAWN,
    DAY
}

data class TrackConfig(
    val id: String,
    val name: String,
    val totalSegments: Int,
    val segmentLength: Int = 200, // z distance per segment
    val initialWeather: Weather,
    val initialTimeOfDay: TimeOfDay,
    val difficultyMultiplier: Float,
    val roadWidth: Float = 2000f,
    val baseLaneCount: Int = 3
) {
    fun getSkyColors(): Pair<Color, Color> {
        return when (initialTimeOfDay) {
            TimeOfDay.SUNSET -> Pair(Color(0xFFE65C00), Color(0xFF190A05)) // Beautiful orange to dark violet
            TimeOfDay.NIGHT -> Pair(Color(0xFF0D0C1D), Color(0xFF03010C))  // Cyberpunk dark blue to pitch black
            TimeOfDay.DAWN -> Pair(Color(0xFFF3904F), Color(0xFF3B4371))   // Pink dawn purple
            TimeOfDay.DAY -> Pair(Color(0xFF4CA1AF), Color(0xFF2C3E50))    // Bright modern teal blue
        }
    }
}
