package com.tarasov.tic_tac_toe.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * DTO запроса на логин/регистрацию.
 */
@Serializable
data class LoginRequest(
    @SerialName("login") val login: String,
    @SerialName("password") val password: String,
)

/**
 * DTO ответа сервера. null-поля здесь допустимы: не все эндпоинты возвращают токен.
 */
@Serializable
data class LoginResponse(
    @SerialName("token") val token: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("message") val message: String? = null,
)

/**
 * Retrofit-интерфейс. Только объявления, никакой логики — реализацию генерирует Retrofit.
 */
interface LoginApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun registration(@Body request: LoginRequest): LoginResponse
}
