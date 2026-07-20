package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "Apex Racer",
    val avatarId: Int = 0, // index of avatar
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 1500, // Generous starting cash
    val selectedCarId: String = "car_comet",
    val bestTimeTrack1: Long = 0L, // City Center best time in ms
    val bestTimeTrack2: Long = 0L, // Desert Highway best time in ms
    val bestTimeTrack3: Long = 0L, // Neon Midnight best time in ms
    val controlType: Int = 0, // 0 = D-Pad, 1 = Steering Wheel
    val soundEffectsEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val lastDailyRewardClaimed: Long = 0L // UTC timestamp
)
