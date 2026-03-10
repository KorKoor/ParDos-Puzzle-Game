package com.korkoor.pardos.ui.menu

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.R

@Composable
fun AccessibleMenuScreen(
    onPlayClick: () -> Unit,
    onCustomClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val accessibilityManager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }

    LaunchedEffect(Unit) {
        if (accessibilityManager?.isEnabled == true) {
            view.announceForAccessibility(context.getString(R.string.accessible_menu_announce_ready))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.accessible_menu_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.accessible_menu_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(onClick = onPlayClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.menu_play))
        }
        Button(onClick = onCustomClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.menu_customize))
        }
        Button(onClick = onDailyChallengeClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.menu_daily_challenge))
        }
        Button(onClick = onProfileClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.menu_profile))
        }
        Button(onClick = onFriendsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.menu_friends))
        }
        Button(onClick = onRecordsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.menu_records))
        }
        Button(onClick = onAchievementsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.menu_achievements))
        }
    }
}
