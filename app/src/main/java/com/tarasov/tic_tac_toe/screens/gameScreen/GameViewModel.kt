package com.tarasov.tic_tac_toe.screens.gameScreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel(): ViewModel() {
    //TODO StateFlow или SharedFlow ?
    private val _gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState())
  //  val gameState: StateFlow<List<List<Int>>> = _gameState
    val gameState: StateFlow<GameState> = _gameState

  //  private val testList = listOf(listOf(1, 0, 2), listOf(0, 0, 2), listOf(1, 1, 2))

    init {
       // val x = CellState
        val test = GameState(board = listOf(
            CellType.O, CellType.EMPTY, CellType.O,
            CellType.EMPTY, CellType.X, CellType.EMPTY,
            CellType.EMPTY, CellType.EMPTY, CellType.X
            )
        )
        _gameState.value = test

    }
}