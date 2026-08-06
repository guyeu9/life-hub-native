package com.lifehub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.lifehub.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 撒花粒子效果，用于记录/完成等正向反馈。
 * 触发条件：外部传入 trigger 变化时播放；首次组合不触发。
 */
@Composable
fun ConfettiOverlay(
    trigger: Any,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Clay, Sage, Amber, Slate, ClayLight, SageLight, Danger, Gold)
) {
    val duration = 2800
    var active by remember { mutableStateOf(false) }
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    var prev by remember { mutableStateOf<Any?>(null) }

    val density = LocalDensity.current

    LaunchedEffect(trigger) {
        // 首次组合 prev 为 null，不触发；之后 trigger 变化才触发
        if (prev == null) {
            prev = trigger
            return@LaunchedEffect
        }
        if (trigger == prev) return@LaunchedEffect
        prev = trigger
        active = true
        particles = List(90) { Particle.random(colors) }
        delay(duration.toLong())
        active = false
        particles = emptyList()
    }

    if (!active) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val infinite = rememberInfiniteTransition(label = "confetti")
        val progress by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val t = (progress * duration + p.delay) / duration.toFloat()
                if (t <= 0f || t >= 1f) return@forEach
                val x = p.startX * widthPx + p.vx * t * widthPx + sin(t * p.wobbleFreq + p.phase) * 50f
                val y = p.startY * heightPx + p.vy * t * heightPx + 0.5f * 900f * t * t
                val rotation = p.rotation + p.rotationSpeed * t * 360f
                val alpha = if (t < 0.15f) t / 0.15f else (1f - t).coerceAtLeast(0f)
                // 用旋转的小矩形模拟纸屑，比圆点更明显
                val rad = Math.toRadians(rotation.toDouble())
                val hw = p.size * 0.9f
                val hh = p.size * 0.4f
                val center = Offset(x.coerceIn(0f, widthPx), y.coerceIn(0f, heightPx))
                val p1 = Offset(center.x + (cos(rad) * hw - sin(rad) * hh).toFloat(), center.y + (sin(rad) * hw + cos(rad) * hh).toFloat())
                val p2 = Offset(center.x + (-cos(rad) * hw - sin(rad) * hh).toFloat(), center.y + (-sin(rad) * hw + cos(rad) * hh).toFloat())
                val p3 = Offset(center.x + (-cos(rad) * hw + sin(rad) * hh).toFloat(), center.y + (-sin(rad) * hw - cos(rad) * hh).toFloat())
                val p4 = Offset(center.x + (cos(rad) * hw + sin(rad) * hh).toFloat(), center.y + (sin(rad) * hw - cos(rad) * hh).toFloat())
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    },
                    color = p.color.copy(alpha = alpha),
                    alpha = alpha
                )
            }
        }
    }
}

private data class Particle(
    val color: Color,
    val size: Float,
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val wobbleFreq: Float,
    val phase: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val delay: Float
) {
    companion object {
        fun random(colors: List<Color>): Particle {
            val startY = Random.nextFloat() * 0.25f + 0.05f
            return Particle(
                color = colors.random(),
                size = Random.nextFloat() * 8f + 6f,
                startX = Random.nextFloat() * 0.6f + 0.2f,
                startY = startY,
                vx = (Random.nextFloat() - 0.5f) * 1.8f,
                vy = Random.nextFloat() * -0.8f - 0.3f,
                wobbleFreq = Random.nextFloat() * 8f + 3f,
                phase = Random.nextFloat() * 6.28f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 6f,
                delay = Random.nextFloat() * 500f
            )
        }
    }
}
