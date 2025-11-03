package com.escom.buscaminas.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.escom.buscaminas.R
import com.escom.buscaminas.ui.GameStatus
import com.escom.buscaminas.ui.GameViewModel
import com.escom.buscaminas.ui.Player
import com.escom.buscaminas.ui.UICell

private const val MINE_COUNT = 12

@Composable
fun GameScreen(navController: NavController, viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Inicia el juego automáticamente
    LaunchedEffect(Unit) {
        if (uiState.status == GameStatus.IDLE) {
            viewModel.startNewGame()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Vista del Jugador 2 (arriba)
            PlayerView(
                modifier = Modifier
                    .weight(1f)
                    .rotate(180f),
                player = Player.TWO,
                playerState = uiState.player2,
                backgroundImageResId = R.drawable.fondo_py1, // Fondo personalizado
                onCellClick = { row, col -> viewModel.onCellClick(Player.TWO, row, col) },
                onCellLongClick = { row, col -> viewModel.onCellLongClick(Player.TWO, row, col) },
                onPauseClick = { viewModel.pauseGame() },
                gameTimerValue = uiState.gameTimerValue
            )

            // --- Vista del Jugador 1 (abajo)
            PlayerView(
                modifier = Modifier.weight(1f),
                player = Player.ONE,
                playerState = uiState.player1,
                backgroundImageResId = R.drawable.fondo_py2, // Fondo personalizado
                onCellClick = { row, col -> viewModel.onCellClick(Player.ONE, row, col) },
                onCellLongClick = { row, col -> viewModel.onCellLongClick(Player.ONE, row, col) },
                onPauseClick = { viewModel.pauseGame() },
                gameTimerValue = uiState.gameTimerValue
            )
        }

        // --- Overlays de Estado del Juego ---
        AnimatedVisibility(
            visible = uiState.status == GameStatus.COUNTDOWN,
            enter = fadeIn(), exit = fadeOut()
        ) {
            CountdownOverlay(uiState.countdownValue)
        }

        AnimatedVisibility(
            visible = uiState.status == GameStatus.PAUSED,
            enter = fadeIn(), exit = fadeOut()
        ) {
            PauseOverlay(
                onResume = { viewModel.resumeGame() },
                onSave = { viewModel.saveGame() },
                onLoad = { viewModel.loadGame() },
                onNewGame = { viewModel.startNewGame() },
                onQuit = { navController.popBackStack() },
                hasSavedGame = uiState.hasSavedGame
            )
        }

        AnimatedVisibility(
            visible = uiState.status == GameStatus.GAME_OVER,
            enter = fadeIn() + scaleIn(), exit = fadeOut()
        ) {
            GameOverOverlay(
                winner = uiState.winner,
                onPlayAgain = { viewModel.startNewGame() },
                onMainMenu = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PlayerView(
    modifier: Modifier = Modifier,
    player: Player,
    playerState: com.escom.buscaminas.ui.UIPlayerState,
    @DrawableRes backgroundImageResId: Int,
    onCellClick: (row: Int, col: Int) -> Unit,
    onCellLongClick: (row: Int, col: Int) -> Unit,
    onPauseClick: () -> Unit,
    gameTimerValue: Int
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundImageResId),
            contentDescription = "Fondo de jugador",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            PlayerHUD(
                gameTimerValue = gameTimerValue,
                flagsPlaced = playerState.flagsPlaced,
                onPauseClick = onPauseClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            GameBoard(
                board = playerState.board,
                onCellClick = onCellClick,
                onCellLongClick = onCellLongClick
            )
        }
    }
}

@Composable
fun PlayerHUD(gameTimerValue: Int, flagsPlaced: Int, onPauseClick: () -> Unit) {
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatTime(gameTimerValue),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Minas: ${MINE_COUNT - flagsPlaced}",
            fontSize = 18.sp,
            color = Color.White
        )
        IconButton(onClick = onPauseClick) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pausar Juego",
                tint = Color.White
            )
        }
    }
}

@Composable
fun GameBoard(
    board: List<List<UICell>>,
    onCellClick: (row: Int, col: Int) -> Unit,
    onCellLongClick: (row: Int, col: Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            board.forEachIndexed { rowIndex, row ->
                Row {
                    row.forEachIndexed { colIndex, cell ->
                        GameCell(
                            cell = cell,
                            onClick = { onCellClick(rowIndex, colIndex) },
                            onLongClick = { onCellLongClick(rowIndex, colIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameCell(cell: UICell, onClick: () -> Unit, onLongClick: () -> Unit) {
    val cellColor = when {
        !cell.isRevealed -> MaterialTheme.colorScheme.surfaceVariant
        cell.hasMine -> Color.Red.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (!cell.isRevealed) MaterialTheme.colorScheme.outline else Color.Transparent

    Box(
        modifier = Modifier
            .size(30.dp) // Este valor se puede ajustar para hacer la cuadrícula más grande o pequeña
            .background(cellColor)
            .border(1.dp, borderColor)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (cell.isRevealed) {
            when {
                cell.hasMine -> Text("💣", fontSize = 14.sp)
                cell.adjacentMines > 0 -> Text(
                    text = cell.adjacentMines.toString(),
                    color = getNumberColor(cell.adjacentMines),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else if (cell.isFlagged) {
            Text("🚩", fontSize = 14.sp)
        }
    }
}

@Composable
private fun getNumberColor(number: Int): Color {
    return when (number) {
        1 -> Color.Blue
        2 -> Color(0xFF008000) // Verde
        3 -> Color.Red
        4 -> Color(0xFF000080) // Azul Marino
        5 -> Color(0xFF800000) // Marrón
        6 -> Color(0xFF008080) // Cian
        7 -> Color.Black
        8 -> Color.Gray
        else -> Color.Transparent
    }
}

// --- OVERLAYS ---
@Composable
fun CountdownOverlay(value: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = value.toString(), fontSize = 120.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun PauseOverlay(
    onResume: () -> Unit, onSave: () -> Unit, onLoad: () -> Unit,
    onNewGame: () -> Unit, onQuit: () -> Unit, hasSavedGame: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSA", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onResume) { Text("Reanudar") }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSave) { Text("Guardar Partida") }
            if (hasSavedGame) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onLoad) { Text("Cargar Última Partida") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNewGame) { Text("Empezar Nueva Partida") }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onQuit) { Text("Salir al Menú Principal") }
        }
    }
}

@Composable
fun GameOverOverlay(winner: Player?, onPlayAgain: () -> Unit, onMainMenu: () -> Unit) {
    val winnerText = when (winner) {
        Player.ONE -> "¡Jugador 1 Gana!"
        Player.TWO -> "¡Jugador 2 Gana!"
        null -> "¡Es un Empate!"
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Fin del Juego", fontSize = 24.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = winnerText, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                color = if (winner == Player.ONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Text(
                modifier = Modifier.rotate(180f), text = winnerText, fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (winner == Player.TWO) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row {
                Button(onClick = onPlayAgain) { Text("Jugar de Nuevo") }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(onClick = onMainMenu) { Text("Menú Principal") }
            }
        }
    }
}