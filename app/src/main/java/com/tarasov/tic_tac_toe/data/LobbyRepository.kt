package com.tarasov.tic_tac_toe.data

import retrofit2.http.GET
import retrofit2.http.POST

interface LobbyRepository {
    @GET("lobby/get_list_gamers")
    suspend fun getListGamers(): List<Gamer>

    @POST("lobby/invite")
    suspend fun inviteToGame(id: Long)
}