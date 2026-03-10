package com.korkoor.pardos.ui.game

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.BoardState
import kotlinx.coroutines.delay

@Composable
fun AccessibleGameScreen(
    viewModel: GameViewModel,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.boardState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val accessibilityManager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    val prefs = remember(context) {
        context.getSharedPreferences("pardos_accessibility", Context.MODE_PRIVATE)
    }

    val announce = remember(view, accessibilityManager) {
        { message: String ->
            if (accessibilityManager?.isEnabled == true && message.isNotBlank()) {
                view.announceForAccessibility(message)
            }
        }
    }

    fun startAccessibleMatch() {
        viewModel.setupCustomGame(
            size = 4,
            target = 128,
            allowPowerUps = false,
            difficulty = "Zen",
            level = 1,
            initialScore = 0,
            isCustom = true
        )
    }

    var tutorialVisible by rememberSaveable { mutableStateOf(false) }
    var tutorialStepIndex by rememberSaveable { mutableIntStateOf(0) }
    var showBoardDetails by rememberSaveable { mutableStateOf(false) }
    var lastSpawnedValue by rememberSaveable { mutableStateOf<Int?>(null) }

    val tutorialSteps = remember(state.levelLimit) { context.buildTutorialSteps(state) }

    fun closeTutorial() {
        tutorialVisible = false
        prefs.edit().putBoolean("tutorial_seen_v2", true).apply()
    }

    LaunchedEffect(Unit) {
        if (state.tiles.isEmpty()) {
            startAccessibleMatch()
        }
        announce(context.getString(R.string.accessible_mode_announce_started))

        val seenTutorial = prefs.getBoolean("tutorial_seen_v2", false)
        if (!seenTutorial) {
            tutorialVisible = true
            tutorialStepIndex = 0
            announce(context.getString(R.string.accessible_mode_announce_tutorial_opened))
        }
    }

    LaunchedEffect(tutorialVisible, tutorialStepIndex, tutorialSteps) {
        if (tutorialVisible && tutorialSteps.isNotEmpty()) {
            announce(tutorialSteps[tutorialStepIndex])
        }
    }

    var pendingDirection by remember { mutableStateOf<Direction?>(null) }
    var previousState by remember { mutableStateOf(AccessibleBoardSnapshot.from(state)) }

    val controlsEnabled = !state.isGameOver && !state.isLevelCompleted && !viewModel.showLevelSummary
    val objectiveText = remember(state.levelLimit, state.boardSize) { context.buildObjectiveSummary(state) }
    val statusText = remember(state) { context.buildStatusSummary(state) }
    val quickHelpText = remember(state.isLevelCompleted, state.isGameOver) { context.buildNextActionHint(state) }
    val boardRows = remember(state) { context.buildBoardRows(state) }
    val boardSummaryText = remember(boardRows) { boardRows.joinToString(separator = ". ") }
    val summaryText = remember(objectiveText, statusText, quickHelpText, lastSpawnedValue) {
        context.buildAccessibleSummary(objectiveText, statusText, quickHelpText, lastSpawnedValue)
    }

    LaunchedEffect(state.tiles, state.score, state.moveCount, state.isGameOver, state.isLevelCompleted) {
        pendingDirection?.let { direction ->
            val moved = state.moveCount > previousState.moveCount
            val directionLabel = context.getString(direction.toLabelRes())
            val newTileValue = state.tiles.firstOrNull { it.id !in previousState.tileIds }?.value

            if (moved) {
                announce(
                    context.getString(
                        R.string.accessible_mode_announce_move_success,
                        directionLabel,
                        state.score,
                        state.moveCount,
                        state.highestTile?.value ?: 0,
                        state.emptySpaces
                    )
                )

                if (newTileValue != null) {
                    lastSpawnedValue = newTileValue
                    val newTileMsg = if (newTileValue == 2) {
                        context.getString(R.string.accessible_mode_announce_new_tile_common_two)
                    } else {
                        context.getString(R.string.accessible_mode_announce_new_tile, newTileValue)
                    }
                    announce(newTileMsg)
                }
            } else {
                announce(
                    context.getString(
                        R.string.accessible_mode_announce_move_blocked,
                        directionLabel
                    )
                )
            }
            pendingDirection = null
        }
        previousState = AccessibleBoardSnapshot.from(state)
    }

    LaunchedEffect(pendingDirection) {
        val direction = pendingDirection ?: return@LaunchedEffect
        delay(220)
        if (pendingDirection == direction) {
            announce(
                context.getString(
                    R.string.accessible_mode_announce_move_blocked,
                    context.getString(direction.toLabelRes())
                )
            )
            pendingDirection = null
        }
    }

    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver) {
            announce(context.getString(R.string.accessible_mode_announce_game_over))
        }
    }

    LaunchedEffect(viewModel.showLevelSummary, state.isLevelCompleted) {
        if (viewModel.showLevelSummary || state.isLevelCompleted) {
            announce(context.getString(R.string.accessible_mode_announce_level_complete))
        }
    }

    fun requestMove(direction: Direction) {
        if (!controlsEnabled) return
        pendingDirection = direction
        viewModel.onMove(direction) { _ -> }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.accessible_mode_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.accessible_mode_subtitle_simple),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )

                OutlinedButton(
                    onClick = { announce(summaryText) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.accessible_mode_repeat_summary))
                }

                OutlinedButton(
                    onClick = {
                        tutorialStepIndex = 0
                        tutorialVisible = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.accessible_mode_open_tutorial))
                }

                OutlinedButton(
                    onClick = {
                        showBoardDetails = !showBoardDetails
                        val msg = if (showBoardDetails) {
                            context.getString(R.string.accessible_mode_board_shown)
                        } else {
                            context.getString(R.string.accessible_mode_board_hidden)
                        }
                        announce(msg)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showBoardDetails) {
                            stringResource(R.string.accessible_mode_hide_board)
                        } else {
                            stringResource(R.string.accessible_mode_show_board)
                        }
                    )
                }
            }
        }

        if (showBoardDetails) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.accessible_mode_board_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    boardRows.forEach { row ->
                        Text(text = row, style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = { announce(boardSummaryText) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.accessible_mode_repeat_board))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.accessible_mode_controls_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { requestMove(Direction.UP) },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.accessible_move_up))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { requestMove(Direction.LEFT) },
                enabled = controlsEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.accessible_move_left))
            }
            Button(
                onClick = { requestMove(Direction.RIGHT) },
                enabled = controlsEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.accessible_move_right))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { requestMove(Direction.DOWN) },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.accessible_move_down))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { startAccessibleMatch() },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.accessible_mode_new_match))
            }
            OutlinedButton(
                onClick = { announce(boardSummaryText) },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.accessible_mode_repeat_board))
            }
        }

        TextButton(
            onClick = onExitApp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.accessible_mode_exit_to_menu))
        }
    }

    if (tutorialVisible) {
        val stepTitle = stringResource(
            R.string.accessible_tutorial_step_title,
            tutorialStepIndex + 1,
            tutorialSteps.size
        )
        val stepText = tutorialSteps[tutorialStepIndex]

        AlertDialog(
            onDismissRequest = { closeTutorial() },
            title = { Text(text = stepTitle) },
            text = { Text(text = stepText) },
            confirmButton = {
                if (tutorialStepIndex < tutorialSteps.lastIndex) {
                    TextButton(onClick = { tutorialStepIndex += 1 }) {
                        Text(text = stringResource(R.string.accessible_tutorial_next))
                    }
                } else {
                    TextButton(onClick = { closeTutorial() }) {
                        Text(text = stringResource(R.string.accessible_tutorial_finish))
                    }
                }
            },
            dismissButton = {
                if (tutorialStepIndex > 0) {
                    TextButton(onClick = { tutorialStepIndex -= 1 }) {
                        Text(text = stringResource(R.string.accessible_tutorial_previous))
                    }
                } else {
                    TextButton(onClick = { closeTutorial() }) {
                        Text(text = stringResource(R.string.accessible_tutorial_close))
                    }
                }
            }
        )
    }

    if (viewModel.showLevelSummary || state.isLevelCompleted) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.accessible_mode_win_title)) },
            text = { Text(text = stringResource(R.string.accessible_mode_win_body)) },
            confirmButton = {
                TextButton(onClick = { startAccessibleMatch() }) {
                    Text(text = stringResource(R.string.accessible_mode_new_match))
                }
            },
            dismissButton = {
                TextButton(onClick = onExitApp) {
                    Text(text = stringResource(R.string.accessible_mode_exit_to_menu))
                }
            }
        )
    }

    if (state.isGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.accessible_mode_game_over_title)) },
            text = { Text(text = stringResource(R.string.accessible_mode_game_over_body)) },
            confirmButton = {
                TextButton(onClick = { startAccessibleMatch() }) {
                    Text(text = stringResource(R.string.accessible_mode_new_match))
                }
            },
            dismissButton = {
                TextButton(onClick = onExitApp) {
                    Text(text = stringResource(R.string.accessible_mode_exit_to_menu))
                }
            }
        )
    }
}

