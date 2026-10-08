package com.interactivebook.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity

/**
 * Доли от изображения book_open.png (941 × 1672):
 *   границы правой страницы книги — куда ложится весь контент.
 */
private const val PAGE_LEFT_FRACTION = 0.085f
private const val PAGE_RIGHT_FRACTION = 0.903f
private const val PAGE_TOP_FRACTION = 0.084f
private const val PAGE_BOTTOM_FRACTION = 0.891f

/** Соотношение сторон изображения book_open.png: 941 / 1672. */
private const val BOOK_IMAGE_RATIO = 941f / 1672f

private data class BookRect(
    val widthPx: Float,
    val heightPx: Float,
    val offsetXPx: Float,
    val offsetYPx: Float
)

private fun computeBookRect(screenWidthPx: Float, screenHeightPx: Float): BookRect {
    val screenRatio = screenWidthPx / screenHeightPx
    return if (screenRatio > BOOK_IMAGE_RATIO) {
        // Экран шире книги — книга вписывается по высоте.
        val h = screenHeightPx
        val w = h * BOOK_IMAGE_RATIO
        BookRect(
            widthPx = w,
            heightPx = h,
            offsetXPx = (screenWidthPx - w) / 2f,
            offsetYPx = 0f
        )
    } else {
        // Экран уже книги — книга вписывается по ширине.
        val w = screenWidthPx
        val h = w / BOOK_IMAGE_RATIO
        BookRect(
            widthPx = w,
            heightPx = h,
            offsetXPx = 0f,
            offsetYPx = (screenHeightPx - h) / 2f
        )
    }
}

/**
 * Контейнер, который размещает контент ВНУТРИ правой страницы книги book_open.png.
 *
 * Работает так:
 *  1. BoxWithConstraints измеряет область.
 *  2. Вычисляет, как ляжет картинка книги при ContentScale.Fit.
 *  3. Внутри этой области находит прямоугольник правой страницы.
 *  4. Размещает content внутри этого прямоугольника.
 */
@Composable
fun BookPageContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current

        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val bookRect = computeBookRect(screenWidthPx, screenHeightPx)

        val pageLeftPx = bookRect.offsetXPx + bookRect.widthPx * PAGE_LEFT_FRACTION
        val pageRightPx = bookRect.offsetXPx + bookRect.widthPx * PAGE_RIGHT_FRACTION
        val pageTopPx = bookRect.offsetYPx + bookRect.heightPx * PAGE_TOP_FRACTION
        val pageBottomPx = bookRect.offsetYPx + bookRect.heightPx * PAGE_BOTTOM_FRACTION

        val paddingStartDp = with(density) { pageLeftPx.toDp() }
        val paddingEndDp = with(density) { (screenWidthPx - pageRightPx).toDp() }
        val paddingTopDp = with(density) { pageTopPx.toDp() }
        val paddingBottomDp = with(density) { (screenHeightPx - pageBottomPx).toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingStartDp,
                    end = paddingEndDp,
                    top = paddingTopDp,
                    bottom = paddingBottomDp
                )
        ) {
            content()
        }
    }
}

/**
 * Фоновая подложка для всех экранов "страницы книги":
 *   слой 0 — background.png (дерево),
 *   слой 1 — book_open.png (книга, Fit),
 *   слой 2 — book_frame.png (декоративная рамка).
 */
@Composable
fun BookPageBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        AssetImage(
            fileName = "background.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        AssetImage(
            fileName = "book_open.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        AssetImage(
            fileName = "book_frame.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}