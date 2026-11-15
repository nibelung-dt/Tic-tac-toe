package com.tarasov.tic_tac_toe.screens.gameScreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel(): ViewModel() {
    //TODO StateFlow или SharedFlow ?
    private val _gameState: MutableStateFlow<List<List<Int>>> = MutableStateFlow(emptyList())
  //  val gameState: StateFlow<List<List<Int>>> = _gameState
    val gameState: MutableStateFlow<List<List<Int>>> = _gameState

    private val testList = listOf(listOf(1, 0, 2), listOf(0, 0, 2), listOf(1, 1, 2))

    init {
        gameState.value = testList
    }
}