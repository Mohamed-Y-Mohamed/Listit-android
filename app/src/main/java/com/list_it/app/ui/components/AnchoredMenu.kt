package com.list_it.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.list_it.app.ui.theme.LocalPalette

private val CaretWidth = 18.dp
private val CaretHeight = 8.dp
private val MenuCorner = 14.dp
private val ScreenMargin = 12.dp
private val AnchorGap = 6.dp

/**
 * Menu panel drawn as one path: a rounded body plus a caret, so a single border traces
 * both and there is no seam where they meet.
 */
private class CaretPanelShape(
    private val caretCenterX: Float,
    private val pointingUp: Boolean,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val caretH = with(density) { CaretHeight.toPx() }
        val halfCaretW = with(density) { CaretWidth.toPx() } / 2f
        val corner = with(density) { MenuCorner.toPx() }
        val top = if (pointingUp) caretH else 0f
        val bottom = if (pointingUp) size.height else size.height - caretH

        // Keep the caret inside the rounded corners, or it detaches from the body.
        val tipX = caretCenterX.coerceIn(corner + halfCaretW, size.width - corner - halfCaretW)
        val body = Path().apply {
            addRoundRect(RoundRect(0f, top, size.width, bottom, CornerRadius(corner)))
        }
        val caret = Path().apply {
            if (pointingUp) {
                moveTo(tipX - halfCaretW, top)
                lineTo(tipX, 0f)
                lineTo(tipX + halfCaretW, top)
            } else {
                moveTo(tipX - halfCaretW, bottom)
                lineTo(tipX, size.height)
                lineTo(tipX + halfCaretW, bottom)
            }
            close()
        }
        return Outline.Generic(Path().apply { op(body, caret, PathOperation.Union) })
    }
}

/**
 * A menu that opens from whatever composable encloses it, with a caret pointing back at
 * that anchor and a scale that grows out of the caret, so it reads as belonging to the
 * control that was tapped rather than appearing from nowhere.
 *
 * Flips above the anchor when there is no room below.
 */
@Composable
fun AnchoredMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    // Stay mounted until the exit animation has finished.
    if (!visible.currentState && !visible.targetState) return

    val palette = LocalPalette.current
    var caretX by remember { mutableIntStateOf(0) }
    var pointingUp by remember { mutableStateOf(true) }
    var panelWidth by remember { mutableIntStateOf(1) }

    val density = LocalDensity.current
    val marginPx = with(density) { ScreenMargin.roundToPx() }
    val gapPx = with(density) { AnchorGap.roundToPx() }

    val position = remember(marginPx, gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val margin = marginPx
                val gap = gapPx
                val maxX = maxOf(margin, windowSize.width - popupContentSize.width - margin)
                val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(margin, maxX)
                val below = anchorBounds.bottom + gap + popupContentSize.height <= windowSize.height - margin
                val y = if (below) {
                    anchorBounds.bottom + gap
                } else {
                    maxOf(margin, anchorBounds.top - gap - popupContentSize.height)
                }
                caretX = anchorBounds.center.x - x
                pointingUp = below
                panelWidth = maxOf(1, popupContentSize.width)
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        val shape = CaretPanelShape(caretX.toFloat(), pointingUp)
        val origin = TransformOrigin(
            pivotFractionX = (caretX.toFloat() / panelWidth).coerceIn(0f, 1f),
            pivotFractionY = if (pointingUp) 0f else 1f,
        )
        AnimatedVisibility(
            visibleState = visible,
            enter = scaleIn(tween(140), initialScale = 0.88f, transformOrigin = origin) + fadeIn(tween(140)),
            exit = scaleOut(tween(100), targetScale = 0.92f, transformOrigin = origin) + fadeOut(tween(100)),
        ) {
            Column(
                Modifier
                    // Hug the widest item instead of stretching to the cap.
                    .widthIn(min = 180.dp, max = 290.dp)
                    .width(IntrinsicSize.Max)
                    .shadow(10.dp, shape)
                    .background(palette.secondaryGroupedBackground, shape)
                    .border(0.8.dp, palette.separator, shape)
                    .padding(
                        top = if (pointingUp) CaretHeight + 4.dp else 4.dp,
                        bottom = if (pointingUp) 4.dp else CaretHeight + 4.dp,
                    ),
                content = content,
            )
        }
    }
}
