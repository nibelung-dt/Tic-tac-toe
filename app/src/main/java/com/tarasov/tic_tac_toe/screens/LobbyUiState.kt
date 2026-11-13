package com.tarasov.tic_tac_toe.screens

import com.tarasov.tic_tac_toe.data.User

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