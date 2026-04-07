package com.letta.mobile.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

class MessageBubbleShape(
    private val radius: Dp,
    private val isFromUser: Boolean = false,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radiusPx = with(density) { radius.toPx() }
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    topLeftCornerRadius = if (isFromUser) CornerRadius(radiusPx) else CornerRadius(0f),
                    topRightCornerRadius = if (isFromUser) CornerRadius(0f) else CornerRadius(radiusPx),
                    bottomLeftCornerRadius = CornerRadius(radiusPx),
                    bottomRightCornerRadius = CornerRadius(radiusPx),
                )
            )
        }
        return Outline.Generic(path)
    }
}
