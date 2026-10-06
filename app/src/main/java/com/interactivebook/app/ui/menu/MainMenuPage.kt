package com.interactivebook.app.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactivebook.app.R
import com.interactivebook.app.ui.common.AssetImage

private val MenuInk = Color(0xFF3B2A1A)
private val MenuInkDim = Color(0xFF6B5A44)

@Composable
fun MainMenuPage(
    hasSavedGame: Boolean,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    var showNewGameConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AssetImage(
            fileName = "book_open.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 130.dp, end = 28.dp, top = 140.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            MenuItem(
                label = stringResource(R.string.menu_continue),
                enabled = hasSavedGame,
                onClick = onContinue
            )
            Spacer(Modifier.height(20.dp))
            MenuItem(
                label = stringResource(R.string.menu_new_game),
                enabled = true,
                onClick = {
                    if (hasSavedGame) showNewGameConfirm = true else onNewGame()
                }
            )
            Spacer(Modifier.height(20.dp))
            MenuItem(
                label = stringResource(R.string.menu_settings),
                enabled = true,
                onClick = onSettings
            )
            Spacer(Modifier.height(20.dp))
            MenuItem(
                label = stringResource(R.string.menu_exit),
                enabled = true,
                onClick = onExit
            )
        }
    }

    if (showNewGameConfirm) {
        AlertDialog(
            onDismissRequest = { showNewGameConfirm = false },
            title = { Text(stringResource(R.string.menu_new_game_confirm_title)) },
            text = { Text(stringResource(R.string.menu_new_game_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showNewGameConfirm = false
                    onNewGame()
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun MenuItem(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MenuInk else MenuInkDim.copy(alpha = 0.5f),
            textAlign = TextAlign.Start
        )
    }
}