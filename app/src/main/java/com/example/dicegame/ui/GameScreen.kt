package com.example.dicegame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dicegame.R
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(navController: NavController) {
    // States for dice values
    var humanDice by remember { mutableStateOf(List(5) { 0 }) }
    var computerDice by remember { mutableStateOf(List(5) { 0 }) }

    // Scores and wins
    var humanScore by remember { mutableStateOf(0) }
    var computerScore by remember { mutableStateOf(0) }
    var totalHumanWins by remember { mutableStateOf(0) }
    var totalComputerWins by remember { mutableStateOf(0) }

    // Turn counters and reroll counters
    var humanTurnCount by remember { mutableStateOf(0) }
    var computerTurnCount by remember { mutableStateOf(0) }
    var humanRollCount by remember { mutableStateOf(0) } // counts current turn rolls (max 3)
    var computerRollCount by remember { mutableStateOf(0) } // counts current turn rolls (max 3)

    // For holding selected dice indexes (human)
    var selectedDice by remember { mutableStateOf(setOf<Int>()) }

    // Target score and tie-breaker flag
    var targetScore by remember { mutableStateOf(101) }
    var showTargetDialog by remember { mutableStateOf(true) }
    var tieBreaker by remember { mutableStateOf(false) }

    // Error message for when no dice is selected for rerolling
    var errorMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Determines winner after both turns are complete or during tie-breaker rounds.
    fun determineWinner() {
        when {
            humanScore > computerScore -> totalHumanWins++
            computerScore > humanScore -> totalComputerWins++
            // If scores are equal, we remain in tie-breaker mode.
        }
    }

    // Helper to perform a full roll of 5 dice
    fun rollAllDice(): List<Int> = List(5) { Random.nextInt(1, 7) }

    // Helper to re-roll only dice that are NOT selected
    fun rerollDice(currentDice: List<Int>, selected: Set<Int>): List<Int> =
        currentDice.mapIndexed { index, value ->
            if (index in selected) value else Random.nextInt(1, 7)
        }

    // Computer's optional reroll strategy (random selection)
    fun computerReroll(currentDice: List<Int>): List<Int> {
        // For each die, randomly decide to keep (true) or re-roll (false)
        val diceToKeep = List(5) { Random.nextBoolean() }
        return currentDice.mapIndexed { index, value ->
            if (diceToKeep[index]) value else Random.nextInt(1, 7)
        }
    }

    // Handler for the Throw/Re-roll button
    fun onThrow() {
        errorMessage = "" // reset any previous error
        if (!tieBreaker) {
            if (humanRollCount == 0) {
                // First roll: roll all dice for both players
                humanDice = rollAllDice()
                computerDice = rollAllDice()
                humanRollCount = 1
                computerRollCount = 1
            } else if (humanRollCount in 1 until 3) {
                // Before reroll, ensure human selected at least one die to keep.
                if (selectedDice.isEmpty()) {
                    errorMessage = "Please select at least one die to hold before re-rolling."
                } else {
                    // Human re-roll: only re-roll dice that are not selected.
                    humanDice = rerollDice(humanDice, selectedDice)
                    humanRollCount++
                    // For computer: simulate its optional re-roll if available
                    if (computerRollCount < 3) {
                        computerDice = computerReroll(computerDice)
                        computerRollCount++
                    }
                    // Clear the selected dice for next optional re-roll (if any)
                    selectedDice = emptySet()
                }
            }
        } else {
            // Tie-breaker: always roll all dice
            humanDice = rollAllDice()
            computerDice = rollAllDice()
        }
    }

    // Handler for the Score button – ends current turn and updates overall scores.
    fun onScore() {
        if (!tieBreaker) {
            // If computer still has optional re-rolls, simulate them until computerRollCount == 3.
            while (computerRollCount < 3) {
                computerDice = computerReroll(computerDice)
                computerRollCount++
            }
        }
        // Add turn scores (sum of dice)
        humanScore += humanDice.sum()
        computerScore += computerDice.sum()
        humanTurnCount++
        computerTurnCount++

        // Check if win condition is met (only check after both players completed the turn)
        if (humanScore >= targetScore || computerScore >= targetScore) {
            // If both players had the same number of turns, see if there's a winner or tie.
            if (humanTurnCount == computerTurnCount) {
                if (humanScore == computerScore) {
                    tieBreaker = true
                } else {
                    determineWinner()
                }
            } else {
                // If turn counts are different, force tie-breaker.
                tieBreaker = true
            }
        }

        // Reset for next turn (if not in tie-breaker mode)
        humanRollCount = 0
        computerRollCount = 0
        selectedDice = emptySet()
    }

    // Display a Snackbar for any error messages.
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Short)
            errorMessage = ""
        }
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss until confirmed */ },
            title = { Text("Set Target Score") },
            text = {
                Column {
                    Text("Enter a target score to win (default: 101):")
                    TextField(
                        value = if (targetScore == 101) "" else targetScore.toString(),
                        onValueChange = { newValue ->
                            targetScore = newValue.toIntOrNull() ?: 101
                        },
                        placeholder = { Text("101") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTargetDialog = false }) {
                    Text("Start Game")
                }
            }
        )
    }

    // Decide what label to use for the Throw/Re-roll button
    val throwButtonLabel = when {
        tieBreaker -> "Tie Breaker Roll!"
        humanRollCount == 0 -> "Throw"
        else -> "Re-roll"
    }

    // Decide whether the throw button should be enabled
    val isThrowButtonEnabled = when {
        tieBreaker -> true   // Always allow rethrow in tie-breaker
        humanRollCount < 3 -> true
        else -> false
    }

    Scaffold(
        topBar = {
            // A colored top bar for a more appealing UI
            TopAppBar(
                title = { Text("Dice Game", color = Color.White) },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFF6200EE) // A purple shade
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Display total wins
            Text(
                text = "H: $totalHumanWins  /  C: $totalComputerWins",
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display current scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(text = "Player Score: $humanScore")
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Computer Score: $computerScore")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dice display
            Text(
                text = "Player's Dice",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium
            )
            DiceRow(
                diceValues = humanDice,
                selectedDice = selectedDice,
                onDiceClick = { index ->
                    // Only allow selection if within allowed re-roll rounds and not in tie-breaker
                    if (!tieBreaker && humanRollCount in 1 until 3) {
                        selectedDice = if (index in selectedDice) {
                            selectedDice - index
                        } else {
                            selectedDice + index
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Computer's Dice",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium
            )
            DiceRow(
                diceValues = computerDice,
                selectedDice = emptySet(),
                onDiceClick = { /* no-op for computer dice */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Determine if game is over (without tie-breaker)
            val gameOverNoTie = (humanScore >= targetScore || computerScore >= targetScore) && !tieBreaker

            if (!gameOverNoTie) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { onThrow() },
                        enabled = isThrowButtonEnabled
                    ) {
                        Text(text = throwButtonLabel)
                    }
                    Button(onClick = { onScore() }) {
                        Text(text = "Score")
                    }
                }
            } else {
                // Game over UI
                val winMessage = if (humanScore > computerScore) "You Win! 🎉" else "You Lose! 😢"
                val messageColor = if (humanScore > computerScore) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Text(text = winMessage, fontSize = 24.sp, color = messageColor)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text(text = "Back to Home")
            }
        }
    }
}

@Composable
fun DiceRow(
    diceValues: List<Int>,
    selectedDice: Set<Int>,
    onDiceClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        diceValues.forEachIndexed { index, value ->
            // When a die is selected, add a highlighted border.
            val modifier = Modifier
                .size(64.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (index in selectedDice)
                        Modifier.border(BorderStroke(3.dp, Color.Green), RoundedCornerShape(8.dp))
                    else Modifier
                )
                .clickable { onDiceClick(index) }

            Image(
                painter = painterResource(id = getDiceImage(value)),
                contentDescription = "Dice showing $value",
                modifier = modifier
            )
        }
    }
}

fun getDiceImage(value: Int): Int {
    return when (value) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        6 -> R.drawable.dice_6
        else -> R.drawable.dice_1
    }
}