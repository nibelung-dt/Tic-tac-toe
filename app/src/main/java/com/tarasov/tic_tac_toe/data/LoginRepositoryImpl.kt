package com.tarasov.tic_tac_toe.data

import com.tarasov.tic_tac_toe.data.remote.LoginApi
import com.tarasov.tic_tac_toe.data.remote.LoginRequest

class LoginRepositoryImpl(
    private val api: LoginApi,
) : LoginRepository {

    override suspend fun login() {
        api.login(LoginRequest(login = "", password = ""))
    }

    override suspend fun registration() {
        api.registration(LoginRequest(login = "", password = ""))
    }
}