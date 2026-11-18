package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalFollowLensColors
import kotlin.math.roundToInt

@Composable
fun FollowRatioGauge(
    mutualCount: Int,
    notFollowingBackCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
) {
    val totalFollowing = (mutualCount + notFollowingBackCount).coerceAtLeast(1)
    val mutualFraction = mutualCount.toFloat() / totalFollowing.toFloat()
    val animatedMutual by animateFloatAsState(
        targetValue = mutualFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "mutual_ratio_anim",
    )

    val semantics = LocalFollowLensColors.current
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val mutualColor = semantics.mutual
    val nonFollowerColor = semantics.notFollowingBack

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Base track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Not following back remainder
            if (notFollowingBackCount > 0) {
                drawArc(
                    color = nonFollowerColor.copy(alpha = 0.85f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Mutual follow-back arc
            if (animatedMutual > 0f) {
                drawArc(
                    color = mutualColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedMutual,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val pct = if (mutualCount + notFollowingBackCount == 0) 0 else (mutualFraction * 100f).roundToInt()
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "follow back",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EmptyArchiveIllustration(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Ambient radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.22f),
                    secondary.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = center,
                radius = w * 0.48f,
            ),
            radius = w * 0.48f,
            center = center,
        )

        // Concentric orbit rings
        drawCircle(
            color = primary.copy(alpha = 0.16f),
            radius = minOf(w, h) * 0.39f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(
            color = secondary.copy(alpha = 0.22f),
            radius = minOf(w, h) * 0.29f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        // Stylized folder / ZIP archive card
        val boxW = minOf(w, h) * 0.44f
        val boxH = boxW * 0.72f
        val left = center.x - boxW / 2f
        val top = center.y - boxH / 2f + 6.dp.toPx()

        // Folder tab
        drawRoundRect(
            color = secondary,
            topLeft = Offset(left + 6.dp.toPx(), top - 10.dp.toPx()),
            size = Size(boxW * 0.42f, 18.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )

        // Folder body
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(cardColor, surfaceColor),
                start = Offset(left, top),
                end = Offset(left + boxW, top + boxH),
            ),
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(primary.copy(alpha = 0.7f), secondary.copy(alpha = 0.7f)),
                start = Offset(left, top),
                end = Offset(left + boxW, top + boxH),
            ),
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )

        // Shield badge inside folder
        val shieldCenter = Offset(center.x, top + boxH * 0.52f)
        val sw = boxW * 0.26f
        val sh = boxH * 0.46f
        val shieldPath = Path().apply {
            moveTo(shieldCenter.x, shieldCenter.y - sh / 2f)
            lineTo(shieldCenter.x + sw / 2f, shieldCenter.y - sh * 0.25f)
            lineTo(shieldCenter.x + sw / 2f, shieldCenter.y + sh * 0.1f)
            quadraticTo(
                shieldCenter.x + sw * 0.35f,
                shieldCenter.y + sh * 0.42f,
                shieldCenter.x,
                shieldCenter.y + sh / 2f,
            )
            quadraticTo(
                shieldCenter.x - sw * 0.35f,
                shieldCenter.y + sh * 0.42f,
                shieldCenter.x - sw / 2f,
                shieldCenter.y + sh * 0.1f,
            )
            lineTo(shieldCenter.x - sw / 2f, shieldCenter.y - sh * 0.25f)
            close()
        }
        drawPath(
            path = shieldPath,
            brush = Brush.verticalGradient(listOf(primary, secondary)),
        )

        // Accent orbiting nodes
        drawCircle(
            color = tertiary,
            radius = 6.dp.toPx(),
            center = Offset(center.x + minOf(w, h) * 0.34f, center.y - minOf(w, h) * 0.18f),
        )
        drawCircle(
            color = primary,
            radius = 5.dp.toPx(),
            center = Offset(center.x - minOf(w, h) * 0.35f, center.y + minOf(w, h) * 0.16f),
        )
        drawCircle(
            color = secondary,
            radius = 4.dp.toPx(),
            center = Offset(center.x - minOf(w, h) * 0.20f, center.y - minOf(w, h) * 0.32f),
        )
    }
}
