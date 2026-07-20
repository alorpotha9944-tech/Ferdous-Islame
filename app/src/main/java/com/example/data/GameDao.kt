package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // Player Profile
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getPlayerProfileDirect(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProfile(profile: PlayerProfile)

    // Unlocked Cars
    @Query("SELECT * FROM unlocked_cars")
    fun getAllUnlockedCars(): Flow<List<UnlockedCar>>

    @Query("SELECT * FROM unlocked_cars WHERE carId = :carId LIMIT 1")
    suspend fun getCarById(carId: String): UnlockedCar?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnlockedCar(car: UnlockedCar)

    @Update
    suspend fun updateUnlockedCar(car: UnlockedCar)

    // Missions
    @Query("SELECT * FROM missions")
    fun getAllMissions(): Flow<List<MissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)

    @Update
    suspend fun updateMission(mission: MissionEntity)
}
