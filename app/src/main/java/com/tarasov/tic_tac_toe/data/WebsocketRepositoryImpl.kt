package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.ws.GreetingDto
import com.tarasov.tic_tac_toe.data.remote.ws.StompClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WebsocketRepositoryImpl(
    private val stompClient: StompClient,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : WebsocketRepository {

    /**
     * Живёт столько же, сколько синглтон в Koin. Слушает фреймы один раз
     * и раздаёт уже доменные события подписчикам (ViewModel).
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<WebsocketEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<WebsocketEvent> = _events.asSharedFlow()

    init {
        scope.launch {
            stompClient.events.collect { event ->
                handleClientEvent(event)
            }
        }
    }

    override fun connect() {
        val token = tokenStore.token
        if (token.isNullOrBlank()) {
            _events.tryEmit(WebsocketEvent.Error("Нет токена авторизации"))
            return
        }
        stompClient.connect(token)
    }

    override fun disconnect() {
        stompClient.disconnect()
    }

    private fun handleClientEvent(event: StompClient.Event) {
        when (event) {
            StompClient.Event.Connected -> {
                _events.tryEmit(WebsocketEvent.Connected)

                // @SubscribeMapping("/greeting"): сервер отвечает прямо в эту сессию,
                // через брокер сообщение не идёт — подписки на /topic не нужны.
                stompClient.subscribe(GREETING_DESTINATION)
            }

            is StompClient.Event.Message -> {
                if (event.destination == GREETING_DESTINATION) {
                    _events.tryEmit(WebsocketEvent.Greeting(decodeGreeting(event.body)))
                }
            }

            is StompClient.Event.Failure -> _events.tryEmit(
                WebsocketEvent.Error(event.reason ?: "Ошибка WebSocket"),
            )

            StompClient.Event.Disconnected -> _events.tryEmit(WebsocketEvent.Disconnected)
        }
    }

    /**
     * Бэкенд присылает JSON вида {"content": "Hello, ..."}.
     * Если поле названо иначе — показываем сырое тело, чтобы сообщение не пропало.
     */
    private fun decodeGreeting(body: String): String =
        runCatching { json.decodeFromString(GreetingDto.serializer(), body) }
            .getOrNull()
            ?.displayText
            ?: body

    private companion object {
        const val GREETING_DESTINATION = "/app/greeting"
    }
}
