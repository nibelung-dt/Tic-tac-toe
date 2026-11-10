package com.tarasov.tic_tac_toe.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarasov.tic_tac_toe.data.ApiResult
import com.tarasov.tic_tac_toe.data.AuthRepository
import com.tarasov.tic_tac_toe.ui.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            username = currentState.login,
            password = currentState.password,
        )
    }

    fun onRegisterClick(onSuccess: () -> Unit) = authenticate(onSuccess) {
        authRepository.registration(
            username = currentState.login,
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
    private fun authenticate(
        onSuccess: () -> Unit,
        action: suspend () -> ApiResult<Unit>,
    ) {
        if (currentState.isLoading || !currentState.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = action()) {
                is ApiResult.Success -> {
                    _uiState.value = LoginUiState()
                    onSuccess()
                }

                is ApiResult.HttpError -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.toErrorMessage(),
                    )
                }

                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.toUserMessage() ?: DEFAULT_ERROR)
                }
            }
        }
    }

    /**
     * Для логина важны специфичные тексты: 401 означает «неверный пароль», а не «сессия истекла».
     */
    private fun ApiResult.HttpError.toErrorMessage(): String = when (code) {
        401 -> INVALID_CREDENTIALS_ERROR
        409 -> USER_EXISTS_ERROR
        else -> toUserMessage() ?: DEFAULT_ERROR
    }

    private companion object {
        const val DEFAULT_ERROR = "Не удалось выполнить вход"
        const val INVALID_CREDENTIALS_ERROR = "Неверный логин или пароль"
        const val USER_EXISTS_ERROR = "Такой пользователь уже существует"
    }
}
