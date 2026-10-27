package com.awake.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** A short, clearly felt click, independent of the system "touch vibration" setting. */
object Haptics {
    @Suppress("DEPRECATION")
    fun click(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }
            if (!vibrator.hasVibrator()) return
            val effect = if (Build.VERSION.SDK_INT >= 29) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else {
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } catch (_: Exception) {}
    }
}
