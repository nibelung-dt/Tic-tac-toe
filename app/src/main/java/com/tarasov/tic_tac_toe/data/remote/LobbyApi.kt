package com.tarasov.tic_tac_toe.data.remote

import com.tarasov.tic_tac_toe.data.Gamer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET


@Serializable
data class LobbyResponse( //TODO доработать
    @SerialName("token") val token: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("message") val message: String? = null,
)

interface LobbyApi {
    @GET("lobby/getActiveUsers")
    suspend fun getActiveUsers(): List<Gamer>
    // @POST("lobby/invite")
    // suspend fun inviteToGame(@Body id: Long)
}