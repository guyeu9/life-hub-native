package com.lifehub.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifehub.ui.theme.Clay
import com.lifehub.ui.theme.Ink
import com.lifehub.ui.theme.InkSoft
import com.lifehub.ui.theme.PaperCard
import com.lifehub.util.vibrateLight
import com.lifehub.util.vibrateSuccess
import com.lifehub.util.vibrateTick
import kotlinx.coroutines.delay

/**
 * 按压回弹 + 轻触感反馈。
 * 按下时轻微缩小，松开后弹簧回弹；同时触发系统点击震动。
 */
fun Modifier.pressScale(scale: Float = 0.96f, onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "pressScale"
    )
    this
        .graphicsLayer { this.scaleX = scaleAnim; this.scaleY = scaleAnim }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * 普通点击：轻触感 + 按压回弹。
 */
fun Modifier.hapticClick(context: Context? = null, onClick: () -> Unit): Modifier = composed {
    val ctx = context ?: LocalContext.current
    pressScale(onClick = {
        ctx.vibrateLight()
        onClick()
    })
}

/**
 * 保存/添加/完成等正向操作：成功震动 + 更明显的按压回弹。
 */
fun Modifier.successClick(context: Context? = null, onClick: () -> Unit): Modifier = composed {
    val ctx = context ?: LocalContext.current
    pressScale(scale = 0.94f, onClick = {
        ctx.vibrateSuccess()
        onClick()
    })
}

/**
 * 切换/勾选类：轻触感 + 明显回弹。
 */
fun Modifier.toggleClick(context: Context? = null, onClick: () -> Unit): Modifier = composed {
    val ctx = context ?: LocalContext.current
    pressScale(scale = 0.92f, onClick = {
        ctx.vibrateTick()
        onClick()
    })
}

/**
 * 列表项入场动画：淡入 + 从底部轻微滑入，按 index 错开。
 */
fun Modifier.animateItemSlide(index: Int): Modifier = composed {
    val delay = (index * 50).coerceAtMost(400)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(450, easing = EaseOutCubic),
        label = "slideAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 28f,
        animationSpec = tween(450, easing = EaseOutCubic),
        label = "slideOffset"
    )
    graphicsLayer { this.alpha = alpha; this.translationY = offsetY }
}

/**
 * 数字变化时的缩放跳动动画。
 */
@Composable
fun AnimatedNumber(value: String, color: Color = Ink) {
    var old by remember { mutableStateOf(value) }
    val changed = value != old
    LaunchedEffect(value) {
        if (changed) {
            delay(50)
            old = value
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (changed) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f),
        label = "numberScale"
    )
    Text(
        text = value,
        color = color,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.scale(scale)
    )
}

/**
 * 页面标题 + 副标题入场动画。
 */
@Composable
fun AnimatedHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = Ink,
            modifier = Modifier.animateContentSize()
        )
        subtitle?.let {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400, 120)) + slideInVertically(tween(400, 120)) { it / 3 }
            ) {
                Text(text = it, style = MaterialTheme.typography.labelMedium, color = InkSoft)
            }
        }
    }
}

/**
 * 保存/提交按钮：带成功震动。
 */
@Composable
fun SuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Clay
) {
    val ctx = LocalContext.current
    Button(
        onClick = {
            ctx.vibrateSuccess()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Text(text, color = PaperCard)
    }
}
