package com.example.dicegame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Dice Game", fontSize = 32.sp, style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { navController.navigate("game") }) {
            Text(text = "New Game", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { showAboutDialog = true }) {
            Text(text = "About", style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
