package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.LoginApi
import com.tarasov.tic_tac_toe.data.remote.LoginRequest
import com.tarasov.tic_tac_toe.data.remote.LoginResponse

class AuthRepositoryImpl(
    private val api: LoginApi,
    private val tokenStore: AuthTokenStore,
) : AuthRepository {

    override suspend fun login(username: String, password: String): ApiResult<Unit> =
        authenticate {
            api.login(LoginRequest(username = username, password = password))
        }

    override suspend fun registration(username: String, password: String): ApiResult<Unit> =
        authenticate {
            api.registration(LoginRequest(username = username, password = password))
        }

    /**
     * После успешного входа сохраняем JWT: он понадобится WebSocket-клиенту
     * для STOMP-фрейма CONNECT. Если сервер токен не вернул — чистим старое значение,
     * чтобы сокет не пытался подключиться с протухшим токеном.
     */
    private suspend fun authenticate(
        request: suspend () -> LoginResponse,
    ): ApiResult<Unit> =
        safeApiCall(request)
            .map { response ->
                tokenStore.save(response.token)
                Unit
            }
}
