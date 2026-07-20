package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.MissionEntity
import com.example.data.PlayerProfile
import com.example.data.UnlockedCar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class GameEngine(
    val trackConfig: TrackConfig,
    val isPoliceChaseMode: Boolean = false,
    val activeCar: UnlockedCar,
    val playerProfile: PlayerProfile,
    val onRaceFinished: (coinsWon: Int, xpEarned: Int, completionTimeMs: Long, isVictory: Boolean, completedMissions: List<String>) -> Unit
) {
    // State of the road track
    val segments: List<RoadSegment> = TrackGenerator.generateTrack(trackConfig)
    val totalTrackLength: Float = segments.size * trackConfig.segmentLength.toFloat()

    // Interactive Entities
    val player = PlayerCar().apply {
        applyUpgrades(
            engineLevel = activeCar.engineLevel,
            brakesLevel = activeCar.brakesLevel,
            turboLevel = activeCar.turboLevel,
            tiresLevel = activeCar.tiresLevel
        )
    }

    val aiCars = mutableStateListOf<AICar>()
    val trafficCars = mutableStateListOf<TrafficCar>()
    val policeCars = mutableStateListOf<AICar>() // Chasing police cars

    // Race conditions
    var isRunning by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var countdownTime by mutableStateOf(3) // 3, 2, 1, GO!
    var totalTimeElapsed by mutableStateOf(0L) // In ms
    var trackCompleted by mutableStateOf(false)
    var isCrashEnding by mutableStateOf(false) // Game over via damage

    // HUD States
    var currentSpeedKmh by mutableStateOf(0)
    var driftBonusNotification by mutableStateOf("")
    var currentPositionRank by mutableStateOf(4) // Start in 4th place
    var policeDistanceWarning by mutableStateOf(0f) // 0 to 1 distance representation, >0 triggers warning flashing
    var isSirenFlashing by mutableStateOf(false)

    // Current weather and lighting (dynamic shifts)
    var weather by mutableStateOf(trackConfig.initialWeather)
    var skyColorStart by mutableStateOf(trackConfig.getSkyColors().first)
    var skyColorEnd by mutableStateOf(trackConfig.getSkyColors().second)

    // Stats compiled during session
    var totalCoinsCollectedThisRace = 0
    var totalDriftDistanceThisRace = 0f
    var maxSpeedReachedThisRace = 0f
    var policeEscapedCount = 0

    // Timing helper
    private var countdownTimer = 0f

    init {
        resetRace()
    }

    fun resetRace() {
        player.reset()
        player.applyUpgrades(
            engineLevel = activeCar.engineLevel,
            brakesLevel = activeCar.brakesLevel,
            turboLevel = activeCar.turboLevel,
            tiresLevel = activeCar.tiresLevel
        )

        aiCars.clear()
        trafficCars.clear()
        policeCars.clear()

        // Setup AI Racers ahead of player at start
        aiCars.add(AICar("ai_1", "Apex Phantom", maxSpeed = 230f, accel = 8.5f, vehicleColor = VehicleColor.BLUE, targetLane = -0.5f).apply { z = 1200f })
        aiCars.add(AICar("ai_2", "Shadow GT", maxSpeed = 215f, accel = 7.8f, vehicleColor = VehicleColor.ORANGE, targetLane = 0.5f).apply { z = 800f })
        aiCars.add(AICar("ai_3", "Racer Neon", maxSpeed = 195f, accel = 7.0f, vehicleColor = VehicleColor.PURPLE, targetLane = 0.0f).apply { z = 400f })

        // Populate initial traffic cars distributed spaced out down the road
        for (i in 0 until 12) {
            val spawnZ = 2000f + i * 2500f + (i * 37 % 500f)
            val lane = if (i % 2 == 0) -0.5f else 0.5f
            trafficCars.add(TrafficCar("traffic_$i", x = lane, z = spawnZ, speed = 80f + (i * 7 % 30f)))
        }

        // Setup police chase if mode is active
        if (isPoliceChaseMode) {
            policeCars.add(
                AICar("police_1", "Interceptor Police", maxSpeed = 240f, accel = 10f, vehicleColor = VehicleColor.POLICE, targetLane = 0f).apply {
                    z = -800f // starts trailing far behind
                }
            )
        }

        isRunning = false
        isPaused = false
        countdownTime = 3
        countdownTimer = 1.0f
        totalTimeElapsed = 0L
        trackCompleted = false
        isCrashEnding = false
        currentSpeedKmh = 0
        totalCoinsCollectedThisRace = 0
        totalDriftDistanceThisRace = 0f
        maxSpeedReachedThisRace = 0f
        policeEscapedCount = 0
        currentPositionRank = 4
        policeDistanceWarning = 0f
        isSirenFlashing = false
    }

    fun startRace() {
        isRunning = true
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun tick(dt: Float) {
        if (!isRunning || isPaused || isCrashEnding || trackCompleted) return

        // 1. Handle Pre-Race Countdown
        if (countdownTime > 0) {
            countdownTimer -= dt
            if (countdownTimer <= 0f) {
                countdownTime--
                countdownTimer = 1.0f
                if (countdownTime == 0) {
                    // Start engine audio cue or visual alert
                }
            }
            return
        }

        // Increment timer
        totalTimeElapsed += (dt * 1000).toLong()

        // 2. Weather & Day Night Dynamic transitions
        updateEnvironment(dt)

        // 3. Player Car Physics
        updatePlayerPhysics(dt)

        // 4. Update AI Racers
        updateAICars(dt)

        // 5. Update Traffic Cars
        updateTraffic(dt)

        // 6. Update Police Chases
        if (isPoliceChaseMode) {
            updatePoliceChase(dt)
        }

        // 7. Collision Detection with Road Items & Obstacles
        checkCollisions()

        // 8. Position Tracking (Ranks)
        calculatePositions()

        // 9. Completion Check
        if (player.z >= totalTrackLength) {
            player.z = totalTrackLength
            player.speed = 0f
            trackCompleted = true
            handleRaceCompletion(isVictory = true)
        }

        // 10. Damage Crash Check
        if (player.currentHealth <= 0f) {
            player.speed = 0f
            isCrashEnding = true
            handleRaceCompletion(isVictory = false)
        }
    }

    private fun updateEnvironment(dt: Float) {
        // Slow sky shift logic to simulate real time (Sunset -> Night etc)
        if (trackConfig.initialTimeOfDay == TimeOfDay.SUNSET) {
            val factor = min(1f, totalTimeElapsed / 90000f) // shift to night over 90 seconds
            skyColorStart = Color(
                red = (1f - factor) * 0.9f + factor * 0.05f,
                green = (1f - factor) * 0.36f + factor * 0.05f,
                blue = (1f - factor) * 0.0f + factor * 0.11f
            )
            skyColorEnd = Color(
                red = (1f - factor) * 0.1f + factor * 0.01f,
                green = (1f - factor) * 0.04f + factor * 0.01f,
                blue = (1f - factor) * 0.05f + factor * 0.05f
            )
        }
    }

    private fun updatePlayerPhysics(dt: Float) {
        val activeSegment = getSegmentAt(player.z)
        val roadCurve = activeSegment.curve

        // Grip changes with weather
        val weatherGripFactor = when (weather) {
            Weather.RAIN -> 0.6f  // reduces grip dramatically, slippery!
            Weather.FOG -> 0.95f
            Weather.CLEAR -> 1.0f
        }

        // Offroad detection
        val isOffroad = abs(player.x) > 1.0f
        val maxSpeedLimit = if (isOffroad) {
            65f // heavy offroad speed limit in km/h
        } else if (player.isNitroActive) {
            player.maxSpeed + 50f // nitro speed boost
        } else {
            player.maxSpeed
        }

        // Handle active nitro fuel timer
        if (player.isNitroActive) {
            player.nitroTimeRemaining -= dt
            if (player.nitroTimeRemaining <= 0f) {
                player.isNitroActive = false
                player.nitroTimeRemaining = 0f
            }
        }

        // Apply engine acceleration or braking
        if (player.isBraking) {
            player.speed -= player.accel * 4.5f * dt // hard braking
            if (player.speed < 0f) player.speed = 0f
        } else {
            // Natural acceleration up to speed limits
            if (player.speed < maxSpeedLimit) {
                player.speed += player.accel * dt
            } else {
                player.speed -= player.accel * 0.5f * dt // drag decelerating back down to speed limits
            }
        }

        // Steering logic
        val steeringSpeedFactor = min(1.0f, player.speed / 80f) // steering is tighter at speed
        player.x += player.steeringInput * steeringSpeedFactor * 0.04f * weatherGripFactor

        // Centrifugal curve force pulls car outward!
        val centrifugalPull = (player.speed / 150f) * roadCurve * 0.012f
        player.x -= centrifugalPull

        // Drifting system
        // Conditions: steering hard at > 110 km/h with either brake pedal or sudden sharp curves
        val isSteeringHard = abs(player.steeringInput) > 0.6f
        if (player.speed > 110f && isSteeringHard && (player.isBraking || weather == Weather.RAIN || abs(roadCurve) > 2.0f)) {
            player.isDrifting = true
            player.driftAngle = -player.steeringInput * 15f * weatherGripFactor
            val driftDist = (player.speed / 3600f) * dt * 1000f // meters
            player.totalDriftDistance += driftDist
            totalDriftDistanceThisRace += driftDist

            if (player.totalDriftDistance > 15f) {
                driftBonusNotification = "DRIFT BONUS: +${(player.totalDriftDistance / 10).roundToInt()} COINS"
            }
        } else {
            if (player.isDrifting) {
                // Recover from drift, payout drift bonus coins
                val bonusCoins = (player.totalDriftDistance / 10).roundToInt()
                if (bonusCoins > 0) {
                    player.coinsCollected += bonusCoins
                    totalCoinsCollectedThisRace += bonusCoins
                    driftBonusNotification = "DRIFT RECOVERY: +$bonusCoins COINS!"
                }
                player.totalDriftDistance = 0f
            }
            player.isDrifting = false
            player.driftAngle = 0f
        }

        // Move forward down the track
        // Speed converted from km/h to world coordinate units per second
        val speedWorldUnit = player.speed * 8f
        player.z += speedWorldUnit * dt

        // Track stats
        currentSpeedKmh = player.speed.roundToInt()
        if (player.speed > maxSpeedReachedThisRace) {
            maxSpeedReachedThisRace = player.speed
        }

        // Percentage completed
        player.trackCompletedPercent = min(1.0f, player.z / totalTrackLength)
    }

    private fun updateAICars(dt: Float) {
        for (ai in aiCars) {
            if (ai.finished) continue

            val aiSeg = getSegmentAt(ai.z)

            // Dynamic speed adjustments based on curves ahead
            val curveFactor = abs(aiSeg.curve)
            val aiTargetSpeed = if (curveFactor > 3f) ai.maxSpeed - 50f else ai.maxSpeed

            if (ai.speed < aiTargetSpeed) {
                ai.speed += ai.accel * dt
            } else {
                ai.speed -= ai.accel * dt
            }

            // Move AI forward
            ai.z += ai.speed * 8f * dt

            // Simple steering lane logic: stay on lanes and try to avoid other cars
            ai.changeLaneTimer -= dt
            if (ai.changeLaneTimer <= 0f) {
                val rand = (ai.z * 17 % 100) / 100f
                ai.targetLane = if (rand < 0.33f) -0.5f else if (rand < 0.66f) 0.5f else 0.0f
                ai.changeLaneTimer = 2.0f + rand * 3f
            }

            // Smooth glide steering
            ai.x += (ai.targetLane - ai.x) * 0.05f

            if (ai.z >= totalTrackLength) {
                ai.z = totalTrackLength
                ai.speed = 0f
                ai.finished = true
            }
        }
    }

    private fun updateTraffic(dt: Float) {
        for (tc in trafficCars) {
            // Traffic drives slow in fixed lanes
            tc.z += tc.speed * 8f * dt

            // If player has fully passed traffic by a large distance, wrap around/re-spawn ahead to maintain road density!
            if (player.z - tc.z > 1500f) {
                val randZ = player.z + 5000f + (tc.z * 13 % 1200f)
                val randLane = if ((tc.z * 7).roundToInt() % 2 == 0) -0.5f else 0.5f
                tc.z = randZ
                tc.x = randLane
            }
        }
    }

    private fun updatePoliceChase(dt: Float) {
        for (cop in policeCars) {
            // Police drives incredibly fast to chase player!
            val catchUpSpeed = player.speed + 45f
            val maxCopSpeed = max(220f, catchUpSpeed)

            if (cop.speed < maxCopSpeed) {
                cop.speed += cop.accel * dt
            } else {
                cop.speed -= cop.accel * dt
            }

            cop.z += cop.speed * 8f * dt

            // Siren flashing state
            isSirenFlashing = (totalTimeElapsed / 200) % 2 == 0L

            // Police targets player lane
            cop.x += (player.x - cop.x) * 0.07f

            // Calculate distance for warning sirens
            val distance = player.z - cop.z
            policeDistanceWarning = if (distance > 0f) {
                min(1.0f, 1500f / distance) // Siren volume/alert warning grows as police get closer!
            } else {
                1.0f // Cop is side-by-side or ahead
            }

            // Evaded / Escaped event: if police is falling behind by a large distance after player hits Nitro!
            if (cop.z > 0 && distance > 2200f) {
                policeEscapedCount++
                driftBonusNotification = "POLICE ESCAPED! XP +150"
                // respawn police back down again to restart loop!
                cop.z = player.z - 3000f
                cop.speed = player.speed
            }
        }
    }

    private fun checkCollisions() {
        val playerSegIndex = (player.z / trackConfig.segmentLength).toInt()
        if (playerSegIndex >= segments.size) return

        val segment = segments[playerSegIndex]

        // 1. Collision with TrackItems (Coins, Nitro, Repair, Obstacles)
        val iterator = segment.items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.isCollected) continue

            // Check offset distance between player and item
            if (abs(player.x - item.offset) < 0.22f) {
                item.isCollected = true

                when (item.type) {
                    TrackItemType.COIN -> {
                        player.coinsCollected += 10
                        totalCoinsCollectedThisRace += 10
                        driftBonusNotification = "COIN COLLECTED! +10"
                    }
                    TrackItemType.NITRO -> {
                        player.isNitroActive = true
                        player.nitroTimeRemaining = 4.0f // 4 seconds turbo burst
                        player.nitroCapacity = min(100f, player.nitroCapacity + 35f)
                        driftBonusNotification = "NITRO BURST TRIGGERED!"
                    }
                    TrackItemType.REPAIR -> {
                        player.repair(25f)
                        driftBonusNotification = "CAR REPAIRED +25%"
                    }
                    TrackItemType.ROCK, TrackItemType.NEON_SIGN -> {
                        player.takeDamage(20f)
                        player.speed = max(30f, player.speed - 50f) // slow down
                        driftBonusNotification = "CRASH! DAMAGE TAKEN!"
                    }
                    TrackItemType.CONE -> {
                        player.takeDamage(5f)
                        player.speed = max(60f, player.speed - 15f)
                        driftBonusNotification = "CONE HIT!"
                    }
                    TrackItemType.POLICE_BLOCK -> {
                        player.takeDamage(35f)
                        player.speed = 10f // Spikes flat tires immediate drag
                        driftBonusNotification = "SPIKE TRAP! FLAT TIRE!"
                    }
                }
            }
        }

        // 2. Collision with Traffic Cars
        for (tc in trafficCars) {
            val distanceZ = abs(player.z - tc.z)
            if (distanceZ < 150f) { // check proximity
                if (abs(player.x - tc.x) < 0.25f) {
                    // Collision!
                    player.takeDamage(25f)
                    player.speed = tc.speed * 0.7f // snap speed to traffic speed

                    // Bounce car away slightly to avoid continuous sticking
                    if (player.x > tc.x) player.x += 0.3f else player.x -= 0.3f
                    driftBonusNotification = "TRAFFIC COLLISION!"

                    // Push traffic ahead
                    tc.z += 300f
                }
            }
        }

        // 3. Collision with AI Opponents
        for (ai in aiCars) {
            val distanceZ = abs(player.z - ai.z)
            if (distanceZ < 150f) {
                if (abs(player.x - ai.x) < 0.25f) {
                    player.takeDamage(10f)
                    player.speed = max(80f, player.speed - 30f)
                    ai.speed = max(80f, ai.speed - 30f)

                    if (player.x > ai.x) player.x += 0.3f else player.x -= 0.3f
                    driftBonusNotification = "COMPETITOR CONTACT!"
                }
            }
        }

        // 4. Collision with Police Cars (in chase mode)
        for (cop in policeCars) {
            val distanceZ = abs(player.z - cop.z)
            if (distanceZ < 140f) {
                if (abs(player.x - cop.x) < 0.25f) {
                    if (player.isNitroActive) {
                        // Player is boosted and rams the police car out of the way!
                        cop.z -= 400f // bump police back
                        cop.speed = 80f
                        player.coinsCollected += 150 // Bounty for ramming cop
                        driftBonusNotification = "POLICE SHAKEN OFF! +150 COINS"
                    } else {
                        // Police ramming player!
                        player.takeDamage(20f)
                        player.speed = max(40f, player.speed - 45f)
                        driftBonusNotification = "POLICE RAMMED YOU!"
                        cop.z -= 150f
                    }
                }
            }
        }
    }

    private fun calculatePositions() {
        // Count how many AI racers are ahead of the player
        var aheadCount = 0
        for (ai in aiCars) {
            if (ai.z > player.z) aheadCount++
        }
        currentPositionRank = aheadCount + 1
    }

    private fun handleRaceCompletion(isVictory: Boolean) {
        isRunning = false

        // XP Math: Level + completions
        val baseXP = if (isVictory) 350 else 50
        val rankBonusXP = if (isVictory) {
            when (currentPositionRank) {
                1 -> 250
                2 -> 150
                3 -> 70
                else -> 20
            }
        } else 0
        val driftXP = (totalDriftDistanceThisRace / 20).roundToInt()
        val finalXPEarned = baseXP + rankBonusXP + driftXP

        // Coin calculation
        val coinsReward = player.coinsCollected + (if (isVictory && currentPositionRank == 1) 300 else if (isVictory) 100 else 20)

        // Track achievements / Completed missions check during race
        val completedMissions = ArrayList<String>()
        if (maxSpeedReachedThisRace >= 220f) {
            completedMissions.add("m_speed")
        }
        if (totalDriftDistanceThisRace >= 1000f) {
            completedMissions.add("m_drift")
        }
        if (totalCoinsCollectedThisRace >= 100) {
            completedMissions.add("m_coins")
        }
        if (isPoliceChaseMode && isVictory && player.currentHealth >= 70f) {
            completedMissions.add("m_police")
        }
        if (trackConfig.id == "track_city" && isVictory && totalTimeElapsed < 60000L) {
            completedMissions.add("m_perfect_run")
        }

        onRaceFinished(coinsReward, finalXPEarned, totalTimeElapsed, isVictory && currentPositionRank <= 3, completedMissions)
    }

    fun getSegmentAt(zPos: Float): RoadSegment {
        val index = (zPos / trackConfig.segmentLength).toInt()
        return segments[index % segments.size]
    }
}
