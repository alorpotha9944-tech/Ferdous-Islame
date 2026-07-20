package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_cars")
data class UnlockedCar(
    @PrimaryKey val carId: String,
    val carName: String,
    val isUnlocked: Boolean = false,
    val engineLevel: Int = 1, // Max 5
    val brakesLevel: Int = 1,  // Max 5
    val turboLevel: Int = 1,   // Max 5
    val tiresLevel: Int = 1    // Max 5
) {
    // Stat multipliers based on upgrade levels
    fun getEnginePower(): Float = 1.0f + (engineLevel - 1) * 0.12f
    fun getBrakesEfficiency(): Float = 1.0f + (brakesLevel - 1) * 0.15f
    fun getTurboBoost(): Float = 1.0f + (turboLevel - 1) * 0.18f
    fun getTireGrip(): Float = 1.0f + (tiresLevel - 1) * 0.10f
}
