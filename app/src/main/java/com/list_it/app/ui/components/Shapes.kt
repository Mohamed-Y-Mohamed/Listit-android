package com.list_it.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** The wavy edge drawn on list cards. Sizes are in pixels. */
fun wavePath(size: Size, depth: Float, curvature: Float): Path = Path().apply {
    val x = minOf(depth, size.width)
    moveTo(0f, 0f)
    lineTo(x, 0f)
    cubicTo(x + curvature, size.height * 0.30f, x - curvature, size.height * 0.70f, x, size.height)
    lineTo(0f, size.height)
    close()
}

/** The slanted coloured strip behind a collection's header. */
fun diagonalSlicePath(size: Size, skewOffset: Float): Path = Path().apply {
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(maxOf(0f, size.width - skewOffset), size.height)
    lineTo(0f, size.height)
    close()
}

/** The outline of a note card, with its bottom-right corner folded over. */
class FoldedNoteShape(private val cornerRadius: Float, private val foldSize: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = cornerRadius * density.density
        val f = foldSize * density.density
        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(size.width - r, 0f)
            arcTo(Rect(Offset(size.width - 2 * r, 0f), Size(2 * r, 2 * r)), -90f, 90f, false)
            lineTo(size.width, size.height - f)
            lineTo(size.width - f, size.height)
            lineTo(r, size.height)
            arcTo(Rect(Offset(0f, size.height - 2 * r), Size(2 * r, 2 * r)), 90f, 90f, false)
            lineTo(0f, r)
            arcTo(Rect(Offset(0f, 0f), Size(2 * r, 2 * r)), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

/** The small triangle that makes the folded corner on a note card. */
object FoldFlapShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(0f, size.height); close() })
}
