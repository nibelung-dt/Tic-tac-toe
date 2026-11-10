package com.tarasov.tic_tac_toe.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Результат сетевого вызова. Явно описывает все исходы,
 * поэтому вызывающий код обязан обработать каждый вариант — компилятор подскажет.
 */
sealed interface ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>

    /**
     * Сервер ответил не-2xx.
     * @param serverMessage тело ответа, если сервер вернул текст ошибки.
     */
    data class HttpError(
        val code: Int,
        val serverMessage: String? = null,
    ) : ApiResult<Nothing>

    /** Нет сети: таймаут, DNS, обрыв соединения, TLS. */
    data object NoConnection : ApiResult<Nothing>

    /** Ответ пришёл, но не соответствует модели (поля не совпали). */
    data object InvalidResponse : ApiResult<Nothing>

    /** Всё остальное, что мы не предусмотрели. */
    data class Unknown(val cause: Throwable) : ApiResult<Nothing>
}

/**
 * Оборачивает suspend-вызов Retrofit в [ApiResult].
 *
 * Важно: [CancellationException] пробрасывается дальше, а не превращается в ошибку.
 * Иначе отменённая корутина продолжила бы выполняться и обновила UI после ухода с экрана.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ApiResult.HttpError(
            code = e.code(),
            serverMessage = e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() },
        )
    } catch (_: SerializationException) {
        ApiResult.InvalidResponse
    } catch (_: IOException) {
        ApiResult.NoConnection
    } catch (e: Exception) {
        ApiResult.Unknown(e)
    }
