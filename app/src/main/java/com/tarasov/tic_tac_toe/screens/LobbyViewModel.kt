package com.tarasov.tic_tac_toe.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarasov.tic_tac_toe.data.ApiResult
import com.tarasov.tic_tac_toe.data.LobbyRepository
import com.tarasov.tic_tac_toe.data.User
import com.tarasov.tic_tac_toe.ui.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LobbyUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** true, если сервер ответил 401 и сессия больше не действительна. */
    val isSessionExpired: Boolean = false,
)

class LobbyViewModel(
    private val lobbyRepository: LobbyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState(isLoading = true))
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    init {
        loadActiveUsers()
    }

    fun loadActiveUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = lobbyRepository.getActiveUsers()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(users = result.data, isLoading = false, errorMessage = null)
                }

                is ApiResult.HttpError -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.toUserMessage(),
                        isSessionExpired = result.code == 401,
                    )
                }

                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.toUserMessage())
                }
            }
        }
    }

    fun inviteToGame(id: Long) {
        viewModelScope.launch {
            when (val result = lobbyRepository.inviteToGame(id)) {
                is ApiResult.Success -> Unit
                is ApiResult.HttpError -> _uiState.update {
                    it.copy(
                        errorMessage = result.toUserMessage(),
                        isSessionExpired = result.code == 401,
                    )
                }

                else -> _uiState.update { it.copy(errorMessage = result.toUserMessage()) }
            }
        }
    }
}
