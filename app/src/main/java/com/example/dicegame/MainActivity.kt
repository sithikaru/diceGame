package com.example.dicegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dicegame.ui.theme.DiceGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiceGameTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Dice Game", fontSize = 32.sp)

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { /* TODO: Navigate to Game Screen */ }) {
            Text(text = "New Game")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { showAboutDialog = true }) {
            Text(text = "About")
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "About") },
        text = {
            Text(
                text = """
                Name: Sithija Karunasena
                Student ID: 20230849

                "I confirm that I understand what plagiarism is and have read and understood 
                the section on Assessment Offences in the Essential Information for Students. 
                The work that I have submitted is entirely my own. Any work from other authors 
                is duly referenced and acknowledged."
                """.trimIndent()
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
