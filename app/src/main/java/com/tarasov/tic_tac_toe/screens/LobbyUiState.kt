package com.tarasov.tic_tac_toe.screens

import android.util.Log
import com.tarasov.tic_tac_toe.data.User
import com.tarasov.tic_tac_toe.data.remote.ws.LobbyEvent

data class LobbyUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** true, если сервер ответил 401 и сессия больше не действительна. */
    val isSessionExpired: Boolean = false,
    /** Приветствие, которое сервер присылает по WebSocket сразу после авторизации. */
    val greeting: String? = null,
    /** Ошибка WebSocket-соединения (не влияет на HTTP-часть экрана). */
    val socketError: String? = null,
)

/**
 * Применяет событие лобби к текущему списку онлайн-пользователей.
 *
 * Идентификатор — [User.id]: два разных аккаунта могут называться одинаково.
 */
internal fun LobbyUiState.applyLobbyEvent(event: LobbyEvent): LobbyUiState {
    Log.d(
        "MY_LOG",
        "LobbyEvent: type=${event.type}, user=${event.user?.username}, users=${event.users?.size}",
    )

    val updatedUsers = when (event.type) {
        LobbyEvent.Type.SNAPSHOT -> event.users.orEmpty()

        LobbyEvent.Type.USER_ONLINE -> {
            val user = event.user ?: return this
            users.filterNot { it.id == user.id } + user
        }

        LobbyEvent.Type.USER_OFFLINE -> {
            val user = event.user ?: return this
            users.filterNot { it.id == user.id }
        }
    }

    return copy(
        users = updatedUsers,
        isLoading = false,
        errorMessage = null,
        socketError = null,
    )
}