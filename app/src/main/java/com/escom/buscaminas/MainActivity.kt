package com.escom.buscaminas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.escom.buscaminas.data.UserPreferencesRepository
import com.escom.buscaminas.ui.screens.GameScreen
import com.escom.buscaminas.ui.screens.MainScreen
import com.escom.buscaminas.ui.theme.BuscaminasTheme
import com.escom.buscaminas.ui.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPreferencesRepository = UserPreferencesRepository(this)

        setContent {
            val isDarkMode by userPreferencesRepository.isDarkMode.collectAsState(initial = isSystemInDarkTheme())

            BuscaminasTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val gameViewModel: GameViewModel = viewModel()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            navController = navController,
                            userPreferencesRepository = userPreferencesRepository,
                            isDarkMode = isDarkMode
                        )
                    }
                    composable("game") {
                        GameScreen(
                            navController = navController,
                            viewModel = gameViewModel
                        )
                    }
                }
            }
        }
    }
}