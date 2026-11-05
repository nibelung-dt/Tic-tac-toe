package com.tarasov.tic_tac_toe.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarasov.tic_tac_toe.data.Gamer
import com.tarasov.tic_tac_toe.data.LobbyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class LobbyViewModel(
    private val lobbyRepository: LobbyRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): ViewModel() {

    private val _listUsers = MutableStateFlow<List<Gamer>>(emptyList())
    val listUsers = _listUsers

    init {
        viewModelScope.launch(dispatcher) {
            _listUsers.value = lobbyRepository.getListGamers()
        }
    }



}