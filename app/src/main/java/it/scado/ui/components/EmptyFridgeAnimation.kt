package it.scado.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.scado.ui.theme.LocalDarkTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Un frigo sorridente che sbatte le palpebre; ogni tanto apre la porta e
 * mostra i ripiani vuoti illuminati, con un filo di aria fredda che esce.
 */
@Composable
fun EmptyFridgeAnimation(modifier: Modifier = Modifier.size(width = 170.dp, height = 220.dp)) {
    val transition = rememberInfiniteTransition(label = "fridge")
    val open by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 5600
                0f at 0
                0f at 1500 using FastOutSlowInEasing
                1f at 2300
                1f at 4300 using FastOutSlowInEasing
                0f at 4900
                0f at 5600
            }
        ),
        label = "door",
    )
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "mist",
    )
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3700
                1f at 0
                1f at 3350
                0.1f at 3470
                1f at 3620
            }
        ),
        label = "blink",
    )

    val dark = LocalDarkTheme.current
    val scheme = MaterialTheme.colorScheme
    val colors = FridgeColors(
        body = scheme.primaryContainer,
        outline = scheme.primary,
        face = scheme.onPrimaryContainer,
        interior = if (dark) Color(0xFF2E3326) else Color(0xFFFFFBEA),
        glow = if (dark) Color(0xFF5C5A3A) else Color(0xFFFFEFA8),
        shelf = scheme.outlineVariant,
        shade = scheme.outline,
        mist = if (dark) Color(0xFFCFEFFF) else Color(0xFF8FC9E8),
    )

    Canvas(modifier.semantics { contentDescription = "Un frigo vuoto" }) {
        drawFridge(open, time, blink, colors)
    }
}

private class FridgeColors(
    val body: Color,
    val outline: Color,
    val face: Color,
    val interior: Color,
    val glow: Color,
    val shelf: Color,
    val shade: Color,
    val mist: Color,
)

private fun DrawScope.drawFridge(open: Float, time: Float, blink: Float, c: FridgeColors) {
    val w = size.width
    val h = size.height
    val stroke = 3.dp.toPx()
    val radius = CornerRadius(18.dp.toPx())

    val left = w * 0.16f
    val right = w * 0.84f
    val top = h * 0.03f
    val bottom = h * 0.92f
    val bodyW = right - left
    val bodyH = bottom - top
    val split = top + bodyH * 0.33f
    val cx = (left + right) / 2

    // piedini
    val footW = bodyW * 0.14f
    listOf(left + bodyW * 0.12f, right - bodyW * 0.12f - footW).forEach { x ->
        drawRoundRect(c.outline, Offset(x, bottom - 2f), Size(footW, h * 0.05f), CornerRadius(4.dp.toPx()))
    }

    // corpo
    drawRoundRect(c.body, Offset(left, top), Size(bodyW, bodyH), radius)

    // interno del frigo (visibile quando la porta si apre)
    val inset = stroke * 1.5f
    val compLeft = left + inset
    val compTop = split + inset
    val compW = bodyW - inset * 2
    val compH = bottom - inset - compTop
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(lerp(c.interior, c.glow, open), c.interior),
            center = Offset(cx, compTop + compH * 0.1f),
            radius = bodyW,
        ),
        topLeft = Offset(compLeft, compTop),
        size = Size(compW, compH),
    )
    // ripiani vuoti
    for (i in 1..2) {
        val y = compTop + compH * i / 3f
        drawLine(c.shelf, Offset(compLeft + 6f, y), Offset(compLeft + compW - 6f, y), stroke * 0.8f, StrokeCap.Round)
    }
    // lampadina
    drawCircle(lerp(c.shelf, c.glow, open), 5.dp.toPx(), Offset(cx, compTop + 8.dp.toPx()))

    // porta del frigo: ruota sul cardine a sinistra (in vista frontale si restringe)
    val doorW = compW * (1f - open * 0.8f)
    val doorColor = lerp(c.body, c.shade, open * 0.35f)
    drawRoundRect(
        doorColor, Offset(compLeft - inset / 2, compTop - inset / 2),
        Size(doorW + inset, compH + inset), CornerRadius(10.dp.toPx())
    )
    drawRoundRect(
        c.outline, Offset(compLeft - inset / 2, compTop - inset / 2),
        Size(doorW + inset, compH + inset), CornerRadius(10.dp.toPx()), style = Stroke(stroke * 0.7f)
    )
    val handleX = compLeft + doorW - 14.dp.toPx() * (doorW / compW)
    drawLine(
        c.outline, Offset(handleX, compTop + 14.dp.toPx()), Offset(handleX, compTop + 44.dp.toPx()),
        stroke * 1.4f * (0.4f + 0.6f * doorW / compW), StrokeCap.Round
    )

    // contorno e separazione freezer
    drawRoundRect(c.outline, Offset(left, top), Size(bodyW, bodyH), radius, style = Stroke(stroke))
    drawLine(c.outline, Offset(left, split), Offset(right, split), stroke)
    // maniglia freezer
    val fhX = right - 16.dp.toPx()
    drawLine(c.outline, Offset(fhX, split - 30.dp.toPx()), Offset(fhX, split - 12.dp.toPx()), stroke * 1.4f, StrokeCap.Round)

    // faccina sul freezer
    val eyeY = top + (split - top) * 0.42f
    val eyeR = 4.5.dp.toPx()
    listOf(cx - bodyW * 0.15f, cx + bodyW * 0.08f).forEach { x ->
        drawOval(
            c.face,
            topLeft = Offset(x - eyeR, eyeY - eyeR * blink),
            size = Size(eyeR * 2, eyeR * 2 * blink),
        )
    }
    val mouthW = bodyW * 0.16f
    val mouthCx = cx - bodyW * 0.035f
    if (open > 0.5f) {
        // stupore: "o"
        drawCircle(c.face, 5.dp.toPx() * open, Offset(mouthCx, eyeY + 16.dp.toPx()), style = Stroke(stroke * 0.9f))
    } else {
        drawArc(
            c.face, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(mouthCx - mouthW / 2, eyeY + 2.dp.toPx()),
            size = Size(mouthW, mouthW * 0.8f),
            style = Stroke(stroke * 0.9f, cap = StrokeCap.Round),
        )
    }

    // aria fredda che esce dalla porta aperta
    if (open > 0.05f) {
        for (i in 0 until 6) {
            val phase = (time + i / 6f) % 1f
            val x = compLeft + compW * (0.4f + 0.75f * phase) +
                    sin((phase * 2 * PI + i).toFloat()) * 5.dp.toPx()
            val y = compTop + compH * (0.18f + (i % 3) * 0.25f) + phase * 18.dp.toPx()
            val alpha = (open * (1f - phase) * 0.55f).coerceIn(0f, 1f)
            drawCircle(c.mist.copy(alpha = alpha), (3f + 6f * phase).dp.toPx(), Offset(x, y))
        }
    }
}
