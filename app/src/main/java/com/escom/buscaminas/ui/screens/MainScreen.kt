package com.escom.buscaminas.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.NavController
import com.escom.buscaminas.R
import com.escom.buscaminas.data.UserPreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navController: NavController,
    userPreferencesRepository: UserPreferencesRepository,
    isDarkMode: Boolean
) {

    val scope = rememberCoroutineScope()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.fondo),
            contentDescription = "Fondo de pantalla del juego",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tuerca),
                contentDescription = "Ajustes"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Image(
                painter = painterResource(id = R.drawable.titulo),
                contentDescription = "Logo de Buscaminas",
                modifier = Modifier
                    .fillMaxWidth(0.99f)
                    .padding(top = 0.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            ThemedButton(
                text = "INICIAR MISIÓN",
                onClick = { navController.navigate("game") }
            )

            Spacer(modifier = Modifier.height(130.dp))
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showSettingsDialog = false },
            onThemeChange = { newIsDarkMode ->

                scope.launch {
                    userPreferencesRepository.setDarkMode(newIsDarkMode)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedButton(text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .height(250.dp)
                .width(600.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                color = Color(0xFFE53935),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(id = R.drawable.boton),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

        }
    }
}

@Composable
fun SettingsDialog(isDarkMode: Boolean, onDismiss: () -> Unit, onThemeChange: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes") },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Modo Oscuro", modifier = Modifier.weight(1f))
                    Switch(checked = isDarkMode, onCheckedChange = onThemeChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}