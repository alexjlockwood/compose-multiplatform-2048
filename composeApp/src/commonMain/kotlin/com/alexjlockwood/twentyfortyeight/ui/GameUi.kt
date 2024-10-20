package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Renders the 2048 game's home screen UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameUi(
    gridTileMovements: List<GridTileMovement>,
    currentScore: Int,
    bestScore: Int,
    isGameOver: Boolean,
    onNewGameRequest: () -> Unit,
    onSwipeListener: (direction: Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var shouldShowNewGameDialog by remember { mutableStateOf(false) }
    var swipeAngle by remember { mutableDoubleStateOf(0.0) }
    var activeKeyDown by remember { mutableStateOf<Key?>(null) }

    Scaffold(
        modifier = modifier
            .onKeyEvent {
                when (it.type) {
                    KeyEventType.KeyDown -> {
                        val direction = it.direction
                        if (activeKeyDown == null && direction != null) {
                            activeKeyDown = it.key
                            onSwipeListener(direction)
                        }
                    }

                    KeyEventType.KeyUp -> {
                        if (it.key == activeKeyDown) {
                            activeKeyDown = null
                        }
                    }
                }

                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        topBar = {
            val title = @Composable { Text("2048") }
            val actions = @Composable {
                IconButton(onClick = { shouldShowNewGameDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
            if (shouldCenterAlignTopAppBar()) {
                CenterAlignedTopAppBar(
                    title = title,
                    actions = { actions() },
                )
            } else {
                TopAppBar(
                    title = title,
                    actions = { actions() },
                )
            }
        },
    ) { innerPadding ->
        GameLayout(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .then(
                    if (shouldDetectSwipes()) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    swipeAngle = with(dragAmount) { (atan2(-y, x) * 180 / PI + 360) % 360 }
                                },
                                onDragEnd = {
                                    onSwipeListener(
                                        when {
                                            45 <= swipeAngle && swipeAngle < 135 -> Direction.NORTH
                                            135 <= swipeAngle && swipeAngle < 225 -> Direction.WEST
                                            225 <= swipeAngle && swipeAngle < 315 -> Direction.SOUTH
                                            else -> Direction.EAST
                                        },
                                    )
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
            gameGrid = { gridSize -> GameGrid(gridTileMovements = gridTileMovements, gridSize = gridSize) },
            currentScoreText = { TextLabel(text = "$currentScore", fontSize = 36.sp) },
            currentScoreLabel = { TextLabel(text = "SCORE", fontSize = 18.sp) },
            bestScoreText = { TextLabel(text = "$bestScore", fontSize = 36.sp) },
            bestScoreLabel = { TextLabel(text = "BEST", fontSize = 18.sp) },
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (isGameOver) {
        GameDialog(
            title = "Game over",
            message = "Start a new game?",
            onConfirmListener = { onNewGameRequest() },
            onDismissListener = null,
        )
    } else if (shouldShowNewGameDialog) {
        GameDialog(
            title = "Start a new game?",
            message = "Starting a new game will erase your current game.",
            onConfirmListener = {
                onNewGameRequest()
                shouldShowNewGameDialog = false
            },
            onDismissListener = { shouldShowNewGameDialog = false },
        )
    }
}

@Composable
private fun TextLabel(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = FontWeight.Light,
    )
}

private val KeyEvent.direction: Direction?
    get() = when (key) {
        Key.DirectionUp, Key.W -> Direction.NORTH
        Key.DirectionLeft, Key.A -> Direction.WEST
        Key.DirectionDown, Key.S -> Direction.SOUTH
        Key.DirectionRight, Key.D -> Direction.EAST
        else -> null
    }

/**
 * Returns true if the platform should support moves via touch gestures.
 */
internal expect fun shouldDetectSwipes(): Boolean

/**
 * Returns true if the top app bar should be center aligned.
 */
internal expect fun shouldCenterAlignTopAppBar(): Boolean
