package com.interactivebook.app.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.interactivebook.app.R
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent

private const val MENU_TOP_ALPHA_DISABLED = 0.45f

// Доли высоты области (координаты из menu_top.png / menu_bottom.png, 1024 × 1536).
// Верхняя и нижняя картинки наложены друг на друга.
private const val ZONE_TOP_SPACER = 0.143f
private const val ZONE_CONTINUE = 0.137f       // 0.143 – 0.280
private const val ZONE_GAP_1 = 0.046f          // 0.280 – 0.326
private const val ZONE_NEW_GAME = 0.130f       // 0.326 – 0.456
private const val ZONE_GAP_2 = 0.065f          // 0.456 – 0.521
private const val ZONE_SETTINGS = 0.143f       // 0.521 – 0.664
private const val ZONE_GAP_3 = 0.078f          // 0.664 – 0.742
private const val ZONE_EXIT = 0.143f           // 0.742 – 0.885
private const val ZONE_BOTTOM_SPACER = 0.115f  // 0.885 – 1.0

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

                // menu_top.png — строка «Продолжить читать», с увеличением.
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

                // menu_bottom.png — остальные три строки.
                AssetImage(
                    fileName = "menu_bottom.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                // Невидимые зоны нажатия — в тех же долях высоты.
                Column(modifier = Modifier.fillMaxSize()) {

                    Spacer(Modifier.weight(ZONE_TOP_SPACER))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(ZONE_CONTINUE)
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
                            .fillMaxWidth()
                            .weight(ZONE_NEW_GAME)
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
                            .fillMaxWidth()
                            .weight(ZONE_SETTINGS)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSettings
                            )
                    )

                    Spacer(Modifier.weight(ZONE_GAP_3))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(ZONE_EXIT)
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