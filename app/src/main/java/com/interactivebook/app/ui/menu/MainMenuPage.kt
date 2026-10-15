package com.interactivebook.app.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import com.interactivebook.app.ui.common.BookDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.interactivebook.app.R
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent

private const val MENU_TOP_ALPHA_DISABLED = 0.45f

// Доли высоты области (координаты из menu_top.png / menu_bottom.png, 1024 × 1536).
private const val ZONE_TOP_SPACER = 0.143f
private const val ZONE_CONTINUE = 0.137f
private const val ZONE_GAP_1 = 0.046f
private const val ZONE_NEW_GAME = 0.130f
private const val ZONE_GAP_2 = 0.065f
private const val ZONE_SETTINGS = 0.143f
private const val ZONE_GAP_3 = 0.078f
private const val ZONE_EXIT = 0.143f
private const val ZONE_BOTTOM_SPACER = 0.115f

// Ширина зоны нажатия — 60% (по 20% отступа с каждого края).
private const val TAP_ZONE_WIDTH_FRACTION = 0.6f

/**
 * Главное меню — страница открытой книги.
 *
 * Меню состоит из двух картинок:
 *   - menu_top.png — «Продолжить читать»;
 *   - menu_bottom.png — остальные три строки.
 *
 * Зоны нажатия ограничены центральной частью страницы (60% ширины),
 * чтобы тап по краю не срабатывал как нажатие пункта меню.
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
        BookPageBackground()

        BookPageContent {
            Box(modifier = Modifier.fillMaxSize()) {

                AssetImage(
                    fileName = "menu_top.png",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (hasSavedGame) 1f else MENU_TOP_ALPHA_DISABLED
                        },
                    contentScale = ContentScale.FillBounds
                )

                AssetImage(
                    fileName = "menu_bottom.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Column(modifier = Modifier.fillMaxSize()) {

                    Spacer(Modifier.weight(ZONE_TOP_SPACER))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(TAP_ZONE_WIDTH_FRACTION)
                            .weight(ZONE_CONTINUE)
                            .align(Alignment.CenterHorizontally)
                            .clickable(
                                enabled = hasSavedGame,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onContinue
                            )
                    )

                    Spacer(Modifier.weight(ZONE_GAP_1))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(TAP_ZONE_WIDTH_FRACTION)
                            .weight(ZONE_NEW_GAME)
                            .align(Alignment.CenterHorizontally)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (hasSavedGame) showNewGameConfirm = true
                                    else onNewGame()
                                }
                            )
                    )

                    Spacer(Modifier.weight(ZONE_GAP_2))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(TAP_ZONE_WIDTH_FRACTION)
                            .weight(ZONE_SETTINGS)
                            .align(Alignment.CenterHorizontally)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSettings
                            )
                    )

                    Spacer(Modifier.weight(ZONE_GAP_3))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(TAP_ZONE_WIDTH_FRACTION)
                            .weight(ZONE_EXIT)
                            .align(Alignment.CenterHorizontally)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onExit
                            )
                    )

                    Spacer(Modifier.weight(ZONE_BOTTOM_SPACER))
                }
            }
        }
    }

    if (showNewGameConfirm) {
        BookDialog(
            onDismissRequest = { showNewGameConfirm = false },
            title = stringResource(R.string.menu_new_game_confirm_title),
            message = stringResource(R.string.menu_new_game_confirm_text),
            confirmLabel = stringResource(R.string.action_confirm),
            onConfirm = {
                showNewGameConfirm = false
                onNewGame()
            },
            dismissLabel = stringResource(R.string.action_cancel),
            onDismiss = { showNewGameConfirm = false }
        )
    }
}