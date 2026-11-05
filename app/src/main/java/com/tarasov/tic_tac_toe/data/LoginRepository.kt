package com.tarasov.tic_tac_toe.data

interface LoginRepository {
    suspend fun login()
    suspend fun registration()
}