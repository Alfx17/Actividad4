package com.escom.buscaminas.data

import kotlinx.serialization.Serializable

@Serializable
data class CellState(
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val hasMine: Boolean = false,
    val adjacentMines: Int = 0
)

@Serializable
data class PlayerState(
    val board: List<List<CellState>>,
    val bombsFound: Int,
)

@Serializable
data class GameState(
    val player1State: PlayerState,
    val player2State: PlayerState,
    val remainingTime: Int
)