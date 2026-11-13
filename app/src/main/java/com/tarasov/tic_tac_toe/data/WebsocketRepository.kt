package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.ws.LobbyEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * События WebSocket-соединения в терминах приложения.
 */
sealed interface WebsocketEvent {

    data object Connected : WebsocketEvent

    /** Приветствие из @SubscribeMapping("/greeting") — приходит сразу после CONNECT. */
    data class Greeting(val text: String) : WebsocketEvent

    /** Событие лобби, полученное после подписки на /app/lobby. */
    data class LobbyEventReceived(val lobbyEvent: LobbyEvent) : WebsocketEvent

    data class Error(val message: String) : WebsocketEvent

    data object Disconnected : WebsocketEvent
}

/**
 * Приём сообщений по WebSocket (STOMP).
 *
 * Реализация подписывается на /app/greeting и /app/lobby сразу после успешного CONNECT.
 * Чтобы добавить другие каналы (например /topic/greetings или /user/queue/...),
 * достаточно подписаться на них в WebsocketRepositoryImpl после Connected.
 */
interface WebsocketRepository {

    val events: SharedFlow<WebsocketEvent>

    /** Подключается с JWT, сохранённым при логине. */
    fun connect()

    fun disconnect()
}