private fun Context.buildAccessibleSummary(
    objectiveText: String,
    statusText: String,
    quickHelpText: String,
    lastSpawnedValue: Int?
): String {
    val spawnPart = if (lastSpawnedValue != null) {
        getString(R.string.accessible_mode_last_spawn_value, lastSpawnedValue)
    } else {
        getString(R.string.accessible_mode_spawn_rule_short)
    }
    return listOf(objectiveText, statusText, quickHelpText, spawnPart).joinToString(separator = " ")
}

private fun Context.buildObjectiveSummary(state: BoardState): String {
    return getString(
        R.string.accessible_mode_objective_summary,
        state.levelLimit,
        state.boardSize,
        state.boardSize
    )
}

private fun Context.buildNextActionHint(state: BoardState): String {
    return when {
        state.isGameOver -> getString(R.string.accessible_mode_hint_game_over)
        state.isLevelCompleted -> getString(R.string.accessible_mode_hint_level_complete)
        else -> getString(R.string.accessible_mode_hint_playing)
    }
}

private fun Context.buildTutorialSteps(state: BoardState): List<String> {
    return listOf(
        getString(R.string.accessible_tutorial_step_1, state.levelLimit),
        getString(R.string.accessible_tutorial_step_2),
        getString(R.string.accessible_tutorial_step_3),
        getString(R.string.accessible_tutorial_step_4),
        getString(R.string.accessible_tutorial_step_5),
        getString(R.string.accessible_tutorial_step_6),
        getString(R.string.accessible_tutorial_step_7)
    )
}

