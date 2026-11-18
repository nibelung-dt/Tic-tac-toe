package com.tarasov.tic_tac_toe.data.remote.ws

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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
 * При обрыве соединения клиент сам переподключается с экспоненциальным backoff
 * и восстанавливает активные подписки в новой STOMP-сессии.
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

    /** Активные подписки: восстанавливаются после переподключения. */
    private val activeSubscriptions = LinkedHashMap<String, String>()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var jwt: String? = null

    @Volatile
    private var isConnected = false

    /** Сколько раз подряд переподключались без успешного CONNECTED. */
    @Volatile
    private var reconnectAttempts = 0

    /** Отменяет запланированное переподключение, если пользователь сам вызвал disconnect(). */
    @Volatile
    private var reconnectJob: Job? = null

    /** Скоуп для отложенного переподключения; живёт столько же, сколько клиент. */
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        // Счётчик попыток начинаем с нуля: это явное подключение, а не реконнект.
        reconnectAttempts = 0
        openSocket()
    }

    private fun openSocket() {
        webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(url).build(),
            Listener(),
        )
    }

    /**
     * Подписка на destination.
     *
     * Структура destination зависит от того, кто отвечает:
     * - `@SubscribeMapping` / `@MessageMapping` — через application prefix (`/app/...`);
     * - рассылка сервером через `SimpMessagingTemplate` — напрямую (`/lobby`, `/topic/...`).
     *
     * @return id подписки (сервер вернёт его в заголовке subscription).
     */
    fun subscribe(destination: String): String {
        val id = "sub-${subscriptionIds.incrementAndGet()}"

        // Подписка может быть вызвана сразу из обработчика CONNECTED:
        // фиксируем её в журнале, чтобы при переподключении восстановить.
        synchronized(pendingLock) {
            activeSubscriptions[id] = destination
        }

        Log.d(TAG, "SUBSCRIBE $destination (id=$id)")
        sendSubscriptionFrame(id, destination)
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
        reconnectJob?.cancel()
        reconnectJob = null

        val socket = webSocket ?: return
        Log.d(TAG, "Disconnecting")

        if (isConnected) {
            isConnected = false
            // DISCONNECT отправляем напрямую: sendFrame() смотрит на webSocket.
            socket.send(buildFrame(command = "DISCONNECT", headers = emptyMap(), body = ""))
        }

        synchronized(pendingLock) {
            activeSubscriptions.clear()
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
        reconnectAttempts = 0
        reconnectJob?.cancel()
        reconnectJob = null

        Log.d(TAG, "STOMP CONNECTED")
        eventsChannel.trySend(Event.Connected)

        // Новое соединение — новая STOMP-сессия: подписки старой сессии
        // на сервере уже не действуют, отправляем их заново.
        restoreSubscriptions()
    }

    private fun restoreSubscriptions() {
        val subscriptions = synchronized(pendingLock) {
            activeSubscriptions.toList()
        }
        if (subscriptions.isEmpty()) return

        Log.d(TAG, "Restoring ${subscriptions.size} subscription(s)")
        subscriptions.forEach { (id, destination) ->
            sendSubscriptionFrame(id, destination)
        }
    }

    private fun sendSubscriptionFrame(id: String, destination: String) {
        sendFrame(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to id,
                "destination" to destination,
                "ack" to "auto",
            ),
        )
    }

    /**
     * Планирует переподключение с экспоненциальным backoff.
     *
     * Без этого обрыв (Connection reset, рестарт бэкенда, таймаут прокси)
     * оставлял лобби «замороженным»: подписки уже не действуют,
     * а новые события прийти не могут.
     */
    private fun scheduleReconnect() {
        // Токена нет — переподключаться бессмысленно.
        if (jwt.isNullOrBlank()) return

        if (reconnectJob?.isActive == true) return

        val attempt = reconnectAttempts++
        val delayMillis = (BASE_RECONNECT_DELAY_MS shl attempt.coerceAtMost(MAX_BACKOFF_SHIFT))
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)
        Log.d(TAG, "Reconnect in ${delayMillis}ms (attempt ${attempt + 1})")

        reconnectJob = reconnectScope.launch {
            delay(delayMillis)
            // Пока ждали, пользователь мог уйти с экрана или переподключиться вручную.
            if (isConnected) return@launch
            Log.d(TAG, "Reconnecting...")
            openSocket()
        }
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
                scheduleReconnect()
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
            Log.d(TAG, "WebSocket closed: $code $reason")
            eventsChannel.trySend(Event.Disconnected)
            // Штатное закрытие сервером (например, таймаут) — тоже повод вернуться.
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!isCurrent(webSocket)) return
            isConnected = false
            val reason = t.message?.takeIf { it.isNotBlank() } ?: response?.message
            Log.e(TAG, "WebSocket failure: $reason", t)
            eventsChannel.trySend(Event.Failure(reason, t))
            scheduleReconnect()
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

        /** 1s, 2s, 4s, ... — пауза перед переподключением. */
        const val BASE_RECONNECT_DELAY_MS = 1_000L
        const val MAX_RECONNECT_DELAY_MS = 30_000L
        const val MAX_BACKOFF_SHIFT = 5
    }
}
