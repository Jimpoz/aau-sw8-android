package com.example.aauapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.aauapp.ui.theme.AAUAppTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val userSessionViewModel: UserSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val sessionState by userSessionViewModel.uiState.collectAsState()

            AAUAppTheme {
                if (sessionState.isLoggedIn) {
                    MainScreen(userSessionViewModel = userSessionViewModel)
                } else {
                    LoginScreen(viewModel = userSessionViewModel)
                }
            }
        }
    }
}
