package com.example.dicegame.ui

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dicegame.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GameScreen(navController: NavController) {
    // -------------------------------------------------------------------
    // Game State Variables & Logic
    // (Same as your code – unchanged)
    // -------------------------------------------------------------------
    var humanDice by remember { mutableStateOf(List(5) { 0 }) }
    var computerDice by remember { mutableStateOf(List(5) { 0 }) }

    var humanScore by remember { mutableStateOf(0) }
    var computerScore by remember { mutableStateOf(0) }
    var totalHumanWins by remember { mutableStateOf(0) }
    var totalComputerWins by remember { mutableStateOf(0) }

    var humanTurnCount by remember { mutableStateOf(0) }
    var computerTurnCount by remember { mutableStateOf(0) }
    var humanRollCount by remember { mutableStateOf(0) }
    var computerRollCount by remember { mutableStateOf(0) }

    var selectedDice by remember { mutableStateOf(setOf<Int>()) }

    var targetScore by remember { mutableStateOf(101) }
    var showTargetDialog by remember { mutableStateOf(true) }
    var tieBreaker by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    var diceEnabled by remember { mutableStateOf(false) }
    var isComputerRolling by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Orientation detection
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // -------------------------------------------------------------------
    // Functions (Intelligent Logic)
    // (Same as your code – unchanged)
    // -------------------------------------------------------------------
    fun intelligentComputerReroll(
        currentDice: List<Int>,
        computerScore: Int,
        humanScore: Int,
        targetScore: Int
    ): List<Int> {
        val pointsNeeded = targetScore - computerScore
        val isTrailing = computerScore < humanScore
        val threshold = if (isTrailing) {
            if (pointsNeeded > 20) 6 else 5
        } else {
            4
        }
        return currentDice.map { die ->
            if (die < threshold) Random.nextInt(1, 7) else die
        }
    }

    fun determineWinner() {
        when {
            humanScore > computerScore -> totalHumanWins++
            computerScore > humanScore -> totalComputerWins++
            // remain in tie-breaker if still equal
        }
    }

    suspend fun onScore() {
        // Disable dice
        diceEnabled = false

        // Computer's optional rerolls
        if (!tieBreaker) {
            isComputerRolling = true
            while (computerRollCount < 3) {
                computerDice = intelligentComputerReroll(computerDice, computerScore, humanScore, targetScore)
                computerRollCount++
                delay(500L)
            }
            isComputerRolling = false
        }

        // Add turn scores
        humanScore += humanDice.sum()
        computerScore += computerDice.sum()
        humanTurnCount++
        computerTurnCount++

        // Check if someone reached the target
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

        // Reset for next turn
        humanRollCount = 0
        computerRollCount = 0
        selectedDice = emptySet()
    }

    fun rollAllDice(): List<Int> = List(5) { Random.nextInt(1, 7) }

    fun rerollDice(currentDice: List<Int>, selected: Set<Int>): List<Int> =
        currentDice.mapIndexed { index, value ->
            if (index in selected) value else Random.nextInt(1, 7)
        }

    fun onThrow() {
        errorMessage = ""
        diceEnabled = true

        if (!tieBreaker) {
            if (humanRollCount == 0) {
                // First roll
                humanDice = rollAllDice()
                computerDice = rollAllDice()
                humanRollCount = 1
                computerRollCount = 1
            } else if (humanRollCount in 1 until 3) {
                if (selectedDice.isEmpty()) {
                    errorMessage = "Please select at least one die to hold before re-rolling."
                } else {
                    humanDice = rerollDice(humanDice, selectedDice)
                    humanRollCount++
                    selectedDice = emptySet()

                    // If 3rd roll, auto-score
                    if (humanRollCount == 3) {
                        coroutineScope.launch { onScore() }
                    }
                }
            }
        } else {
            // Tie-breaker: single roll
            humanDice = rollAllDice()
            computerDice = rollAllDice()
        }
    }

    // -------------------------------------------------------------------
    // Snackbar for Error Messages
    // -------------------------------------------------------------------
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Short)
            errorMessage = ""
        }
    }

    // -------------------------------------------------------------------
    // Show Target Dialog (First Start)
    // -------------------------------------------------------------------
    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { /* do nothing */ },
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

    // -------------------------------------------------------------------
    // Decide the label for the Throw/Re-roll button
    // -------------------------------------------------------------------
    val throwButtonLabel = when {
        tieBreaker -> "Tie Breaker Roll!"
        humanRollCount == 0 -> "Throw"
        else -> "Re-roll"
    }

    // Decide whether the throw button should be enabled
    val isThrowButtonEnabled = when {
        tieBreaker -> true
        humanRollCount < 3 -> true
        else -> false
    }

    // Check if game is over (without tie-breaker)
    val gameOverNoTie = (humanScore >= targetScore || computerScore >= targetScore) && !tieBreaker

    // -------------------------------------------------------------------
    // Main UI: Two separate layouts for portrait vs. landscape
    // -------------------------------------------------------------------
    if (isLandscape) {
        // LANDSCAPE LAYOUT
        LandscapeLayout(
            navController = navController,
            // pass in relevant states and functions
            humanDice = humanDice,
            computerDice = computerDice,
            humanScore = humanScore,
            computerScore = computerScore,
            totalHumanWins = totalHumanWins,
            totalComputerWins = totalComputerWins,
            targetScore = targetScore,
            throwButtonLabel = throwButtonLabel,
            isThrowButtonEnabled = isThrowButtonEnabled,
            diceEnabled = diceEnabled,
            isComputerRolling = isComputerRolling,
            gameOverNoTie = gameOverNoTie,
            tieBreaker = tieBreaker,
            selectedDice = selectedDice,
            humanRollCount = humanRollCount,
            onDiceSelected = { index ->
                if (!tieBreaker && humanRollCount in 1 until 3 && diceEnabled) {
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
            onThrow = ::onThrow,
            onScore = {
                coroutineScope.launch { onScore() }
            },
            snackbarHostState = snackbarHostState
        )
    } else {
        // PORTRAIT LAYOUT
        PortraitLayout(
            navController = navController,
            humanDice = humanDice,
            computerDice = computerDice,
            humanScore = humanScore,
            computerScore = computerScore,
            totalHumanWins = totalHumanWins,
            totalComputerWins = totalComputerWins,
            targetScore = targetScore,
            throwButtonLabel = throwButtonLabel,
            isThrowButtonEnabled = isThrowButtonEnabled,
            diceEnabled = diceEnabled,
            isComputerRolling = isComputerRolling,
            gameOverNoTie = gameOverNoTie,
            tieBreaker = tieBreaker,
            selectedDice = selectedDice,
            humanRollCount = humanRollCount,
            onDiceSelected = { index ->
                if (!tieBreaker && humanRollCount in 1 until 3 && diceEnabled) {
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
            onThrow = ::onThrow,
            onScore = {
                coroutineScope.launch { onScore() }
            },
            snackbarHostState = snackbarHostState
        )
    }
}

/**
 * ---------------------------------------------------
 *  LANDSCAPE LAYOUT
 *  Left side: Wooden box with dice
 *  Right side: scoreboard info & buttons (stacked)
 * ---------------------------------------------------
 */
@Composable
fun LandscapeLayout(
    navController: NavController,
    humanDice: List<Int>,
    computerDice: List<Int>,
    humanScore: Int,
    computerScore: Int,
    totalHumanWins: Int,
    totalComputerWins: Int,
    targetScore: Int,
    throwButtonLabel: String,
    isThrowButtonEnabled: Boolean,
    diceEnabled: Boolean,
    isComputerRolling: Boolean,
    gameOverNoTie: Boolean,
    tieBreaker: Boolean,
    selectedDice: Set<Int>,
    humanRollCount: Int,
    onDiceSelected: (Int) -> Unit,
    onThrow: () -> Unit,
    onScore: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT SIDE: Wooden Box with dice
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.wood),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
                // Dice layout inside
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Computer label + dice
                    Text(
                        text = "Computer - $computerScore",
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiceRow(
                        diceValues = computerDice,
                        selectedDice = emptySet(),
                        onDiceClick = { /* no-op for computer dice */ },
                        isDiceEnabled = diceEnabled
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Human dice
                    DiceRow(
                        diceValues = humanDice,
                        selectedDice = selectedDice,
                        onDiceClick = onDiceSelected,
                        isDiceEnabled = diceEnabled
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Human - $humanScore",
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                }
            }

            // RIGHT SIDE: scoreboard info & buttons stacked vertically
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Rounds + dice game label
                Text(
                    text = "Rounds won | H:$totalHumanWins / C:$totalComputerWins",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dice Game",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Target Score - $targetScore",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                if (!gameOverNoTie) {
                    // "Throw"/"Re-roll" black button
                    Button(
                        onClick = { onThrow() },
                        enabled = isThrowButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(text = throwButtonLabel)
                    }

                    // "Score" green button
                    Button(
                        onClick = onScore,
                        enabled = diceEnabled && !isComputerRolling,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853), // bright green
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(text = "Score")
                    }

                    // "Back to Home" (red or any color you want)
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Back to Home")
                    }
                } else {
                    // Game Over
                    val winMessage = if (humanScore > computerScore) "You Win!" else "You Lose!"
                    val messageColor = if (humanScore > computerScore) Color(0xFF388E3C) else Color.Red

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = winMessage, fontSize = 24.sp, color = messageColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Back to Home")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Loading animation overlay while computer is re-rolling
                if (isComputerRolling) {
                    Text(text = "Computer is rolling...", color = Color.Gray)
                }
            }
        }

        // If computer is rolling, we can optionally overlay a spinner
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

        // Snackbar at bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * ---------------------------------------------------
 *  PORTRAIT LAYOUT (Your existing code, slightly refactored)
 * ---------------------------------------------------
 */
@Composable
fun PortraitLayout(
    navController: NavController,
    humanDice: List<Int>,
    computerDice: List<Int>,
    humanScore: Int,
    computerScore: Int,
    totalHumanWins: Int,
    totalComputerWins: Int,
    targetScore: Int,
    throwButtonLabel: String,
    isThrowButtonEnabled: Boolean,
    diceEnabled: Boolean,
    isComputerRolling: Boolean,
    gameOverNoTie: Boolean,
    tieBreaker: Boolean,
    selectedDice: Set<Int>,
    humanRollCount: Int,
    onDiceSelected: (Int) -> Unit,
    onThrow: () -> Unit,
    onScore: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Same portrait code you used before, just grouped in one composable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {}
            // Top row: Rounds + Dice Game
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rounds won | H:$totalHumanWins / C:$totalComputerWins",
                    fontSize = 16.sp
                )
                Text(
                    text = "Dice Game",
                    fontSize = 20.sp
                )
            }

            // Target Score
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Target Score - $targetScore",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wooden box with dice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.wood),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Computer - $computerScore",
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiceRow(
                        diceValues = computerDice,
                        selectedDice = emptySet(),
                        onDiceClick = { /* no-op for computer dice */ },
                        isDiceEnabled = diceEnabled
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    DiceRow(
                        diceValues = humanDice,
                        selectedDice = selectedDice,
                        onDiceClick = onDiceSelected,
                        isDiceEnabled = diceEnabled
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Human - $humanScore",
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!gameOverNoTie) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onThrow,
                        enabled = isThrowButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Text(text = throwButtonLabel)
                    }
                    Button(
                        onClick = onScore,
                        enabled = diceEnabled && !isComputerRolling,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Text(text = "Score")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Back to Home")
                }
            } else {
                val winMessage = if (humanScore > computerScore) "You Win!" else "You Lose!"
                val messageColor = if (humanScore > computerScore) Color(0xFF388E3C) else Color.Red

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = winMessage, fontSize = 24.sp, color = messageColor)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Back to Home")
                }

            }
        }

        // Overlay while computer is re-rolling
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * DiceRow remains the same
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
                .size(72.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (index in selectedDice && isDiceEnabled)
                        Modifier.border(BorderStroke(4.dp, Color.Green), RoundedCornerShape(8.dp))
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
[Same as before...]

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

