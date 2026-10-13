package com.interactivebook.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- Размеры холста book_page.png ----------
private const val CANVAS_W = 3200f
private const val CANVAS_H = 3000f

// ---------- Координаты книги (открытой) на холсте ----------
private const val BOOK_LEFT_PX = 1130f
private const val BOOK_RIGHT_PX = 2039f
private const val BOOK_TOP_PX = 659f
private const val BOOK_BOTTOM_PX = 2330f

// ---------- Координаты правой страницы (внутри книги) на холсте ----------
private const val PAGE_LEFT_PX = 1211f
private const val PAGE_RIGHT_PX = 1984f
private const val PAGE_TOP_PX = 805f
private const val PAGE_BOTTOM_PX = 2154f

// ---------- Сдвиг содержимого вправо ----------
// Доля от ширины экрана. Нужна, чтобы правая линия рамки ушла за край экрана.
private const val SHIFT_RIGHT_FRACTION = 0.015f

/** Порог: экран < 600 dp — считаем телефоном. */
private val PHONE_WIDTH_THRESHOLD = 600.dp

private fun computeZoom(screenW: Float, screenH: Float, isPhone: Boolean): Float {
    if (!isPhone) return 1f
    val cropScale = max(screenW / CANVAS_W, screenH / CANVAS_H)
    val bookWidthOnScreen = (BOOK_RIGHT_PX - BOOK_LEFT_PX) * cropScale
    return if (bookWidthOnScreen < screenW) screenW / bookWidthOnScreen else 1f
}

/**
 * Фоновая подложка: book_page.png (дерево + книга + рамка).
 * На телефоне — зум так, чтобы книга заполнила ширину, плюс сдвиг вправо.
 */
@Composable
fun BookPageBackground(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val isPhone = maxWidth < PHONE_WIDTH_THRESHOLD
        val zoom = computeZoom(screenW, screenH, isPhone)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = screenW * SHIFT_RIGHT_FRACTION
                },
            contentAlignment = Alignment.Center
        ) {
            AssetImage(
                fileName = "book_page.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Контейнер, который размещает контент ВНУТРИ правой страницы книги
 * на холсте book_page.png (с учётом Crop, зума и сдвига вправо).
 */
@Composable
fun BookPageContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val isPhone = maxWidth < PHONE_WIDTH_THRESHOLD
        val zoom = computeZoom(screenW, screenH, isPhone)

        val scale = max(screenW / CANVAS_W, screenH / CANVAS_H) * zoom

        val drawnW = CANVAS_W * scale
        val drawnH = CANVAS_H * scale
        val offsetX = (screenW - drawnW) / 2f + screenW * SHIFT_RIGHT_FRACTION
        val offsetY = (screenH - drawnH) / 2f

        val pageLeft = offsetX + PAGE_LEFT_PX * scale
        val pageTop = offsetY + PAGE_TOP_PX * scale
        val pageRight = offsetX + PAGE_RIGHT_PX * scale
        val pageBottom = offsetY + PAGE_BOTTOM_PX * scale

        val pageWidthDp = with(density) { (pageRight - pageLeft).toDp() }
        val pageHeightDp = with(density) { (pageBottom - pageTop).toDp() }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(pageLeft.roundToInt(), pageTop.roundToInt())
                    }
                    .size(width = pageWidthDp, height = pageHeightDp)
            ) {
                content()
            }
        }
    }
}