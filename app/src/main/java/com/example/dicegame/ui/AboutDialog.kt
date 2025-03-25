package com.example.dicegame.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "About", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                text = """Name: Sithija Karunasena
Student ID: 20230849/w2084792

'I confirm that I understand what plagiarism is and have read and understood the section on Assessment Offences in the Essential Information for Students. The work that I have submitted is entirely my own. Any work from other authors is duly referenced and acknowledged.'""",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
