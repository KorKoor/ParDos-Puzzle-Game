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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    LaunchedEffect(Unit) {
        startAccessibleMatch()
        announce(context.getString(R.string.accessible_mode_announce_started))
    }

    var pendingDirection by remember { mutableStateOf<Direction?>(null) }
    var previousState by remember { mutableStateOf(AccessibleBoardSnapshot.from(state)) }

    val controlsEnabled = !state.isGameOver && !state.isLevelCompleted && !viewModel.showLevelSummary
    val statusText = remember(state) { context.buildStatusSummary(state) }
    val boardRows = remember(state) { context.buildBoardRows(state) }

    LaunchedEffect(state.tiles, state.score, state.moveCount, state.isGameOver, state.isLevelCompleted) {
        pendingDirection?.let { direction ->
            val moved = state.moveCount > previousState.moveCount
            val directionLabel = context.getString(direction.toLabelRes())

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
            text = stringResource(R.string.accessible_mode_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { announce(statusText) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.accessible_mode_repeat_status))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.accessible_mode_board_title),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                boardRows.forEach { row ->
                    Text(text = row, style = MaterialTheme.typography.bodyMedium)
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

        OutlinedButton(
            onClick = { startAccessibleMatch() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.accessible_mode_new_match))
        }
        TextButton(
            onClick = onExitApp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.accessible_mode_exit_app))
        }
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
                    Text(text = stringResource(R.string.accessible_mode_exit_app))
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
                    Text(text = stringResource(R.string.accessible_mode_exit_app))
                }
            }
        )
    }
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
    val score: Int,
    val tiles: List<String>
) {
    companion object {
        fun from(state: BoardState): AccessibleBoardSnapshot {
            return AccessibleBoardSnapshot(
                moveCount = state.moveCount,
                score = state.score,
                tiles = state.tiles
                    .sortedWith(compareBy({ it.row }, { it.col }, { it.id }))
                    .map { "${it.row},${it.col}:${it.value}" }
            )
        }
    }
}
