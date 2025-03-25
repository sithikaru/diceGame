package com.example.dicegame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dicegame.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // Error message for when no dice is selected for rerolling or max selection reached
    var errorMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Tracks if dice should appear in full color (active) or grayscale (inactive)
    var diceEnabled by remember { mutableStateOf(false) }

    // Tracks if the computer is busy re-rolling (for loading animation)
    var isComputerRolling by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

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

    // Helper to re-roll only dice that are NOT selected (for human)
    fun rerollDice(currentDice: List<Int>, selected: Set<Int>): List<Int> =
        currentDice.mapIndexed { index, value ->
            if (index in selected) value else Random.nextInt(1, 7)
        }

    /**
     * Intelligent computer re-roll strategy.
     *
     * This strategy uses the following rules:
     * - The computer always keeps dice showing 5 or 6 if it is trailing.
     * - If the computer is trailing and still needs many points (pointsNeeded > 20), it becomes even more aggressive,
     *   keeping only dice showing 6.
     * - If the computer is leading, it is more conservative and keeps dice showing 4 or higher.
     * - The computer's decision is based solely on its current dice values, its current score,
     *   the human’s score, and the target score.
     *
     * Advantages:
     * - When trailing, re-rolling more dice increases the chance of hitting high values (5s or 6s) to catch up.
     * - When leading, keeping moderately high dice (4 and above) minimizes risk.
     *
     * Disadvantages:
     * - The strategy is heuristic-based and may not always yield the optimal expected value,
     *   but it balances risk and reward with relatively low computational cost.
     */
    fun intelligentComputerReroll(
        currentDice: List<Int>,
        computerScore: Int,
        humanScore: Int,
        targetScore: Int
    ): List<Int> {
        val pointsNeeded = targetScore - computerScore
        val isTrailing = computerScore < humanScore
        val threshold = if (isTrailing) {
            if (pointsNeeded > 20) 6 else 5  // Aggressive if far behind: only keep 6; otherwise, keep 5 and 6.
        } else {
            4  // Conservative when leading: keep dice of value 4 or above.
        }
        return currentDice.map { die ->
            if (die < threshold) Random.nextInt(1, 7) else die
        }
    }

    suspend fun onScore() {
        // Once the user scores, disable dice (show them in grayscale)
        diceEnabled = false

        if (!tieBreaker) {
            // Simulate computer re-rolls with a loading animation.
            isComputerRolling = true
            while (computerRollCount < 3) {
                computerDice = intelligentComputerReroll(computerDice, computerScore, humanScore, targetScore)
                computerRollCount++
                delay(500L) // Delay to simulate re-roll animation
            }
            isComputerRolling = false
        }
        // Add turn scores (sum of dice)
        humanScore += humanDice.sum()
        computerScore += computerDice.sum()
        humanTurnCount++
        computerTurnCount++

        // Check if win condition is met (only check after both players completed the turn)
        if (humanScore >= targetScore || computerScore >= targetScore) {
            if (humanTurnCount == computerTurnCount) {
                if (humanScore == computerScore) {
                    tieBreaker = true
                } else {
                    determineWinner()
                }
            } else {
                tieBreaker = true
            }
        }

        // Reset for next turn (if not in tie-breaker mode)
        humanRollCount = 0
        computerRollCount = 0
        selectedDice = emptySet()
    }

    // Handler for the Throw/Re-roll button
    fun onThrow() {
        errorMessage = "" // reset any previous error

        // As soon as the user hits Throw, we enable dice for selection
        diceEnabled = true

        if (!tieBreaker) {
            if (humanRollCount == 0) {
                // First roll: roll all dice for both players
                humanDice = rollAllDice()
                computerDice = rollAllDice()
                humanRollCount = 1
                computerRollCount = 1
            } else if (humanRollCount in 1 until 3) {
                // Before re-roll, ensure human selected at least one die to hold.
                if (selectedDice.isEmpty()) {
                    errorMessage = "Please select at least one die to hold before re-rolling."
                } else {
                    // Human re-roll: only re-roll dice that are not selected.
                    humanDice = rerollDice(humanDice, selectedDice)
                    humanRollCount++
                    // Clear the selected dice for next optional re-roll (if any)
                    selectedDice = emptySet()

                    // AUTO-SCORE: If maximum (3 rolls) reached, automatically update score.
                    if (humanRollCount == 3) {
                        coroutineScope.launch { onScore() }
                    }
                }
            }
        } else {
            // Tie-breaker: always roll all dice
            humanDice = rollAllDice()
            computerDice = rollAllDice()
        }
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


    // UI Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dice Game", color = Color.White) },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFF6200EE)
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                        if (!tieBreaker && humanRollCount in 1 until 3) {
                            // Allow unselecting even if 4 dice are already selected.
                            if (index in selectedDice) {
                                selectedDice = selectedDice - index
                            } else {
                                if (selectedDice.size < 4) {
                                    selectedDice = selectedDice + index
                                } else {
                                    errorMessage = "Maximum of 4 dice can be selected."
                                }
                            }
                        }
                    },
                    isDiceEnabled = diceEnabled
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
                    onDiceClick = { /* no-op for computer dice */ },
                    isDiceEnabled = diceEnabled
                )
                Spacer(modifier = Modifier.height(24.dp))
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
                        // Score button is disabled when the computer is rolling
                        Button(
                            onClick = {
                                coroutineScope.launch { onScore() }
                            },
                            enabled = !isComputerRolling
                        ) {
                            Text(text = "Score")
                        }
                    }
                } else {
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
            // Loading animation overlay while computer is re-rolling
            if (isComputerRolling) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Displays a row of dice. Each die can be highlighted if selected and may be displayed in grayscale (disabled).
 */
@Composable
fun DiceRow(
    diceValues: List<Int>,
    selectedDice: Set<Int>,
    onDiceClick: (Int) -> Unit,
    isDiceEnabled: Boolean
) {
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val grayscaleColorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        diceValues.forEachIndexed { index, value ->
            val modifier = Modifier
                .size(64.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (index in selectedDice && isDiceEnabled)
                        Modifier.border(BorderStroke(3.dp, Color.Green), RoundedCornerShape(8.dp))
                    else Modifier
                )
                .clickable(enabled = isDiceEnabled) { onDiceClick(index) }
            Image(
                painter = painterResource(id = getDiceImage(value)),
                contentDescription = "Dice showing $value",
                modifier = modifier,
                colorFilter = if (!isDiceEnabled) grayscaleColorFilter else null
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

/*
----------------------- Documentation of the Intelligent Computer Strategy -----------------------

Strategy Overview:
------------------
The computer’s re-roll strategy is designed to be efficient and risk-aware, considering both its current game state and the target score.

Rules:
1. When the computer is trailing (its score is less than the human's):
   - It adopts an aggressive strategy.
   - It only keeps dice that show high values.
   - If many points are needed (pointsNeeded > 20), it keeps only dice showing 6.
   - Otherwise, it keeps dice showing 5 or 6.
2. When the computer is leading:
   - It plays conservatively.
   - It keeps dice showing 4 or above.
3. The decision is based on:
   - The computer’s current score.
   - The human player’s current score.
   - The target score (which determines how many points are still needed).

Justification:
--------------
- By re-rolling lower dice, the computer increases its chance to hit a higher total when trailing.
- When leading, minimizing risk by keeping moderately high dice (≥ 4) helps maintain its advantage.
- The strategy factors in the urgency (points needed) to adjust its risk tolerance.
- This approach is computationally simple yet mathematically grounded in the concept of expected value, balancing risk and reward.

Advantages:
-----------
- Adaptive: It changes its behavior based on whether it’s trailing or leading.
- Efficient: It uses a simple threshold-based approach to decide which dice to keep.
- Risk-Aware: It increases the chance of catching up when behind by being more aggressive.

Disadvantages:
--------------
- Heuristic-Based: While it uses expected value concepts, it is not an exhaustive optimal solution.
- Limited to 3 Rolls: The strategy works within the constraints of the game (maximum 3 rolls per turn).

-----------------------------------------------------------------------------------------------
*/

