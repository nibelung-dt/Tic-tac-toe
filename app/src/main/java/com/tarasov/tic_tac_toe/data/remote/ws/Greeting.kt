package com.tarasov.tic_tac_toe.data.remote.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Сообщение приветствия, которое отправляет Spring-бэкенд. */
@Serializable
data class Greeting(
    @SerialName("content") val content: String? = null,
)
