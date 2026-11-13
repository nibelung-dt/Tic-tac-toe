package com.tarasov.tic_tac_toe.data.remote.ws

import com.tarasov.tic_tac_toe.data.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Событие лобби.
 *
 * SNAPSHOT — полный список онлайн-пользователей в [users].
 * USER_ONLINE — пользователь зашёл в сеть, данные находятся в [user].
 * USER_OFFLINE — пользователь вышел из сети, данные находятся в [user].
 */
@Serializable
data class LobbyEvent(
    @SerialName("type") val type: Type,
    @SerialName("user") val user: User? = null,
    @SerialName("users") val users: List<User>? = null,
) {
    @Serializable
    enum class Type {
        @SerialName("SNAPSHOT") SNAPSHOT,
        @SerialName("USER_ONLINE") USER_ONLINE,
        @SerialName("USER_OFFLINE") USER_OFFLINE,
    }
}
