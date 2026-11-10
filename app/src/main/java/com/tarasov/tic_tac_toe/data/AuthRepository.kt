package com.tarasov.tic_tac_toe.data

interface AuthRepository {
    suspend fun login(username: String, password: String): ApiResult<Unit>
    suspend fun registration(username: String, password: String): ApiResult<Unit>
}