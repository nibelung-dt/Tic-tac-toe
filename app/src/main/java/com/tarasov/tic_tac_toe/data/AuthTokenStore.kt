package com.tarasov.tic_tac_toe.data

/**
 * Хранилище JWT текущей сессии.
 *
 * Нужно, чтобы после логина отдать токен сокету: STOMP-клиент кладёт его
 * в заголовок CONNECT, а JwtChannelInterceptor на бэке по нему выставляет Principal.
 *
 * Реализация in-memory: после смерти процесса токен теряется.
 * Если понадобится «вход сохраняется» — заменить на DataStore/EncryptedSharedPreferences.
 */
interface AuthTokenStore {
    val token: String?
    fun save(token: String?)
    fun clear()
}

class InMemoryAuthTokenStore : AuthTokenStore {

    @Volatile
    private var value: String? = null

    override val token: String?
        get() = value

    override fun save(token: String?) {
        value = token?.takeIf { it.isNotBlank() }
    }

    override fun clear() {
        value = null
    }
}
