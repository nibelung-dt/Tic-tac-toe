package com.tarasov.tic_tac_toe.ui

import com.tarasov.tic_tac_toe.data.ApiResult

/**
 * Приводит ошибку к тексту, который не стыдно показать пользователю.
 *
 * TODO: вынести в strings.xml, когда появится локализация.
 */
fun ApiResult<*>.toUserMessage(): String? = when (this) {
    is ApiResult.Success -> null

    is ApiResult.HttpError -> when (code) {
        401 -> "Требуется повторный вход"
        403 -> "Доступ запрещён"
        404 -> "Запрошенные данные не найдены"
        409 -> "Конфликт данных"
        408 -> "Сервер не отвечает"
        in 500..599 -> "Сервер недоступен, попробуйте позже"
        else -> "Ошибка запроса ($code)"
    }

    ApiResult.NoConnection -> "Нет подключения к интернету"
    ApiResult.InvalidResponse -> "Некорректный ответ сервера"
    is ApiResult.Unknown -> cause.message ?: "Неизвестная ошибка"
}
