package com.escom.buscaminas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GameBoard(rows: Int, cols: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        repeat(rows) { rowIndex ->
            Row {
                repeat(cols) { colIndex ->
                    GameCell(
                        onClick = { /* TODO: viewModel.onCellClick(...) */ },
                        onLongClick = { /* TODO: viewModel.onCellLongClick(...) */ }
                    )
                }
            }
        }
    }
}

@Composable
fun GameCell(onClick: () -> Unit, onLongClick: () -> Unit) {
    // Este es un placeholder. La apariencia de la celda dependería del estado
    // (revelada, oculta, con bandera, con número, con bomba).
    Box(
        modifier = Modifier
            .size(36.dp) // Ajusta el tamaño según la pantalla
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {

    }
}