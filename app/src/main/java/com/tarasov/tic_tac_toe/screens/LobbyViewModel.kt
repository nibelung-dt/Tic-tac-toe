package com.tarasov.tic_tac_toe.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarasov.tic_tac_toe.data.Gamer
import com.tarasov.tic_tac_toe.data.LobbyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class LobbyViewModel(
    private val lobbyRepository: LobbyRepository,
): ViewModel() {

    private val _listUsers = MutableStateFlow<List<Gamer>>(emptyList())
    val listUsers = _listUsers

    init {
        viewModelScope.launch {
            // _listUsers.value = lobbyRepository.getListGamers()
            _listUsers.value = emptyList() //TODO это заглушка
        }
    }

    fun inviteToGame(id: Long) {
        viewModelScope.launch {
            lobbyRepository.inviteToGame(id)
        }
    }
}