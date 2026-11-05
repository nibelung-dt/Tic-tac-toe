package com.tarasov.tic_tac_toe.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarasov.tic_tac_toe.data.LoginRepository
import kotlinx.coroutines.CancellationException
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
    private val loginRepository: LoginRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onLoginChange(value: String) {
        _uiState.update { it.copy(login = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onLoginClick(onSuccess: () -> Unit) = authenticate(onSuccess, loginRepository::login)

    fun onRegisterClick(onSuccess: () -> Unit) = authenticate(onSuccess, loginRepository::registration)

    fun onCancelClick() {
        _uiState.value = LoginUiState()
    }

    private fun authenticate(onSuccess: () -> Unit, action: suspend () -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                action()
                _uiState.value = LoginUiState()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Не удалось выполнить вход")
                }
            }
        }
    }
}
