package com.tarasov.tic_tac_toe.data

interface AuthRepository {
    suspend fun login(login: String, password: String)
    suspend fun registration(login: String, password: String)
}