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
 * 触发条件：外部传入 key 变化时自动播放一次。
 */
@Composable
fun ConfettiOverlay(
    trigger: Any,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Clay, Sage, Amber, Slate, ClayLight, SageLight)
) {
    var active by remember { mutableStateOf(false) }
    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    val density = LocalDensity.current

    LaunchedEffect(trigger) {
        active = true
        particles = List(40) { Particle.random(colors) }
        delay(2200)
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
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val t = (progress * 2200 + p.delay) / 2200f
                if (t <= 0f || t >= 1f) return@forEach
                val x = p.startX * widthPx + p.vx * t * widthPx + sin(t * p.wobbleFreq + p.phase) * 30f
                val y = p.startY * heightPx + p.vy * t * heightPx + 0.5f * 800f * t * t
                val rotation = p.rotation + p.rotationSpeed * t * 360f
                val alpha = if (t < 0.2f) t * 5f else (1f - t).coerceAtLeast(0f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size,
                    center = Offset(x.coerceIn(0f, widthPx), y.coerceIn(0f, heightPx)),
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
            val startY = Random.nextFloat() * 0.3f + 0.1f
            return Particle(
                color = colors.random(),
                size = Random.nextFloat() * 6f + 4f,
                startX = Random.nextFloat() * 0.6f + 0.2f,
                startY = startY,
                vx = (Random.nextFloat() - 0.5f) * 1.2f,
                vy = Random.nextFloat() * -0.6f - 0.2f,
                wobbleFreq = Random.nextFloat() * 6f + 2f,
                phase = Random.nextFloat() * 6.28f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 4f,
                delay = Random.nextFloat() * 400f
            )
        }
    }
}
