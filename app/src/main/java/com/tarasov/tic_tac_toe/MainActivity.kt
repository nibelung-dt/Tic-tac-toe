package com.tarasov.tic_tac_toe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tarasov.tic_tac_toe.navigation.Routes
import com.tarasov.tic_tac_toe.navigation.authGraph
import com.tarasov.tic_tac_toe.navigation.lobbyGraph
import com.tarasov.tic_tac_toe.ui.theme.TictactoeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TictactoeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.LOGIN,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        authGraph(
                            onAuthenticated = {
                                navController.navigate(Routes.LOBBY) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                        lobbyGraph()
                    }
                }
            }
        }
    }
}