package com.escom.buscaminas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.escom.buscaminas.data.CellState
import com.escom.buscaminas.data.GameState
import com.escom.buscaminas.data.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

// --- Constantes del Juego ---
private const val ROW_COUNT = 9
private const val COL_COUNT = 11
private const val MINE_COUNT = 12
private const val GAME_DURATION_SECONDS = 180
private const val INITIAL_COUNTDOWN_SECONDS = 5
private const val PENALTY_SECONDS = 5
private const val SAVE_FILE_NAME = "last_game_state.json"

// --- Clases de Estado para la UI ---

// Representa el estado de una celda individual en la UI
data class UICell(
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val hasMine: Boolean = false,
    val adjacentMines: Int = 0,
)

// Representa el estado de un jugador
data class UIPlayerState(
    val board: List<List<UICell>> = emptyList(),
    val flagsPlaced: Int = 0,
    val timeTaken: Int? = null // Tiempo final del jugador
)

// Representa el estado global del juego que la UI observará
data class UIGameState(
    val status: GameStatus = GameStatus.IDLE,
    val player1: UIPlayerState = UIPlayerState(),
    val player2: UIPlayerState = UIPlayerState(),
    val countdownValue: Int = INITIAL_COUNTDOWN_SECONDS,
    val gameTimerValue: Int = GAME_DURATION_SECONDS,
    val winner: Player? = null,
    val hasSavedGame: Boolean = false // Para saber si hay una partida guardada
)

// Enum para los diferentes estados del flujo del juego
enum class GameStatus { IDLE, COUNTDOWN, PLAYING, PAUSED, GAME_OVER }
enum class Player { ONE, TWO }

class GameViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UIGameState())
    val uiState = _uiState.asStateFlow()

    private var gameTimerJob: Job? = null
    private val json = Json { prettyPrint = true }
    private val saveFile = File(application.filesDir, SAVE_FILE_NAME)

    init {
        // Al iniciar el ViewModel, comprueba si existe una partida guardada
        checkForSavedGame()
    }

    // --- Lógica de Control del Juego ---

    fun startNewGame() {
        _uiState.update {
            it.copy(
                player1 = UIPlayerState(board = generateBoard()),
                player2 = UIPlayerState(board = generateBoard()),
                status = GameStatus.COUNTDOWN,
                countdownValue = INITIAL_COUNTDOWN_SECONDS,
                gameTimerValue = GAME_DURATION_SECONDS,
                winner = null
            )
        }
        startInitialCountdown()
    }

    fun resumeGame() {
        if (_uiState.value.status == GameStatus.PAUSED) {
            _uiState.update { it.copy(status = GameStatus.COUNTDOWN) }
            startInitialCountdown(isResuming = true)
        }
    }

    private fun startInitialCountdown(isResuming: Boolean = false) {
        viewModelScope.launch {
            for (i in INITIAL_COUNTDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(countdownValue = i) }
                delay(1000)
            }
            _uiState.update { it.copy(status = GameStatus.PLAYING) }
            startGameTimer(isResuming)
        }
    }

    private fun startGameTimer(isResuming: Boolean) {
        gameTimerJob?.cancel() // Cancela cualquier timer anterior
        gameTimerJob = viewModelScope.launch {
            val startTime = if (isResuming) _uiState.value.gameTimerValue else GAME_DURATION_SECONDS
            for (time in startTime downTo 0) {
                _uiState.update { it.copy(gameTimerValue = time) }
                if (time == 0) {
                    endGameByTimeout()
                }
                delay(1000)
            }
        }
    }

    fun pauseGame() {
        gameTimerJob?.cancel()
        _uiState.update { it.copy(status = GameStatus.PAUSED) }
    }

    private fun endGameByTimeout() {
        // Lógica para determinar el ganador si se acaba el tiempo
        // Por ejemplo, gana quien más casillas haya revelado
        val p1Revealed = _uiState.value.player1.board.flatten().count { it.isRevealed && !it.hasMine }
        val p2Revealed = _uiState.value.player2.board.flatten().count { it.isRevealed && !it.hasMine }

        val winner = when {
            p1Revealed > p2Revealed -> Player.ONE
            p2Revealed > p1Revealed -> Player.TWO
            else -> null // Empate
        }
        _uiState.update { it.copy(status = GameStatus.GAME_OVER, winner = winner) }
    }

    // --- Lógica de Interacción del Jugador ---

    fun onCellClick(player: Player, row: Int, col: Int) {
        if (_uiState.value.status != GameStatus.PLAYING) return

        val currentPlayerState = if (player == Player.ONE) _uiState.value.player1 else _uiState.value.player2
        val cell = currentPlayerState.board[row][col]

        if (cell.isRevealed || cell.isFlagged) return

        if (cell.hasMine) {
            // El jugador pierde
            val winner = if (player == Player.ONE) Player.TWO else Player.ONE
            revealAllMines(player) // Muestra las minas al jugador que perdió
            _uiState.update { it.copy(status = GameStatus.GAME_OVER, winner = winner) }
            gameTimerJob?.cancel()
            return
        }

        val newBoard = revealCellRecursive(row, col, currentPlayerState.board)
        updatePlayerState(player, newBoard)
        checkWinCondition(player)
    }

    fun onCellLongClick(player: Player, row: Int, col: Int) {
        if (_uiState.value.status != GameStatus.PLAYING) return

        val currentPlayerState = if (player == Player.ONE) _uiState.value.player1 else _uiState.value.player2
        val cell = currentPlayerState.board[row][col]

        if (cell.isRevealed) return

        val newBoard = currentPlayerState.board.map { it.toMutableList() }.toMutableList()
        val currentFlags = currentPlayerState.flagsPlaced

        val newFlags = if (!cell.isFlagged) {
            newBoard[row][col] = cell.copy(isFlagged = true)
            currentFlags + 1
        } else {
            newBoard[row][col] = cell.copy(isFlagged = false)
            currentFlags - 1
        }

        val finalBoard = newBoard.map { it.toList() }
        if(player == Player.ONE) {
            _uiState.update { it.copy(player1 = it.player1.copy(board = finalBoard, flagsPlaced = newFlags)) }
        } else {
            _uiState.update { it.copy(player2 = it.player2.copy(board = finalBoard, flagsPlaced = newFlags)) }
        }
    }

    // --- Lógica del Tablero ---

    private fun generateBoard(): List<List<UICell>> {
        val board = MutableList(ROW_COUNT) { MutableList(COL_COUNT) { UICell() } }
        var minesPlaced = 0

        // 1. Colocar minas aleatoriamente
        while (minesPlaced < MINE_COUNT) {
            val r = Random.nextInt(ROW_COUNT)
            val c = Random.nextInt(COL_COUNT)
            if (!board[r][c].hasMine) {
                board[r][c] = board[r][c].copy(hasMine = true)
                minesPlaced++
            }
        }

        // 2. Calcular números de minas adyacentes
        for (r in 0 until ROW_COUNT) {
            for (c in 0 until COL_COUNT) {
                if (!board[r][c].hasMine) {
                    val adjacentMines = countAdjacentMines(r, c, board)
                    board[r][c] = board[r][c].copy(adjacentMines = adjacentMines)
                }
            }
        }
        return board.map { it.toList() }
    }

    private fun countAdjacentMines(row: Int, col: Int, board: List<List<UICell>>): Int {
        var count = 0
        for (r in -1..1) {
            for (c in -1..1) {
                val newRow = row + r
                val newCol = col + c
                if (newRow in 0 until ROW_COUNT && newCol in 0 until COL_COUNT && board[newRow][newCol].hasMine) {
                    count++
                }
            }
        }
        return count
    }

    private fun revealCellRecursive(row: Int, col: Int, board: List<List<UICell>>): List<List<UICell>> {
        val newBoard = board.map { it.toMutableList() }.toMutableList()
        val cell = newBoard[row][col]

        if (cell.isRevealed) return board // Ya está revelada

        newBoard[row][col] = cell.copy(isRevealed = true, isFlagged = false)

        // Si la celda es vacía (0 minas adyacentes), revela las vecinas
        if (cell.adjacentMines == 0) {
            for (r in -1..1) {
                for (c in -1..1) {
                    val newRow = row + r
                    val newCol = col + c
                    if (newRow in 0 until ROW_COUNT && newCol in 0 until COL_COUNT) {
                        // Llamada recursiva
                        return revealCellRecursive(newRow, newCol, newBoard.map { it.toList() })
                    }
                }
            }
        }
        return newBoard.map { it.toList() }
    }

    private fun revealAllMines(player: Player) {
        val currentPlayerState = if (player == Player.ONE) _uiState.value.player1 else _uiState.value.player2
        val newBoard = currentPlayerState.board.map { row ->
            row.map { cell ->
                if (cell.hasMine) cell.copy(isRevealed = true) else cell
            }
        }
        updatePlayerState(player, newBoard)
    }

    private fun checkWinCondition(player: Player) {
        val playerState = if (player == Player.ONE) _uiState.value.player1 else _uiState.value.player2
        val nonMineCells = playerState.board.flatten().filter { !it.hasMine }

        if (nonMineCells.all { it.isRevealed }) {
            // El jugador ha ganado
            val timeTaken = GAME_DURATION_SECONDS - _uiState.value.gameTimerValue

            // Calcular penalización
            val incorrectFlags = playerState.board.flatten().count { it.isFlagged && !it.hasMine }
            val finalTime = timeTaken + (incorrectFlags * PENALTY_SECONDS)

            val updatedPlayerState = playerState.copy(timeTaken = finalTime)

            if (player == Player.ONE) {
                _uiState.update { it.copy(player1 = updatedPlayerState) }
            } else {
                _uiState.update { it.copy(player2 = updatedPlayerState) }
            }

            // Comprobar si ambos han terminado
            val otherPlayer = if (player == Player.ONE) _uiState.value.player2 else _uiState.value.player1
            if(otherPlayer.timeTaken != null) {
                // Ambos terminaron, declarar ganador
                val winner = if(finalTime < otherPlayer.timeTaken) player else (if(player == Player.ONE) Player.TWO else Player.ONE)
                _uiState.update { it.copy(status = GameStatus.GAME_OVER, winner = winner) }
                gameTimerJob?.cancel()
            }
        }
    }

    private fun updatePlayerState(player: Player, newBoard: List<List<UICell>>) {
        if (player == Player.ONE) {
            _uiState.update { it.copy(player1 = it.player1.copy(board = newBoard)) }
        } else {
            _uiState.update { it.copy(player2 = it.player2.copy(board = newBoard)) }
        }
    }


    // --- Lógica de Guardado y Carga ---

    fun saveGame() {
        if (_uiState.value.status != GameStatus.PAUSED) return

        // Mapear estado de UI a estado de datos serializable
        val gameStateToSave = GameState(
            player1State = mapToSerializablePlayerState(_uiState.value.player1),
            player2State = mapToSerializablePlayerState(_uiState.value.player2),
            remainingTime = _uiState.value.gameTimerValue
        )

        try {
            val jsonString = json.encodeToString(gameStateToSave)
            saveFile.writeText(jsonString)
        } catch (e: Exception) {
            // Manejar error de guardado
            e.printStackTrace()
        }
    }

    fun loadGame() {
        if (!saveFile.exists()) return

        try {
            val jsonString = saveFile.readText()
            val loadedGameState = json.decodeFromString<GameState>(jsonString)

            // Mapear estado de datos a estado de UI
            _uiState.update {
                it.copy(
                    player1 = mapFromSerializablePlayerState(loadedGameState.player1State),
                    player2 = mapFromSerializablePlayerState(loadedGameState.player2State),
                    gameTimerValue = loadedGameState.remainingTime,
                    status = GameStatus.PAUSED,
                    hasSavedGame = true
                )
            }
        } catch (e: Exception) {
            // Manejar error de carga
            e.printStackTrace()
            _uiState.update { it.copy(hasSavedGame = false) }
        }
    }

    fun deleteSavedGame() {
        if (saveFile.exists()) {
            saveFile.delete()
        }
        _uiState.update { it.copy(hasSavedGame = false) }
    }

    private fun checkForSavedGame() {
        _uiState.update { it.copy(hasSavedGame = saveFile.exists()) }
    }

    // Funciones de mapeo para la serialización
    private fun mapToSerializablePlayerState(uiPlayerState: UIPlayerState): PlayerState {
        return PlayerState(
            board = uiPlayerState.board.map { row ->
                row.map { cell ->
                    CellState(cell.isRevealed, cell.isFlagged, cell.hasMine, cell.adjacentMines)
                }
            },
            bombsFound = uiPlayerState.flagsPlaced // En este contexto, flagsPlaced es lo mismo
        )
    }

    private fun mapFromSerializablePlayerState(playerState: PlayerState): UIPlayerState {
        return UIPlayerState(
            board = playerState.board.map { row ->
                row.map { cell ->
                    UICell(cell.isRevealed, cell.isFlagged, cell.hasMine, cell.adjacentMines)
                }
            },
            flagsPlaced = playerState.bombsFound
        )
    }
}