package com.tarasov.tic_tac_toe.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarasov.tic_tac_toe.data.ApiResult
import com.tarasov.tic_tac_toe.data.WebsocketEvent
import com.tarasov.tic_tac_toe.data.WebsocketRepository
import com.tarasov.tic_tac_toe.data.LobbyRepository
import com.tarasov.tic_tac_toe.ui.toUserMessage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LobbyViewModel(
    private val lobbyRepository: LobbyRepository,
    private val websocketRepository: WebsocketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState(isLoading = true))
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    init {
        observeSocketEvents()
        loadActiveUsers()
        // Токен уже сохранён репозиторием логина — подключаемся и ждём /app/greeting.
        websocketRepository.connect()
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

    /**
     * Подписка на события сокета. Коллектор стартует синхронно (UNDISPATCHED),
     * поэтому к моменту connect() он уже подписан и ни приветствие,
     * ни ошибка подключения не теряются.
     */
    private fun observeSocketEvents() {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            websocketRepository.events.collect { event ->
                when (event) {
                    is WebsocketEvent.Greeting -> _uiState.update {
                        Log.d("MY_LOG", "event.text = ${event.text}")

                        it.copy(greeting = event.text, socketError = null)
                    }

                    is WebsocketEvent.LobbyEventReceived -> _uiState.update {
                        it.applyLobbyEvent(event.lobbyEvent)
                    }

                    is WebsocketEvent.Error -> _uiState.update {
                        it.copy(socketError = event.message)
                    }

                    WebsocketEvent.Connected -> _uiState.update {
                        it.copy(socketError = null)
                    }

                    WebsocketEvent.Disconnected -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        websocketRepository.disconnect()
        super.onCleared()
    }
}
