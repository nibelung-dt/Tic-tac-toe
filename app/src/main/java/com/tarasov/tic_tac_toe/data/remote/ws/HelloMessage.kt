package com.tarasov.tic_tac_toe.data.remote.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Сообщение с именем пользователя. */
@Serializable
data class HelloMessage(
    @SerialName("name") val name: String? = null,
)
