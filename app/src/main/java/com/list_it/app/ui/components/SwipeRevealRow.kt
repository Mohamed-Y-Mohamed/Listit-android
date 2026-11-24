package com.list_it.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One action revealed behind a swiped row. */
data class SwipeAction(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

/** Past this velocity the flick decides the row, whatever distance it covered. */
private const val FlickVelocity = 400f

private val Settle = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

/** Gap before each circle, so they read as separate buttons rather than one bar. */
private val TileGap = 12.dp

/**
 * A row that slides left to reveal [actions] (iOS `.swipeActions`). Tapping an open row
 * closes it; tapping a closed one runs [onClick].
 *
 * [actions] is also published through the accessibility tree, because a horizontal drag
 * is not something a screen reader can perform.
 */
@Composable
fun SwipeRevealRow(
    actions: List<SwipeAction>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    actionWidth: Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    if (actions.isEmpty()) {
        Box(modifier.clip(shape).clickable(onClick = onClick)) { content() }
        return
    }

    // Each circle carries its own gap, so the strip is n * (diameter + gap) wide.
    val maxOffset = with(LocalDensity.current) { ((actionWidth + TileGap) * actions.size).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val close = { scope.launch { offset.animateTo(0f, Settle) } }
    // Only compose the strip once it shows. Left mounted it would sit behind every closed
    // row and be read out by TalkBack, on top of the custom actions below.
    val revealed by remember { derivedStateOf { offset.value < 0f } }

    // Pinning adds or drops an action, so an open row's travel can shrink underneath it.
    LaunchedEffect(maxOffset) { if (offset.value < -maxOffset) offset.snapTo(-maxOffset) }

    Box(
        modifier.semantics {
            customActions = actions.map { action ->
                CustomAccessibilityAction(action.label) { action.onClick(); true }
            }
        },
    ) {
        // A full circle each, outside the card's clip so its outline cannot shave them.
        // The label rides on the icon as its description, since there is no room to print it.
        if (revealed) {
            Row(
                Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.spacedBy(TileGap, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEach { action ->
                    Box(
                        Modifier
                            .size(actionWidth)
                            .clip(CircleShape)
                            .background(action.color)
                            .clickable { close(); action.onClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(action.icon, action.label, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        // This Box sizes the row and clips the card as it slides out of view.
        Box(Modifier.clip(shape)) {
            Box(
                Modifier
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch { offset.snapTo((offset.value + delta).coerceIn(-maxOffset, 0f)) }
                        },
                        onDragStopped = { velocity ->
                            val open = velocity < -FlickVelocity ||
                                (velocity <= FlickVelocity && offset.value < -maxOffset / 2f)
                            offset.animateTo(if (open) -maxOffset else 0f, Settle)
                        },
                    )
                    .clickable { if (offset.value < -1f) close() else onClick() },
            ) { content() }
        }
    }
}
