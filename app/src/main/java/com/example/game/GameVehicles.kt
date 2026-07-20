package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

enum class VehicleColor {
    RED,
    BLUE,
    YELLOW,
    ORANGE,
    PURPLE,
    POLICE
}

open class GameVehicle(
    var id: String,
    var name: String,
    var speed: Float = 0f, // current speed
    var maxSpeed: Float = 180f, // base max speed in km/h
    var accel: Float = 5f, // base acceleration rate
    var x: Float = 0f, // road offset (-2.0f to 2.0f, 0 = center)
    var z: Float = 0f, // world z distance
    val vehicleColor: VehicleColor = VehicleColor.RED
) {
    fun getColor(): Color {
        return when (vehicleColor) {
            VehicleColor.RED -> Color(0xFFD32F2F)
            VehicleColor.BLUE -> Color(0xFF1976D2)
            VehicleColor.YELLOW -> Color(0xFFFBC02D)
            VehicleColor.ORANGE -> Color(0xFFF57C00)
            VehicleColor.PURPLE -> Color(0xFF7B1FA2)
            VehicleColor.POLICE -> Color(0xFF212121)
        }
    }
}

class PlayerCar(
    id: String = "player",
    name: String = "Retro Comet",
    var currentHealth: Float = 100f, // 0 to 100
    var nitroCapacity: Float = 100f, // 0 to 100
    var isNitroActive: Boolean = false,
    var nitroTimeRemaining: Float = 0f, // in seconds
    var isDrifting: Boolean = false,
    var driftAngle: Float = 0f,
    var totalDriftDistance: Float = 0f,
    var isBraking: Boolean = false,
    var steeringInput: Float = 0f, // -1f (left) to +1f (right)
    var coinsCollected: Int = 0,
    var trackCompletedPercent: Float = 0f
) : GameVehicle(id, name, maxSpeed = 200f, accel = 8f) {

    fun reset() {
        speed = 0f
        x = 0f
        z = 0f
        currentHealth = 100f
        nitroCapacity = 40f // start with partial nitro
        isNitroActive = false
        nitroTimeRemaining = 0f
        isDrifting = false
        driftAngle = 0f
        totalDriftDistance = 0f
        isBraking = false
        steeringInput = 0f
        coinsCollected = 0
        trackCompletedPercent = 0f
    }

    fun applyUpgrades(engineLevel: Int, brakesLevel: Int, turboLevel: Int, tiresLevel: Int) {
        // Boost performance variables based on level 1 to 5
        maxSpeed = 200f + (engineLevel - 1) * 15f
        accel = 8f + (turboLevel - 1) * 1.5f
    }

    fun takeDamage(amount: Float) {
        currentHealth = max(0f, currentHealth - amount)
    }

    fun repair(amount: Float) {
        currentHealth = min(100f, currentHealth + amount)
    }
}

class AICar(
    id: String,
    name: String,
    maxSpeed: Float,
    accel: Float,
    vehicleColor: VehicleColor,
    var targetLane: Float = 0f,
    var finished: Boolean = false
) : GameVehicle(id, name, maxSpeed = maxSpeed, accel = accel, vehicleColor = vehicleColor) {
    // Basic AI steering state
    var changeLaneTimer: Float = 0f
}

class TrafficCar(
    id: String,
    x: Float,
    z: Float,
    speed: Float = 80f,
    vehicleColor: VehicleColor = VehicleColor.YELLOW
) : GameVehicle(id, "Traffic", speed = speed, maxSpeed = speed, x = x, z = z, vehicleColor = vehicleColor)
