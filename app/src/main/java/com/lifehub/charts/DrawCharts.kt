package com.lifehub.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 环形进度图（生活指数圆环）
 * progress: 0..1
 */
@Composable
fun RingProgress(
    modifier: Modifier = Modifier.size(120.dp),
    progress: Float,
    color: Color = Clay,
    trackColor: Color = Line,
    stroke: Float = 12f,
    centerLabel: @Composable () -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension - stroke) / 2
            val center = Offset(size.width / 2, size.height / 2)
            // 背景环
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // 进度环
            val p = progress.coerceIn(0f, 1f)
            if (p > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        centerLabel()
    }
}

private val Clay = Color(0xFFA2543C)
private val Line = Color(0xFFDAD5CC)
private val Sage = Color(0xFF5D7561)
private val Amber = Color(0xFFC8893B)

/**
 * 环形图（按分类聚合）
 * segments: 每段 (占比值, 颜色)
 */
@Composable
fun DonutChart(
    modifier: Modifier = Modifier.size(140.dp),
    segments: List<Pair<Float, Color>>,
    trackColor: Color = Line
) {
    Canvas(modifier = modifier) {
        if (segments.isEmpty()) return@Canvas
        val radius = (size.minDimension) / 2
        val center = Offset(size.width / 2, size.height / 2)
        val total = segments.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(0.0001f)
        val strokeWidth = 18f

        // 背景
        drawCircle(
            color = trackColor,
            radius = radius - strokeWidth / 2,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        var start = -90f
        segments.forEach { (value, color) ->
            val sweep = 360f * (value / total)
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            start += sweep
        }
    }
}

/**
 * 折线图 + 可选 7 日均线
 * points: 原始数据（已按 X 顺序，升序）
 */
@Composable
fun LineChart(
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp),
    points: List<Float>,
    color: Color = Sage,
    lineColor: Color = Clay,
    avgColor: Color = Amber,
    showAvg: Boolean = true
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val pad = 16f
        val maxV = points.maxOf { it }.coerceAtLeast(0.0001f)
        val minV = points.minOf { it }.coerceAtMost(maxV)
        val range = (maxV - minV).coerceAtLeast(0.0001f)
        val stepX = if (points.size > 1) (w - 2 * pad) / (points.size - 1) else 0f

        val toXY = { i: Int, v: Float ->
            Offset(
                pad + stepX * i,
                h - pad - (v - minV) / range * (h - 2 * pad)
            )
        }

        // 7日均线
        if (showAvg && points.size >= 2) {
            val avgPath = Path()
            points.forEachIndexed { i, _ ->
                val startIdx = maxOf(0, i - 6)
                val window = points.subList(startIdx, i + 1)
                val avg = window.average().toFloat()
                val pt = toXY(i, avg)
                if (i == 0) avgPath.moveTo(pt.x, pt.y) else avgPath.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = avgPath,
                color = avgColor.copy(alpha = 0.6f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round)
            )
        }

        // 主折线
        val path = Path()
        points.forEachIndexed { i, v ->
            val pt = toXY(i, v)
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // 数据点
        points.forEachIndexed { i, v ->
            val pt = toXY(i, v)
            drawCircle(
                color = color,
                radius = 3f,
                center = pt
            )
        }
    }
}

/**
 * 月度柱图（12 个月分布）
 * values: 12 个值
 */
@Composable
fun MonthBarChart(
    modifier: Modifier = Modifier.fillMaxWidth().height(120.dp),
    values: List<Int>,
    color: Color = Clay,
    trackColor: Color = Line
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val pad = 6f
        val maxV = values.maxOf { it }.coerceAtLeast(1)
        val n = values.size
        val gap = 6f
        val bw = (w - 2 * pad - (n - 1) * gap) / n
        values.forEachIndexed { i, v ->
            val x = pad + i * (bw + gap)
            val bh = (v.toFloat() / maxV) * (h - 2 * pad)
            val y = h - pad - bh
            drawRoundRect(
                color = if (v > 0) color else trackColor,
                topLeft = Offset(x, y),
                size = Size(bw, bh.coerceAtLeast(2f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }
    }
}

/**
 * 30 格热力图（习惯打卡）
 * states: 30 个值
 */
@Composable
fun HeatmapChart(
    modifier: Modifier = Modifier.fillMaxWidth(),
    states: List<Boolean>,
    color: Color = Sage
) {
    Canvas(modifier = modifier.height(80.dp)) {
        val n = states.size.coerceAtLeast(1)
        val cols = 10
        val rows = (n + cols - 1) / cols
        val gap = 4f
        val cell = (size.width - (cols - 1) * gap) / cols
        val cellH = (size.height - (rows - 1) * gap) / rows

        states.forEachIndexed { i, done ->
            val c = i % cols
            val r = i / cols
            val x = c * (cell + gap)
            val y = r * (cellH + gap)
            drawRoundRect(
                color = if (done) color else Line,
                topLeft = Offset(x, y),
                size = Size(cell, cellH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }
    }
}
