package com.interactivebook.app.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.interactivebook.app.R
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent

private const val MENU_TOP_ALPHA_DISABLED = 0.45f
private const val MENU_TOP_ALPHA_ENABLED = 1.0f

/**
 * Главное меню — страница открытой книги.
 *
 * Меню состоит из двух картинок:
 *   - menu_top.png — строка «Продолжить читать»;
 *   - menu_bottom.png — остальные три строки.
 *
 * Программно:
 *   - menu_top.png при отсутствии сохранения рисуется с прозрачностью 0.45
 *     и его зона нажатия выключена.
 *   - menu_bottom.png всегда непрозрачный и активен.
 *
 * Обе картинки кладутся внутрь BookPageContent — то есть на правую страницу книги.
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
        // Слой 0-2: фон — дерево + книга + рамка.
        BookPageBackground()

        // Слой 3: контент внутри правой страницы.
        BookPageContent {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // menu_top.png — «Продолжить читать».
                // Если нет сохранения — прозрачная, зона нажатия отключена.
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
                ) {
                    AssetImage(
                        fileName = "menu_top.png",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(
                                if (hasSavedGame) MENU_TOP_ALPHA_ENABLED
                                else MENU_TOP_ALPHA_DISABLED
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                // menu_bottom.png — остальные три строки.
                // Всегда непрозрачный. Зоны нажатия — три равные полосы по вертикали.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f)
                ) {
                    AssetImage(
                        fileName = "menu_bottom.png",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        // «Новая История»
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (hasSavedGame) showNewGameConfirm = true
                                        else onNewGame()
                                    }
                                )
                        )
                        // «Настройки»
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
                        // «Выход из игры»
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
            }
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