package app.urgent.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * The two-calls mark, drawn rather than loaded as a drawable because its three parts take different
 * colours: a `VectorDrawable` can only be tinted as a whole. Decorative wherever it is used, since
 * the text beside it always names the app or the switch.
 */
@Composable
fun Mark(
    width: Dp,
    dots: Color,
    ring: Color,
    modifier: Modifier = Modifier,
    firstDotAlpha: Float = 1f,
) {
    Canvas(modifier.size(width, width * (MarkGeometry.HEIGHT / MarkGeometry.WIDTH))) {
        val unit = size.width / MarkGeometry.WIDTH
        drawCircle(
            color = dots,
            radius = MarkGeometry.FIRST_RADIUS * unit,
            center = Offset(MarkGeometry.FIRST_X * unit, MarkGeometry.FIRST_Y * unit),
            alpha = firstDotAlpha,
        )
        drawCircle(
            color = dots,
            radius = MarkGeometry.SECOND_RADIUS * unit,
            center = Offset(MarkGeometry.SECOND_X * unit, MarkGeometry.SECOND_Y * unit),
        )
        drawCircle(
            color = ring,
            radius = MarkGeometry.RING_RADIUS * unit,
            center = Offset(MarkGeometry.RING_X * unit, MarkGeometry.RING_Y * unit),
            style = Stroke(width = MarkGeometry.RING_STROKE * unit),
        )
    }
}
