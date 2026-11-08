package com.tarasov.tic_tac_toe.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tarasov.tic_tac_toe.screens.LobbyRoute
import com.tarasov.tic_tac_toe.screens.LoginRoute

/**
 * Единый реестр маршрутов. Строки-константы вместо магии в местах вызова.
 */
object Routes {
    const val LOGIN = "login"
    const val LOBBY = "lobby"
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

fun NavGraphBuilder.lobbyGraph() {
    composable(Routes.LOBBY) {
        LobbyRoute()
    }
}
