package com.tarasov.tic_tac_toe.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Пользователь в лобби.
 *
 * Бэкенд присылает `{"userId": 1, "username": "denis"}`, поэтому
 * [id] привязан к `userId`: без @SerialName Kotlin будет искать "id"
 * и разбор упадёт.
 *
 * [id] — PK из БД, стабильный ключ. Именно по нему выбираем противника
 * и сопоставляем USER_ONLINE / USER_OFFLINE, а [username] может смениться.
 */
@Serializable
data class User(
    @SerialName("userId") val id: Long,
    @SerialName("username") val username: String,
)