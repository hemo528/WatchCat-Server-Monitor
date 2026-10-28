package com.watchcat.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.watchcat.app.ui.theme.AppThemeState
import com.watchcat.app.ui.theme.PageBackground
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * 中性黑 / 白毛玻璃背景。
 * 深色模式是近黑底 + 白色雾光；浅色模式是白底 + 灰色柔光，不再使用蓝紫渐变。
 */
@Composable
fun AuroraBackground(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val dark = AppThemeState.isDark
    Box(modifier = modifier.fillMaxSize().hazeSource(hazeState)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (dark) {
                drawDarkBackground()
            } else {
                drawLightBackground()
            }
        }
        content()
    }
}

private fun DrawScope.drawDarkBackground() {
    val w = size.width
    val h = size.height

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF010102),
                Color(0xFF08080A),
                Color(0xFF000000),
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
    )

    // 冷白雾光，而不是彩色极光
    orb(Offset(w * 0.10f, h * 0.00f), w * 1.05f, Color.White, 0.070f)
    orb(Offset(w * 0.98f, h * 0.24f), w * 0.90f, Color.White, 0.045f)
    orb(Offset(w * 0.35f, h * 0.72f), w * 0.95f, Color.White, 0.030f)
    orb(Offset(w * 0.85f, h * 0.95f), w * 0.70f, Color.White, 0.026f)

    drawGrid(Color.White.copy(alpha = 0.020f))

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.050f), Color.Transparent),
            startY = 0f,
            endY = h * 0.28f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, PageBackground.copy(alpha = 0.82f)),
            startY = h * 0.60f,
            endY = h,
        ),
    )
}

private fun DrawScope.drawLightBackground() {
    val w = size.width
    val h = size.height

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFECECF1),
                Color(0xFFF7F7FA),
                Color(0xFFE7E7ED),
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
    )

    // 灰色雾团 + 柔光，让白色玻璃能真正"透"出背景
    orb(Offset(w * 0.10f, h * 0.02f), w * 1.08f, Color(0xFFB7B8C4), 0.74f)
    orb(Offset(w * 0.98f, h * 0.18f), w * 0.88f, Color(0xFFC8C9D4), 0.72f)
    orb(Offset(w * 0.18f, h * 0.42f), w * 0.55f, Color(0xFFB2B3C0), 0.40f)
    orb(Offset(w * 0.78f, h * 0.52f), w * 0.52f, Color(0xFFBEBFCC), 0.36f)
    orb(Offset(w * 0.45f, h * 0.86f), w * 0.80f, Color(0xFFCACBD6), 0.46f)
    orb(Offset(w * 0.55f, h * 0.28f), w * 0.42f, Color.White, 0.55f)

    // 小范围散景，透过毛玻璃会形成柔和的圆形光斑
    orb(Offset(w * 0.22f, h * 0.16f), w * 0.22f, Color(0xFFAEB0BE), 0.55f)
    orb(Offset(w * 0.76f, h * 0.13f), w * 0.18f, Color(0xFFB9BBC8), 0.45f)
    orb(Offset(w * 0.32f, h * 0.36f), w * 0.20f, Color(0xFFA8AAB8), 0.45f)
    orb(Offset(w * 0.72f, h * 0.47f), w * 0.18f, Color(0xFFB5B7C4), 0.36f)
    orb(Offset(w * 0.25f, h * 0.62f), w * 0.16f, Color(0xFFADAFBC), 0.36f)
    orb(Offset(w * 0.82f, h * 0.72f), w * 0.18f, Color(0xFFB9BBC7), 0.30f)

    // 斜向灰白带，透过玻璃后形成柔和的明暗过渡
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFB6B7C4).copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            start = Offset(0f, h * 0.10f),
            end = Offset(w, h * 0.92f),
        ),
    )

    drawGrid(Color.Black.copy(alpha = 0.035f))

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
            startY = 0f,
            endY = h * 0.32f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFFD8D9E1).copy(alpha = 0.55f)),
            startY = h * 0.62f,
            endY = h,
        ),
    )
}

private fun DrawScope.drawGrid(color: Color) {
    val step = 56.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.6.dp.toPx(),
        )
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.6.dp.toPx(),
        )
        y += step
    }
}

private fun DrawScope.orb(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.35f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
