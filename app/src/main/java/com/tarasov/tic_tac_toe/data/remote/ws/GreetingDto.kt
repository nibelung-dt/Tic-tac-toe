package com.tarasov.tic_tac_toe.data.remote.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ бэкенда `Greeting`.
 *
 * Точное имя поля в Java-классе нам неизвестно (content/message/text/greeting),
 * поэтому читаем все варианты: неизвестные ключи Json игнорирует.
 * Если не совпало ничего — репозиторий покажет сырое тело, а не потеряет сообщение.
 */
@Serializable
data class GreetingDto(
    @SerialName("content") val content: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("greeting") val greeting: String? = null,
) {
    val displayText: String?
        get() = content ?: message ?: text ?: greeting
}
