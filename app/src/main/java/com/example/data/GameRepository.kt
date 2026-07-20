package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameRepository(private val gameDao: GameDao) {

    val playerProfile: Flow<PlayerProfile?> = gameDao.getPlayerProfile()
    val unlockedCars: Flow<List<UnlockedCar>> = gameDao.getAllUnlockedCars()
    val missions: Flow<List<MissionEntity>> = gameDao.getAllMissions()

    init {
        // Run database check in background coroutine to avoid thread blocking
        CoroutineScope(Dispatchers.IO).launch {
            initializeDatabaseIfEmpty()
        }
    }

    private suspend fun initializeDatabaseIfEmpty() {
        // 1. Initial Profile
        val currentProfile = gameDao.getPlayerProfileDirect()
        if (currentProfile == null) {
            gameDao.insertPlayerProfile(PlayerProfile())
        }

        // 2. Initial Cars
        val cars = gameDao.getAllUnlockedCars().firstOrNull() ?: emptyList()
        if (cars.isEmpty()) {
            val defaultCars = listOf(
                UnlockedCar(carId = "car_comet", carName = "Retro Comet", isUnlocked = true),
                UnlockedCar(carId = "car_neon_gt", carName = "Neon Midnight GT", isUnlocked = false),
                UnlockedCar(carId = "car_desert_rogue", carName = "Desert Rogue V8", isUnlocked = false),
                UnlockedCar(carId = "car_apex_concept", carName = "Apex Concept", isUnlocked = false)
            )
            for (car in defaultCars) {
                gameDao.insertUnlockedCar(car)
            }
        }

        // 3. Initial Missions
        val loadedMissions = gameDao.getAllMissions().firstOrNull() ?: emptyList()
        if (loadedMissions.isEmpty()) {
            val defaultMissions = listOf(
                MissionEntity(
                    missionId = "m_speed",
                    title = "Speed Demon",
                    description = "Reach 220 km/h in Neon Midnight or any track.",
                    rewardCoins = 400,
                    progress = 0f
                ),
                MissionEntity(
                    missionId = "m_drift",
                    title = "Drift Master",
                    description = "Drift for a total of 1,000 meters.",
                    rewardCoins = 500,
                    progress = 0f
                ),
                MissionEntity(
                    missionId = "m_coins",
                    title = "Coin Collector",
                    description = "Collect 100 coins in a single race.",
                    rewardCoins = 300,
                    progress = 0f
                ),
                MissionEntity(
                    missionId = "m_police",
                    title = "Outlaw",
                    description = "Survive a Police Chase mode run with under 30% damage.",
                    rewardCoins = 600,
                    progress = 0f
                ),
                MissionEntity(
                    missionId = "m_perfect_run",
                    title = "Asphalt Legend",
                    description = "Complete City Center in under 60 seconds.",
                    rewardCoins = 1000,
                    progress = 0f
                )
            )
            gameDao.insertMissions(defaultMissions)
        }
    }

    suspend fun updatePlayerProfile(profile: PlayerProfile) {
        gameDao.insertPlayerProfile(profile)
    }

    suspend fun updateUnlockedCar(car: UnlockedCar) {
        gameDao.updateUnlockedCar(car)
    }

    suspend fun insertUnlockedCar(car: UnlockedCar) {
        gameDao.insertUnlockedCar(car)
    }

    suspend fun updateMission(mission: MissionEntity) {
        gameDao.updateMission(mission)
    }

    suspend fun getCarById(carId: String): UnlockedCar? {
        return gameDao.getCarById(carId)
    }
}
