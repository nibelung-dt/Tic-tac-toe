package com.tarasov.tic_tac_toe.screens

import com.tarasov.tic_tac_toe.data.User
import com.tarasov.tic_tac_toe.data.remote.ws.LobbyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class LobbyUiStateTest {

    private val firstUser = User(username = "first")
    private val secondUser = User(username = "second")

    @Test
    fun `snapshot replaces users and clears errors`() {
        val state = LobbyUiState(
            users = listOf(firstUser),
            isLoading = true,
            errorMessage = "HTTP error",
            socketError = "Socket error",
        )
        val event = LobbyEvent(
            type = LobbyEvent.Type.SNAPSHOT,
            users = listOf(secondUser),
        )

        val result = state.applyLobbyEvent(event)

        assertEquals(listOf(secondUser), result.users)
        assertEquals(false, result.isLoading)
        assertEquals(null, result.errorMessage)
        assertEquals(null, result.socketError)
    }

    @Test
    fun `user online adds user`() {
        val state = LobbyUiState(users = listOf(firstUser))
        val event = LobbyEvent(type = LobbyEvent.Type.USER_ONLINE, user = secondUser)

        val result = state.applyLobbyEvent(event)

        assertEquals(listOf(firstUser, secondUser), result.users)
    }

    @Test
    fun `user online does not duplicate existing user`() {
        val state = LobbyUiState(users = listOf(firstUser, secondUser))
        val event = LobbyEvent(type = LobbyEvent.Type.USER_ONLINE, user = firstUser)

        val result = state.applyLobbyEvent(event)

        assertEquals(listOf(secondUser, firstUser), result.users)
    }

    @Test
    fun `user offline removes user`() {
        val state = LobbyUiState(users = listOf(firstUser, secondUser))
        val event = LobbyEvent(type = LobbyEvent.Type.USER_OFFLINE, user = firstUser)

        val result = state.applyLobbyEvent(event)

        assertEquals(listOf(secondUser), result.users)
    }

    @Test
    fun `user event without user keeps state unchanged`() {
        val state = LobbyUiState(users = listOf(firstUser))
        val event = LobbyEvent(type = LobbyEvent.Type.USER_ONLINE)

        assertEquals(state, state.applyLobbyEvent(event))
    }
}
