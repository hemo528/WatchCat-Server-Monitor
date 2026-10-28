package com.watchcat.app.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.TextSecondary

@Composable
fun TrendChart(
    values: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    fixedRange: ClosedFloatingPointRange<Float>? = null,
    chartHeight: Dp = 150.dp,
) {
    if (values.size < 2) {
        ChartPlaceholder(
            accent = accent,
            modifier = modifier.fillMaxWidth().height(chartHeight),
        )
        return
    }

    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        producer.runTransaction {
            lineModel {
                series(values.map { it.toDouble() })
            }
        }
    }

    val theme = rememberM3VicoTheme(
        lineCartesianLayerColors = listOf(accent),
        lineColor = GlassLine.copy(alpha = 0.10f),
        textColor = TextSecondary,
    )

    ProvideVicoTheme(theme) {
        Box(modifier = modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(Modifier.fillMaxSize()) {
                for (i in 1..3) {
                    val y = size.height * i / 4f
                    drawLine(
                        color = GlassLine.copy(alpha = 0.055f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            val chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(accent)),
                            stroke = LineCartesianLayer.LineStroke.Continuous(
                                thickness = 2.dp,
                                cap = StrokeCap.Round,
                            ),
                            areaFill = LineCartesianLayer.AreaFill.single(
                                Fill(
                                    Brush.verticalGradient(
                                        listOf(
                                            accent.copy(alpha = 0.34f),
                                            accent.copy(alpha = 0.06f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                            ),
                            interpolator = LineCartesianLayer.Interpolator.cubic(),
                        ),
                    ),
                    pointSpacing = 4.dp,
                    rangeProvider = if (fixedRange != null) {
                        CartesianLayerRangeProvider.fixed(
                            minY = fixedRange.start.toDouble(),
                            maxY = fixedRange.endInclusive.toDouble(),
                        )
                    } else {
                        CartesianLayerRangeProvider.auto()
                    },
                ),
            )
            CartesianChartHost(
                chart = chart,
                modelProducer = producer,
                modifier = Modifier.fillMaxSize(),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                animationSpec = tween<Float>(500),
                animateIn = true,
            )
        }
    }
}

@Composable
private fun ChartPlaceholder(accent: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 1..3) {
                val y = size.height * i / 4f
                drawLine(
                    color = GlassLine.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            drawLine(
                color = accent.copy(alpha = 0.18f),
                start = Offset(0f, size.height - 1.dp.toPx()),
                end = Offset(size.width, size.height - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Text(
            text = "正在采集趋势数据…",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}
