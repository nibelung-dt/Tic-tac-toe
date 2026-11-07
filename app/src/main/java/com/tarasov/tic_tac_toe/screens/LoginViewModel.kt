package com.tarasov.tic_tac_toe.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarasov.tic_tac_toe.data.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Единое состояние экрана логина. UI полностью stateless — рисует то, что здесь лежит.
 */
data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = login.isNotBlank() && password.isNotBlank() && !isLoading
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val currentState: LoginUiState
        get() = _uiState.value

    fun onLoginChange(value: String) {
        _uiState.update { it.copy(login = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onLoginClick(onSuccess: () -> Unit) = authenticate(onSuccess) {
        authRepository.login(
            login = currentState.login,
            password = currentState.password,
        )
    }

    fun onRegisterClick(onSuccess: () -> Unit) = authenticate(onSuccess) {
        authRepository.registration(
            login = currentState.login,
            password = currentState.password,
        )
    }

    fun onCancelClick() {
        _uiState.value = LoginUiState()
    }

    /**
     * Общая логика для логина и регистрации:
     * защита от двойного клика, индикатор загрузки, единая обработка ошибок.
     */
    private fun authenticate(onSuccess: () -> Unit, action: suspend () -> Unit) {
        if (currentState.isLoading || !currentState.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                action()
                _uiState.value = LoginUiState()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.toUserMessage())
                }
            } catch (_: IOException) {
                // Нет сети, таймаут, DNS, обрыв соединения
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = NO_CONNECTION_ERROR)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: DEFAULT_ERROR)
                }
            }
        }
    }

    /**
     * Приводим HTTP-код к тексту, понятному пользователю.
     * HttpException.message — это "HTTP 401 Client Error", в UI такое показывать нельзя.
     */
    private fun HttpException.toUserMessage(): String = when (code()) {
        401 -> INVALID_CREDENTIALS_ERROR
        409 -> USER_EXISTS_ERROR
        in 500..599 -> SERVER_ERROR
        else -> "Ошибка запроса (${code()})"
    }

    private companion object {
        const val DEFAULT_ERROR = "Не удалось выполнить вход"
        const val INVALID_CREDENTIALS_ERROR = "Неверный логин или пароль"
        const val USER_EXISTS_ERROR = "Такой пользователь уже существует"
        const val SERVER_ERROR = "Сервер недоступен, попробуйте позже"
        const val NO_CONNECTION_ERROR = "Нет подключения к интернету"
    }
}
