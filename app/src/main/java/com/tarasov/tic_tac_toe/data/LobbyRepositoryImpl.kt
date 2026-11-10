package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.LobbyApi

class LobbyRepositoryImpl(
    private val api: LobbyApi
) : LobbyRepository {

    override suspend fun getActiveUsers(): ApiResult<List<User>> =
        safeApiCall { api.getActiveUsers() }

    override suspend fun inviteToGame(id: Long): ApiResult<Unit> = safeApiCall {
        // api.inviteToGame(id)
    }
}