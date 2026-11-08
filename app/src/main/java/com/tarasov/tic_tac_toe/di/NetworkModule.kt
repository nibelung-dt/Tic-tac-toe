package com.tarasov.tic_tac_toe.di

import com.tarasov.tic_tac_toe.BuildConfig
import com.tarasov.tic_tac_toe.data.remote.LobbyApi
import com.tarasov.tic_tac_toe.data.remote.LoginApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Базовая точка входа API приходит из BuildConfig:
 * debug — http://10.0.2.2:8080/ (хост-машина из эмулятора),
 * release — продовый HTTPS-эндпоинт.
 */
// private val BASE_URL = BuildConfig.BASE_URL
private val BASE_URL = "http://10.0.2.2:8080/"

/**
 * Сетевой слой. Всё — синглтоны: OkHttp держит пул соединений,
 * Retrofit — тяжёлый объект, их нельзя пересоздавать.
 */
val networkModule = module {

    single {
        Json {
            ignoreUnknownKeys = true   // сервер может добавить поля — не падаем
            coerceInputValues = true   // null в non-null поле -> дефолт, а не краш
            explicitNulls = false      // не отправляем null-поля в запросе
        }
    }

    single {
        HttpLoggingInterceptor().apply {
            // Логируем только в дебаге: в релизе тело запроса может содержать пароль
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(
                get<Json>().asConverterFactory("application/json".toMediaType()),
            )
            .build()
    }

    single<LoginApi> { get<Retrofit>().create(LoginApi::class.java) }

    single<LobbyApi> { get<Retrofit>().create(LobbyApi::class.java) }
}