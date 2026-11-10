package com.tarasov.tic_tac_toe.data

interface LobbyRepository {
    suspend fun getActiveUsers(): ApiResult<List<User>>

    suspend fun inviteToGame(id: Long): ApiResult<Unit>
}
