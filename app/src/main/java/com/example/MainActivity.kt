package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // Crossfade provides smooth fade transitions between menu viewports
    Crossfade(
        targetState = viewModel.currentScreen,
        label = "screen_nav_crossfade",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            "splash" -> {
                SplashScreen(
                    onLoadingFinished = {
                        viewModel.navigateTo("login")
                    }
                )
            }
            "login" -> {
                LoginScreen(
                    viewModel = viewModel,
                    onEnterGame = {
                        viewModel.navigateTo("menu")
                    }
                )
            }
            "menu" -> {
                DashboardScreen(viewModel = viewModel)
            }
            "track_select" -> {
                TrackSelectorScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.navigateTo("menu")
                    }
                )
            }
            "garage" -> {
                GarageScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.navigateTo("menu")
                    }
                )
            }
            "leaderboard" -> {
                LeaderboardsScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.navigateTo("menu")
                    }
                )
            }
            "settings" -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.navigateTo("menu")
                    }
                )
            }
            "race" -> {
                RaceScreen(
                    viewModel = viewModel,
                    onBackToMenu = {
                        viewModel.navigateTo("menu")
                    }
                )
            }
        }
    }
}
