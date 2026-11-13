package com.tarasov.tic_tac_toe.data.remote.ws

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicLong


/**
 * Минимальный STOMP 1.2 клиент поверх OkHttp WebSocket.
 *
 * Умеет ровно то, что нужно приложению: CONNECT с JWT, SUBSCRIBE, SEND, DISCONNECT
 * и разбор входящих MESSAGE/ERROR. Heartbeat отключён (`heart-beat:0,0`),
 * живость TCP-соединения поддерживает ping OkHttp.
 *
 * События отдаются через [events] — их слушает репозиторий и превращает
 * в доменные события.
 */
class StompClient(
    private val okHttpClient: OkHttpClient,
    private val url: String,
) {

    sealed interface Event {
        data object Connected : Event

        data class Message(
            val destination: String,
            val subscriptionId: String?,
            val body: String,
        ) : Event

        data class Failure(val reason: String?, val cause: Throwable? = null) : Event

        data object Disconnected : Event
    }

    private val eventsChannel = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = eventsChannel.receiveAsFlow()

    private val subscriptionIds = AtomicLong(0)
    private val pendingFrames = ArrayDeque<String>()
    private val pendingLock = Any()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var jwt: String? = null

    @Volatile
    private var isConnected = false

    /**
     * Открывает WebSocket и сразу отправляет STOMP CONNECT с JWT.
     * Токен читает JwtChannelInterceptor на бэке и выставляет Principal сессии.
     */
    fun connect(jwt: String) {
        this.jwt = jwt
        Log.d(TAG, "Connecting to $url")

        synchronized(pendingLock) {
            pendingFrames.clear()
            isConnected = false
        }

        // Сначала «забываем» старое соединение, чтобы его колбэки
        // не сбросили состояние уже нового.
        val previous = webSocket
        webSocket = null
        previous?.cancel()

        webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(url).build(),
            Listener(),
        )
    }

    /**
     * Подписка на destination. Для @SubscribeMapping достаточно подписаться
     * на /app/... — ответ придёт в эту же сессию без участия брокера.
     * @return id подписки (сервер вернёт его в заголовке subscription).
     */
    fun subscribe(destination: String): String {
        val id = "sub-${subscriptionIds.incrementAndGet()}"
        Log.d(TAG, "SUBSCRIBE $destination (id=$id)")
        sendFrame(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to id,
                "destination" to destination,
                "ack" to "auto",
            ),
        )
        return id
    }

    /**
     * Отправка сообщения в @MessageMapping (например, /app/hello).
     */
    fun send(destination: String, body: String, contentType: String = "application/json") {
        Log.d(TAG, "SEND $destination: $body")
        sendFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to destination,
                "content-type" to contentType,
                "content-length" to body.toByteArray(Charsets.UTF_8).size.toString(),
            ),
            body = body,
        )
    }

    fun disconnect() {
        val socket = webSocket ?: return
        Log.d(TAG, "Disconnecting")

        if (isConnected) {
            isConnected = false
            // DISCONNECT отправляем напрямую: sendFrame() смотрит на webSocket.
            socket.send(buildFrame(command = "DISCONNECT", headers = emptyMap(), body = ""))
        }

        webSocket = null
        socket.close(NORMAL_CLOSURE, "client disconnect")
    }

    // region Отправка

    private fun sendFrame(
        command: String,
        headers: Map<String, String>,
        body: String = "",
    ) {
        val frame = buildFrame(command, headers, body)

        synchronized(pendingLock) {
            if (!isConnected) {
                // Подписка могла быть вызвана до CONNECTED — не теряем её.
                if (webSocket != null) pendingFrames.addLast(frame)
                return
            }
        }

        webSocket?.send(frame)
    }

    private fun buildFrame(
        command: String,
        headers: Map<String, String>,
        body: String,
    ): String = buildString {
        append(command).append('\n')
        headers.forEach { (name, value) ->
            append(escapeHeader(name)).append(':').append(escapeHeader(value)).append('\n')
        }
        append('\n')
        append(body)
        append('\u0000')
    }

    private fun escapeHeader(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                ':' -> append("\\c")
                else -> append(char)
            }
        }
    }

    // endregion

    private fun onConnected() {
        synchronized(pendingLock) {
            isConnected = true
            while (pendingFrames.isNotEmpty()) {
                webSocket?.send(pendingFrames.removeFirst())
            }
        }
        Log.d(TAG, "STOMP CONNECTED")
        eventsChannel.trySend(Event.Connected)
    }

    /** Колбэки старого (уже заменённого) соединения игнорируем. */
    private fun isCurrent(socket: WebSocket): Boolean = webSocket === socket

    private inner class Listener : WebSocketListener() {

        private val buffer = StringBuilder()

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!isCurrent(webSocket)) return

            val token = jwt
            if (token.isNullOrBlank()) {
                Log.e(TAG, "No JWT for WebSocket CONNECT")
                eventsChannel.trySend(Event.Failure("Нет JWT для подключения к WebSocket"))
                webSocket.close(POLICY_VIOLATION, "no token")
                return
            }

            // CONNECT отправляем напрямую: соединение ещё не помечено как CONNECTED.
            webSocket.send(
                buildFrame(
                    command = "CONNECT",
                    headers = mapOf(
                        "accept-version" to "1.2",
                        "heart-beat" to "0,0",
                        // Именно этот заголовок читает JwtChannelInterceptor.
                        "Authorization" to "Bearer $token",
                    ),
                    body = "",
                ),
            )
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!isCurrent(webSocket)) return
            appendChunk(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            if (!isCurrent(webSocket)) return
            appendChunk(bytes.utf8())
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!isCurrent(webSocket)) return
            isConnected = false
            eventsChannel.trySend(Event.Disconnected)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!isCurrent(webSocket)) return
            isConnected = false
            val reason = t.message?.takeIf { it.isNotBlank() } ?: response?.message
            Log.e(TAG, "WebSocket failure: $reason", t)
            eventsChannel.trySend(Event.Failure(reason, t))
        }

        private fun appendChunk(chunk: String) {
            buffer.append(chunk)

            while (true) {
                // Heartbeat — одиночный перевод строки между фреймами.
                while (buffer.isNotEmpty() && (buffer[0] == '\n' || buffer[0] == '\r')) {
                    buffer.deleteCharAt(0)
                }

                val frameEnd = buffer.indexOf('\u0000')
                if (frameEnd < 0) return

                val raw = buffer.substring(0, frameEnd)
                buffer.delete(0, frameEnd + 1)
                handleFrame(StompFrame.parse(raw))
            }
        }

        private fun handleFrame(frame: StompFrame?) {
            if (frame == null) return

            when (frame.command) {
                "CONNECTED" -> onConnected()

                "MESSAGE" -> {
                    Log.d(TAG, "MESSAGE ${frame.headers["destination"]}: ${frame.body}")
                    eventsChannel.trySend(
                        Event.Message(
                            destination = frame.headers["destination"].orEmpty(),
                            subscriptionId = frame.headers["subscription"],
                            body = frame.body,
                        ),
                    )
                }

                "ERROR" -> {
                    val reason = frame.headers["message"] ?: frame.body.ifBlank { null }
                    Log.e(TAG, "STOMP ERROR: $reason")
                    eventsChannel.trySend(Event.Failure(reason))
                }

                // RECEIPT и неизвестные команды нам не нужны.
                else -> Unit
            }
        }
    }

    private companion object {
        const val TAG = "MY_LOG"
        const val NORMAL_CLOSURE = 1000
        const val POLICY_VIOLATION = 1008
    }
}
