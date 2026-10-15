package com.interactivebook.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// Цвета окна — в тон книги.
private val DialogBackground = Color(0xFF332A1E)   // = surfaceVariant из темы
private val DialogBorder = Color(0xFF7A2525)       // бордовый
private val DialogText = Color(0xFFE8DBC0)         // кремовый
private val DialogAccent = Color(0xFFE8C766)       // золотой

/**
 * Единое диалоговое окно в стиле книги.
 *
 * Заменяет Material3 AlertDialog с серой подложкой.
 * Используется для подтверждений действий, «Начать новую игру?»,
 * «Пропустить пролог?» и любых других диалогов в приложении.
 */
@Composable
fun BookDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DialogBackground,
            border = BorderStroke(1.5.dp, DialogBorder),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = title,
                    color = DialogText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    color = DialogText,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (dismissLabel != null && onDismiss != null) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = dismissLabel,
                                color = DialogAccent,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = confirmLabel,
                            color = DialogAccent,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}