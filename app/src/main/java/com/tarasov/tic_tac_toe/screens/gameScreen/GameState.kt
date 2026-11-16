package com.tarasov.tic_tac_toe.screens.gameScreen


data class GameState(
    val board: List<CellType> = List(9) { CellType.EMPTY },
    val currentTurn: CellType = CellType.X,
    val status: GamePhase = GamePhase.InProgress
)

sealed interface GamePhase {
    object InProgress : GamePhase
    data class Winner(val symbol: Mark) : GamePhase
}

enum class Mark { X, O , DRAW}

enum class CellType { X, O, EMPTY }