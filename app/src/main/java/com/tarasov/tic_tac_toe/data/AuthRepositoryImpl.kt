package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.LoginApi
import com.tarasov.tic_tac_toe.data.remote.LoginRequest

class AuthRepositoryImpl(
    private val api: LoginApi,
) : AuthRepository {

    override suspend fun login(login: String, password: String) {
        api.login(LoginRequest(login = login, password = password))
    }

    override suspend fun registration(login: String, password: String) {
        api.registration(LoginRequest(login = login, password = password))
    }
}