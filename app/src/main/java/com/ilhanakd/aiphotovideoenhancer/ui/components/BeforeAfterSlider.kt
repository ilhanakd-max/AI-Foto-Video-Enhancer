package com.ilhanakd.aiphotovideoenhancer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun BeforeAfterSlider(
    before: ImageBitmap?,
    after: ImageBitmap?,
    beforeLabel: String,
    afterLabel: String,
    modifier: Modifier = Modifier
) {
    val handlePosition = remember { mutableFloatStateOf(0.5f) }
    if (before == null || after == null) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    val newValue = (handlePosition.floatValue + dragAmount / size.width).coerceIn(0f, 1f)
                    handlePosition.floatValue = newValue
                }
            }
    ) {
        Image(
            bitmap = before,
            contentDescription = beforeLabel,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            clipRect(right = size.width * handlePosition.floatValue) {
                drawImage(after)
            }
            drawLine(
                color = MaterialTheme.colorScheme.primary,
                start = Offset(x = size.width * handlePosition.floatValue, y = 0f),
                end = Offset(x = size.width * handlePosition.floatValue, y = size.height),
                strokeWidth = 4.dp.toPx()
            )
        }
        Text(
            text = beforeLabel,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = afterLabel,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
