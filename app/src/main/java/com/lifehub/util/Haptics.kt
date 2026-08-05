package com.lifehub.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

fun Context.vibrateLight() = vibrate(VibrationEffect.EFFECT_CLICK)
fun Context.vibrateMedium() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
fun Context.vibrateSuccess() = vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
fun Context.vibrateTick() = vibrate(VibrationEffect.EFFECT_TICK)

private fun Context.vibrate(effectId: Int) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }
    }
}
