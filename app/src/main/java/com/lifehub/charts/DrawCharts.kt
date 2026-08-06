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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 * 折线图 + 可选 7 日均线 + 目标线
 * points: 原始数据（已按 X 顺序，升序）
 */
@Composable
fun LineChart(
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp),
    points: List<Float>,
    color: Color = Sage,
    lineColor: Color = Clay,
    avgColor: Color = Amber,
    showAvg: Boolean = true,
    goal: Float? = null,
    goalColor: Color = Clay
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val pad = 16f

        // 7 日均线：满 7 天才计算（对齐 HTML 版 movingAvg）
        val ma = points.mapIndexed { i, _ ->
            if (i >= 6) points.subList(i - 6, i + 1).average().toFloat() else null
        }

        val allVals = buildList {
            addAll(points)
            ma.filterNotNull().forEach { add(it) }
            goal?.let { add(it) }
        }
        val maxV = allVals.maxOf { it }.coerceAtLeast(0.0001f)
        val minV = allVals.minOf { it }.coerceAtMost(maxV)
        val range = (maxV - minV).coerceAtLeast(0.0001f)
        val stepX = if (points.size > 1) (w - 2 * pad) / (points.size - 1) else 0f

        val toXY = { i: Int, v: Float ->
            Offset(
                pad + stepX * i,
                h - pad - (v - minV) / range * (h - 2 * pad)
            )
        }

        // 目标线
        goal?.let { g ->
            val y = toXY(0, g).y
            drawLine(
                color = goalColor,
                start = Offset(pad, y),
                end = Offset(w - pad, y),
                strokeWidth = 1.5f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        }

        // 7日均线
        if (showAvg) {
            val avgPath = Path()
            var started = false
            points.forEachIndexed { i, _ ->
                ma[i]?.let { avg ->
                    val pt = toXY(i, avg)
                    if (!started) {
                        avgPath.moveTo(pt.x, pt.y)
                        started = true
                    } else {
                        avgPath.lineTo(pt.x, pt.y)
                    }
                }
            }
            if (started) {
                drawPath(
                    path = avgPath,
                    color = avgColor,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
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
            style = Stroke(width = 2f, cap = StrokeCap.Round)
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
 * 分组柱图（月度对比：支出 / 收入 / 返利）
 * groupValues: 每组包含多个 series 的值
 */
@Composable
fun GroupedBarChart(
    modifier: Modifier = Modifier.fillMaxWidth().height(140.dp),
    groupValues: List<List<Float>>,
    colors: List<Color> = listOf(Clay, Sage, Amber),
    trackColor: Color = Line
) {
    Canvas(modifier = modifier) {
        if (groupValues.isEmpty()) return@Canvas
        val seriesCount = groupValues.first().size.coerceAtLeast(1)
        val groupCount = groupValues.size
        val pad = 8f
        val groupGap = 16f
        val barGap = 4f
        val available = (size.width - 2 * pad - (groupCount - 1) * groupGap).coerceAtLeast(1f)
        val groupW = available / groupCount
        val barW = (groupW - (seriesCount - 1) * barGap) / seriesCount
        val maxV = groupValues.flatten().maxOf { it }.coerceAtLeast(0.0001f)
        val chartH = size.height - 20f

        groupValues.forEachIndexed { gi, vals ->
            val groupX = pad + gi * (groupW + groupGap)
            vals.forEachIndexed { si, v ->
                val x = groupX + si * (barW + barGap)
                val bh = (v / maxV) * chartH
                val y = chartH - bh
                drawRoundRect(
                    color = colors.getOrElse(si) { Clay },
                    topLeft = Offset(x, y),
                    size = Size(barW.coerceAtLeast(2f), bh.coerceAtLeast(0f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
            }
        }
    }
}

/**
 * 渐变热力图（习惯打卡）
 * states: 每日 ratio 0..1
 */
@Composable
fun HeatmapChart(
    modifier: Modifier = Modifier.fillMaxWidth(),
    states: List<Float>,
    color: Color = Sage
) {
    Canvas(modifier = modifier.height(80.dp)) {
        val n = states.size.coerceAtLeast(1)
        val cols = 15
        val rows = (n + cols - 1) / cols
        val gap = 4f
        val cell = (size.width - (cols - 1) * gap) / cols
        val cellH = (size.height - (rows - 1) * gap) / rows
        val todayIndex = n - 1

        states.forEachIndexed { i, ratio ->
            val c = i % cols
            val r = i / cols
            val x = c * (cell + gap)
            val y = r * (cellH + gap)
            val alpha = (0.24f + ratio * 0.76f).coerceIn(0f, 1f)
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(cell, cellH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
            if (i == todayIndex) {
                drawRoundRect(
                    color = Color(0xFF2B2622),
                    topLeft = Offset(x, y),
                    size = Size(cell, cellH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

/**
 * 结余瀑布图（对齐网页 svgWaterfall）
 * 四根柱子：收入 → 返利 → 支出 → 实际结余，并用虚线连接
 */
@Composable
fun WaterfallChart(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(160.dp),
    income: Double,
    rebate: Double,
    expense: Double
) {
    val ink = Color(0xFF1F1E1B)
    val soft = Color(0xFF918C81)
    val line = Color(0xFFE4DED2)
    val green = Color(0xFF4C7554)
    val gold = Color(0xFFA8842F)
    val red = Color(0xFFB0433A)

    Canvas(modifier = modifier) {
        val net = income + rebate - expense
        val steps = listOf(
            WaterfallStep("收入", income, 0.0, income, green, false),
            WaterfallStep("返利", rebate, income, income + rebate, gold, false),
            WaterfallStep("支出", -expense, income + rebate, net, red, false),
            WaterfallStep("实际结余", net, 0.0, net, if (net >= 0) Color(0xFF3F6347) else Color(0xFF8A2F27), true)
        )
        val peak = maxOf(income + rebate, expense, kotlin.math.abs(net), 1.0)
        val topPad = 28f
        val bottomPad = 42f
        val chartH = size.height - topPad - bottomPad
        val zeroY = size.height - bottomPad
        val scale = (chartH / (peak * 1.12)).toFloat()
        val n = steps.size
        val gw = size.width / n
        val barW = minOf(88f, gw * 0.5f)

        // 零线
        drawLine(
            color = line,
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.5f
        )

        steps.forEachIndexed { i, st ->
            val cx = gw * i + gw / 2
            val yA = zeroY - (st.from * scale).toFloat()
            val yB = zeroY - (st.to * scale).toFloat()
            val y = minOf(yA, yB)
            val h = maxOf(kotlin.math.abs(yA - yB), 2f)

            drawRoundRect(
                color = st.color,
                topLeft = Offset(cx - barW / 2, y),
                size = Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                alpha = if (st.total) 1f else 0.88f
            )

            // 连接虚线
            if (i < n - 1 && !steps[i + 1].total) {
                val nextCx = gw * (i + 1) + gw / 2
                drawLine(
                    color = soft,
                    start = Offset(cx + barW / 2, yB),
                    end = Offset(nextCx - barW / 2, yB),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            }

            // 数值标签
            val valueText = (if (st.total) "" else if (st.value >= 0) "+" else "−") +
                    kotlin.math.abs(st.value).toInt().toString()
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = ink.toArgb()
                    textSize = 13.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(valueText, cx, y - 8f, paint)
            }

            // 类别标签
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = if (st.total) ink.toArgb() else soft.toArgb()
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = st.total
                }
                drawText(st.label, cx, size.height - 24f, paint)
            }
        }

        // 底部公式
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = soft.toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText("实际结余 = 收入 + 返利 − 支出", size.width / 2, size.height - 6f, paint)
        }
    }
}

private data class WaterfallStep(
    val label: String,
    val value: Double,
    val from: Double,
    val to: Double,
    val color: Color,
    val total: Boolean
)
