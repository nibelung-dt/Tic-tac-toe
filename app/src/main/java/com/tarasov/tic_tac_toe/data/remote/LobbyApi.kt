package com.tarasov.tic_tac_toe.data.remote

import com.tarasov.tic_tac_toe.data.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET


@Serializable
data class LobbyResponse(
    @SerialName("token") val token: String? = null,
    @SerialName("message") val message: String? = null,
)

interface LobbyApi {
    @GET("lobby/getActiveUsers")
    suspend fun getActiveUsers(): List<User>

    // @POST("lobby/invite")
    // suspend fun inviteToGame(@Body id: Long)
}