private fun Context.buildStatusSummary(state: BoardState): String {
    return getString(
        R.string.accessible_mode_status_summary,
        state.score,
        state.moveCount,
        state.highestTile?.value ?: 0,
        state.emptySpaces
    )
}

private fun Context.buildBoardRows(state: BoardState): List<String> {
    val cellMap = state.tiles.associateBy { it.row to it.col }
    val emptyLabel = getString(R.string.accessible_mode_cell_empty)

    return (0 until state.boardSize).map { row ->
        val values = (0 until state.boardSize).joinToString(separator = ", ") { col ->
            cellMap[row to col]?.value?.toString() ?: emptyLabel
        }
        getString(R.string.accessible_mode_row_format, row + 1, values)
    }
}

private fun Direction.toLabelRes(): Int {
    return when (this) {
        Direction.UP -> R.string.accessible_move_up
        Direction.DOWN -> R.string.accessible_move_down
        Direction.LEFT -> R.string.accessible_move_left
        Direction.RIGHT -> R.string.accessible_move_right
    }
}

private data class AccessibleBoardSnapshot(
    val moveCount: Int,
    val tileIds: Set<String>
) {
    companion object {
        fun from(state: BoardState): AccessibleBoardSnapshot {
            return AccessibleBoardSnapshot(
                moveCount = state.moveCount,
                tileIds = state.tiles.map { it.id }.toSet()
            )
        }
    }
}
