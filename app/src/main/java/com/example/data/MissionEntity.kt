package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val missionId: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val isCompleted: Boolean = false,
    val progress: Float = 0f, // 0.0f to 1.0f
    val isRewardClaimed: Boolean = false
)
