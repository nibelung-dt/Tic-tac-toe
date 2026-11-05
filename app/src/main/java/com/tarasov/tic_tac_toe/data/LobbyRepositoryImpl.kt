package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.LobbyApi

class LobbyRepositoryImpl(
    private val api: LobbyApi
): LobbyRepository {
    override suspend fun getListGamers(): List<Gamer> {
        TODO("Not yet implemented")
    }
}