package com.tarasov.tic_tac_toe.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tarasov.tic_tac_toe.screens.LobbyRoute
import com.tarasov.tic_tac_toe.screens.LoginRoute
import com.tarasov.tic_tac_toe.screens.gameScreen.GameScreen

/**
 * Единый реестр маршрутов. Строки-константы вместо магии в местах вызова.
 */
object Routes {
    const val LOGIN = "login"
    const val LOBBY = "lobby"
    const val GAME = "game"
}

fun NavGraphBuilder.authGraph(
    onAuthenticated: () -> Unit,
) {
    composable(Routes.LOGIN) {
        LoginRoute(
            onAuthenticated = onAuthenticated,
        )
    }
}

fun NavGraphBuilder.lobbyGraph(
    onEnterToGame: () -> Unit,
) {
    composable(Routes.LOBBY) {
        LobbyRoute(onEnterToGame = onEnterToGame)
    }
    composable(Routes.GAME) {
        GameScreen()
    }
}
