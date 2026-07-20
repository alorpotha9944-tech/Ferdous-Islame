package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.math.sqrt

data class ProceduralRiver(
    val points: List<Offset>,
    val width: Float = 1400f
)

data class ProceduralBeach(
    val points: List<Offset>,
    val width: Float = 2200f // slightly wider than river to create shores
)

data class ProceduralForest(
    val center: Offset,
    val radius: Float
)

data class ProceduralRoad(
    val start: Offset,
    val end: Offset,
    val isHighway: Boolean = false,
    val isTunnel: Boolean = false
)

data class ProceduralBuilding(
    val rect: Offset,
    val size: Size,
    val height: Float,
    val color: Color
)

data class ProceduralGasStation(
    val position: Offset,
    val name: String
)

data class ProceduralParking(
    val rect: Offset,
    val size: Size
)

data class ProceduralOffRoad(
    val points: List<Offset>,
    val width: Float = 400f
)

class ProceduralCityMap(
    val trackSegments: List<RoadSegment>,
    val segmentLength: Float,
    val roadWidth: Float
) {
    val rivers = ArrayList<ProceduralRiver>()
    val beaches = ArrayList<ProceduralBeach>()
    val forests = ArrayList<ProceduralForest>()
    val roads = ArrayList<ProceduralRoad>()
    val buildings = ArrayList<ProceduralBuilding>()
    val gasStations = ArrayList<ProceduralGasStation>()
    val parkingAreas = ArrayList<ProceduralParking>()
    val offRoadAreas = ArrayList<ProceduralOffRoad>()
    
    // Bounds of the city map
    var minX = -20000f
    var maxX = 20000f
    var minZ = -10000f
    var maxZ = 250000f // default for a track of 1200 segments
    
    init {
        generateMap()
    }
    
    private fun generateMap() {
        val totalTrackLength = trackSegments.size * segmentLength
        maxZ = totalTrackLength + 10000f
        
        // Find actual track lateral boundary
        var trackMinX = 0f
        var trackMaxX = 0f
        for (seg in trackSegments) {
            if (seg.p1.x < trackMinX) trackMinX = seg.p1.x
            if (seg.p1.x > trackMaxX) trackMaxX = seg.p1.x
        }
        
        minX = trackMinX - 25000f
        maxX = trackMaxX + 25000f
        
        // 1. Procedural River: Winding diagonally across the city map
        val riverPoints = ArrayList<Offset>()
        val stepZ = 10000f
        var currentZ = minZ - 10000f
        while (currentZ <= maxZ + 10000f) {
            // Smooth sine curve for river winding
            val riverX = (minX + maxX) / 2f + sin(currentZ / 40000f) * 12000f
            riverPoints.add(Offset(riverX, currentZ))
            currentZ += stepZ
        }
        rivers.add(ProceduralRiver(riverPoints))
        beaches.add(ProceduralBeach(riverPoints)) // Shoreline surrounding the river
        
        // 2. Procedural Forests
        // Place multiple forest green circles at locations not overlapping too much with the race track
        for (i in 0 until 16) {
            val fZ = minZ + i * (maxZ - minZ) / 16f + (i * 321 % 6000f)
            val fX = if (i % 2 == 0) minX + 5000f + (i * 123 % 4000f) else maxX - 5000f - (i * 123 % 4000f)
            
            // Avoid spawning trees directly on the track
            if (getDistanceToTrack(fX, fZ) > 2000f) {
                forests.add(ProceduralForest(Offset(fX, fZ), 1800f + (i * 77 % 1000f)))
            }
        }
        
        // 3. Grid of Roads and Highways
        // Main North-South highways near left and right edges
        roads.add(ProceduralRoad(Offset(minX + 4000f, minZ), Offset(minX + 4000f, maxZ), isHighway = true))
        roads.add(ProceduralRoad(Offset(maxX - 4000f, minZ), Offset(maxX - 4000f, maxZ), isHighway = true))
        
        // A couple of horizontal highways crossing everything
        val crossHighwayCount = 6
        for (i in 0 until crossHighwayCount) {
            val hz = minZ + (i + 0.5f) * (maxZ - minZ) / crossHighwayCount
            roads.add(ProceduralRoad(Offset(minX, hz), Offset(maxX, hz), isHighway = true))
        }
        
        // Secondary smaller city streets to form a grid
        val streetCountZ = 24
        for (i in 0 until streetCountZ) {
            val sz = minZ + i * (maxZ - minZ) / streetCountZ
            roads.add(ProceduralRoad(Offset(minX + 4000f, sz), Offset(maxX - 4000f, sz), isHighway = false))
        }
        
        val streetCountX = 12
        for (i in 0 until streetCountX) {
            val sx = minX + 4000f + i * (maxX - minX - 8000f) / streetCountX
            roads.add(ProceduralRoad(Offset(sx, minZ), Offset(sx, maxZ), isHighway = false))
        }
        
        // Tunnels: Mark road segments crossing through mountains/forests as tunnels
        // (We can dynamically draw them on the canvas)
        
        // 4. Buildings block clusters
        // Generate buildings inside blocks formed by grid, avoiding the river and the race track path
        val buildingColors = listOf(
            Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFF7F8C8D),
            Color(0xFF16A085), Color(0xFF27AE60), Color(0xFF2980B9),
            Color(0xFF8E44AD), Color(0xFFD35400)
        )
        
        for (bz in 0 until 20) {
            val blockZ = minZ + bz * (maxZ - minZ) / 20f + 2000f
            for (bx in 0 until 8) {
                val blockX = minX + 5000f + bx * (maxX - minX - 10000f) / 8f + 1000f
                
                // Check if this block center is near the river or the race track
                val distToRiver = getDistanceToRiver(blockX, blockZ)
                if (distToRiver < 2500f) continue // beach area
                
                val distToTrack = getDistanceToTrack(blockX, blockZ)
                if (distToTrack < 1800f) continue // race track safety margin
                
                // Spawn a few buildings inside this block
                val bWidth = 800f + (bx * 41 % 500f)
                val bHeight = 800f + (bz * 19 % 500f)
                val color = buildingColors[(bx + bz) % buildingColors.size].copy(alpha = 0.5f)
                
                buildings.add(
                    ProceduralBuilding(
                        rect = Offset(blockX - bWidth / 2f, blockZ - bHeight / 2f),
                        size = Size(bWidth, bHeight),
                        height = 20f + (bx * bz * 7 % 150f),
                        color = color
                    )
                )
                
                // Spawn a parking area next to the buildings in some blocks
                if ((bx + bz) % 4 == 0) {
                    parkingAreas.add(
                        ProceduralParking(
                            rect = Offset(blockX + bWidth / 2f + 150f, blockZ - bHeight / 2f),
                            size = Size(500f, bHeight)
                        )
                    )
                }
            }
        }
        
        // 5. Gas Stations: near intersections of highways
        for (i in 0 until 8) {
            val gZ = minZ + (i * 0.12f + 0.05f) * (maxZ - minZ)
            val gX = minX + 5000f + (i * 421 % 8000f)
            
            if (getDistanceToTrack(gX, gZ) > 1500f && getDistanceToRiver(gX, gZ) > 2000f) {
                gasStations.add(ProceduralGasStation(Offset(gX, gZ), "NEON NITRO STATION #${i + 1}"))
            }
        }
        
        // 6. Off-road desert/dirt trails
        for (i in 0 until 6) {
            val trailPoints = ArrayList<Offset>()
            val startZ = minZ + (i * 0.16f + 0.1f) * (maxZ - minZ)
            val startX = minX + 3000f
            var curX = startX
            var curZ = startZ
            for (j in 0 until 8) {
                trailPoints.add(Offset(curX, curZ))
                curX += 2500f + sin(j * 45f) * 600f
                curZ += 1200f
            }
            offRoadAreas.add(ProceduralOffRoad(trailPoints))
        }
    }
    
    // Helper to get minimum distance to the river curve
    fun getDistanceToRiver(x: Float, z: Float): Float {
        var minDist = Float.MAX_VALUE
        for (p in rivers.first().points) {
            val dist = getDistance(x, z, p.x, p.y)
            if (dist < minDist) {
                minDist = dist
            }
        }
        return minDist
    }
    
    // Helper to get minimum distance to the race track segments
    fun getDistanceToTrack(x: Float, z: Float): Float {
        var minDist = Float.MAX_VALUE
        // Sample every 15 segments to keep check fast but precise
        for (i in 0 until trackSegments.size step 15) {
            val seg = trackSegments[i]
            val dist = getDistance(x, z, seg.p1.x, seg.p1.z)
            if (dist < minDist) {
                minDist = dist
            }
        }
        return minDist
    }
    
    private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
