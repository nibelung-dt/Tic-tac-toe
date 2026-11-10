package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.LoginApi
import com.tarasov.tic_tac_toe.data.remote.LoginRequest

class AuthRepositoryImpl(
    private val api: LoginApi,
) : AuthRepository {

    override suspend fun login(username: String, password: String): ApiResult<Unit> =
        safeApiCall {
            api.login(LoginRequest(username = username, password = password))
        }

    override suspend fun registration(username: String, password: String): ApiResult<Unit> =
        safeApiCall {
            api.registration(LoginRequest(username = username, password = password))
        }
}
