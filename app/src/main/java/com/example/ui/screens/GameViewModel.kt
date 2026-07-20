package com.example.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioSynth
import com.example.data.*
import com.example.game.GameEngine
import com.example.game.TrackConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GameDatabase.getDatabase(application)
    private val repository = GameRepository(database.gameDao())

    // Profile & Shop states
    val playerProfile: StateFlow<PlayerProfile?> = repository.playerProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val unlockedCars: StateFlow<List<UnlockedCar>> = repository.unlockedCars.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val missions: StateFlow<List<MissionEntity>> = repository.missions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Navigation states
    var currentScreen by mutableStateOf("splash") // splash, login, menu, garage, track_select, race, leaderboard, settings
    var previousScreen by mutableStateOf("menu")

    // Game Config Selection states
    var selectedTrackConfig by mutableStateOf<TrackConfig?>(null)
    var isPoliceChaseSelected by mutableStateOf(false)

    // Active Engine Runner
    var activeEngine by mutableStateOf<GameEngine?>(null)

    // Leveling helper
    fun xpNeededForNextLevel(currentLevel: Int): Int = currentLevel * 1000

    init {
        // Observe profile settings and sync audio toggles
        viewModelScope.launch {
            playerProfile.collectLatest { profile ->
                profile?.let {
                    GameAudioSynth.isSoundEnabled = it.soundEffectsEnabled
                    GameAudioSynth.isMusicEnabled = it.musicEnabled
                }
            }
        }
    }

    fun navigateTo(screen: String) {
        previousScreen = currentScreen
        currentScreen = screen
    }

    // Purchase Car
    fun buyCar(carId: String, price: Int) {
        val currentProfile = playerProfile.value ?: return
        if (currentProfile.coins >= price) {
            viewModelScope.launch {
                // Update car lock status
                val car = repository.getCarById(carId)
                if (car != null) {
                    repository.updateUnlockedCar(car.copy(isUnlocked = true))

                    // Deduct coins and update profile
                    repository.updatePlayerProfile(
                        currentProfile.copy(
                            coins = currentProfile.coins - price,
                            selectedCarId = carId
                        )
                    )
                    GameAudioSynth.playLevelUp()
                }
            }
        }
    }

    // Select Active Car
    fun selectCar(carId: String) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            repository.updatePlayerProfile(currentProfile.copy(selectedCarId = carId))
            GameAudioSynth.playCoin()
        }
    }

    // Upgrade Car System
    fun upgradeCarPart(carId: String, part: String) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            val car = repository.getCarById(carId) ?: return@launch
            val currentLevel = when (part) {
                "engine" -> car.engineLevel
                "brakes" -> car.brakesLevel
                "turbo" -> car.turboLevel
                "tires" -> car.tiresLevel
                else -> 1
            }

            if (currentLevel >= 5) return@launch // max level 5

            val cost = currentLevel * 800 // Upgrade cost scales with level

            if (currentProfile.coins >= cost) {
                val updatedCar = when (part) {
                    "engine" -> car.copy(engineLevel = currentLevel + 1)
                    "brakes" -> car.copy(brakesLevel = currentLevel + 1)
                    "turbo" -> car.copy(turboLevel = currentLevel + 1)
                    "tires" -> car.copy(tiresLevel = currentLevel + 1)
                    else -> car
                }

                repository.updateUnlockedCar(updatedCar)
                repository.updatePlayerProfile(currentProfile.copy(coins = currentProfile.coins - cost))
                GameAudioSynth.playLevelUp()
            }
        }
    }

    // Daily Reward Claim
    fun claimDailyReward() {
        val currentProfile = playerProfile.value ?: return
        val now = System.currentTimeMillis()
        val coolingTimeMs = 24 * 60 * 60 * 1000 // 24 hours

        if (now - currentProfile.lastDailyRewardClaimed >= coolingTimeMs) {
            viewModelScope.launch {
                repository.updatePlayerProfile(
                    currentProfile.copy(
                        coins = currentProfile.coins + 250,
                        lastDailyRewardClaimed = now
                    )
                )
                GameAudioSynth.playLevelUp()
            }
        }
    }

    // Save Track High Time
    fun saveTrackTime(trackId: String, timeMs: Long) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            val updatedProfile = when (trackId) {
                "track_city" -> {
                    if (currentProfile.bestTimeTrack1 == 0L || timeMs < currentProfile.bestTimeTrack1) {
                        currentProfile.copy(bestTimeTrack1 = timeMs)
                    } else currentProfile
                }
                "track_desert" -> {
                    if (currentProfile.bestTimeTrack2 == 0L || timeMs < currentProfile.bestTimeTrack2) {
                        currentProfile.copy(bestTimeTrack2 = timeMs)
                    } else currentProfile
                }
                "track_midnight" -> {
                    if (currentProfile.bestTimeTrack3 == 0L || timeMs < currentProfile.bestTimeTrack3) {
                        currentProfile.copy(bestTimeTrack3 = timeMs)
                    } else currentProfile
                }
                else -> currentProfile
            }

            if (updatedProfile != currentProfile) {
                repository.updatePlayerProfile(updatedProfile)
            }
        }
    }

    // Generic player profile update
    fun updatePlayerProfile(profile: PlayerProfile) {
        viewModelScope.launch {
            repository.updatePlayerProfile(profile)
        }
    }

    // Claim Mission Reward
    fun claimMissionReward(missionId: String) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            val missionsList = missions.value
            val mission = missionsList.find { it.missionId == missionId }

            if (mission != null && mission.isCompleted && !mission.isRewardClaimed) {
                // Award coins and mark as claimed
                repository.updateMission(mission.copy(isRewardClaimed = true))
                repository.updatePlayerProfile(currentProfile.copy(coins = currentProfile.coins + mission.rewardCoins))
                GameAudioSynth.playLevelUp()
            }
        }
    }

    // Handle Post-Race Reward Updates and level progressions
    fun finalizeRace(coinsWon: Int, xpEarned: Int, completionTimeMs: Long, isVictory: Boolean, completedMissions: List<String>) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            // Update active profile: Coins & XP
            var newXP = currentProfile.xp + xpEarned
            var newLevel = currentProfile.level

            // Level up checks!
            while (newXP >= xpNeededForNextLevel(newLevel)) {
                newXP -= xpNeededForNextLevel(newLevel)
                newLevel++
                // play Level Up fanfare chime
                GameAudioSynth.playLevelUp()
            }

            val updatedProfile = currentProfile.copy(
                coins = currentProfile.coins + coinsWon,
                xp = newXP,
                level = newLevel
            )
            repository.updatePlayerProfile(updatedProfile)

            // Save High Score lap/race time
            if (isVictory && selectedTrackConfig != null) {
                saveTrackTime(selectedTrackConfig!!.id, completionTimeMs)
            }

            // Flag and complete relevant missions
            val loadedMissions = missions.value
            for (mId in completedMissions) {
                val mission = loadedMissions.find { it.missionId == mId }
                if (mission != null && !mission.isCompleted) {
                    repository.updateMission(mission.copy(isCompleted = true, progress = 1.0f))
                }
            }
        }
    }

    // Settings adjustments
    fun updateControlPreferences(controlType: Int) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            repository.updatePlayerProfile(currentProfile.copy(controlType = controlType))
        }
    }

    fun toggleSFX(enabled: Boolean) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            repository.updatePlayerProfile(currentProfile.copy(soundEffectsEnabled = enabled))
        }
    }

    fun toggleMusic(enabled: Boolean) {
        val currentProfile = playerProfile.value ?: return
        viewModelScope.launch {
            repository.updatePlayerProfile(currentProfile.copy(musicEnabled = enabled))
        }
    }
}
