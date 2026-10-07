package com.interactivebook.app.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.interactivebook.app.R
import com.interactivebook.app.ui.common.AssetImage

/**
 * Главное меню — страница открытой книги с НАРИСОВАННЫМ текстом (menu_list.png).
 *
 * Поверх картинки — четыре НЕВИДИМЫЕ зоны нажатия, каждая под своей строкой меню.
 * Координаты зон заданы в долях от высоты экрана (weight). При изменении картинки
 * меню — подстроить доли.
 *
 * Зоны (сверху вниз):
 *   1. Продолжить читать
 *   2. Новая История
 *   3. Настройки
 *   4. Выход из игры
 */
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
        // Слой 0: фон — текстура дерева.
        AssetImage(
            fileName = "background.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Слой 1: открытая книга.
        AssetImage(
            fileName = "book_open.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Слой 2: картинка меню — четыре нарисованные строки.
        // Размещена на правой странице книги.
        AssetImage(
            fileName = "menu_list.png",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 130.dp, end = 30.dp, top = 130.dp, bottom = 60.dp),
            contentScale = ContentScale.Fit
        )

        // Слой 3: невидимые зоны нажатия — поверх картинки меню.
        // Пропорции делят область картинки на четыре равные полосы.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 130.dp, end = 30.dp, top = 130.dp, bottom = 60.dp)
        ) {
            // Зона 1: Продолжить читать.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        enabled = hasSavedGame,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onContinue
                    )
            )
            // Зона 2: Новая История.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (hasSavedGame) showNewGameConfirm = true else onNewGame()
                        }
                    )
            )
            // Зона 3: Настройки.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSettings
                    )
            )
            // Зона 4: Выход из игры.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExit
                    )
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