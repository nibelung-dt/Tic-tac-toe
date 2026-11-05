package com.tarasov.tic_tac_toe.data

interface LobbyRepository {
    suspend fun getListGamers(): List<Gamer>
}