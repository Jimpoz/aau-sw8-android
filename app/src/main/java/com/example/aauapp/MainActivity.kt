package com.example.aauapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.aauapp.ui.theme.AAUAppTheme

class MainActivity : ComponentActivity() {

    private val appContainer by lazy {
        (application as AAUAppApplication).appContainer
    }

    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModelFactory(appContainer.themePreferencesStore)
    }

    private val userSessionViewModel: UserSessionViewModel by viewModels {
        UserSessionViewModelFactory(appContainer.userSessionStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val sessionState by userSessionViewModel.uiState.collectAsState()

            AAUAppTheme(darkTheme = themeViewModel.isDarkMode) {
                when {
                    sessionState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    sessionState.isLoggedIn -> {
                        MainScreen(
                            isDarkMode = themeViewModel.isDarkMode,
                            onDarkModeChange = { themeViewModel.setDarkMode(it) },
                            userSessionViewModel = userSessionViewModel
                        )
                    }

                    else -> {
                        LoginScreen(
                            viewModel = userSessionViewModel
                        )
                    }
                }
            }
        }
    }
}