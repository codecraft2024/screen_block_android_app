package com.example.screen_block.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

object Utils {
    private var handler = Handler(Looper.getMainLooper())

    fun showNotification(context: Context, message: String) {
        val channelId = "screen_block_feedback_channel"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Feedback Notifications", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Screen Block Service")
            .setContentText(message)
            .setSmallIcon(com.google.android.material.R.drawable.abc_btn_borderless_material)
            .setAutoCancel(true)
            .build()

        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    fun provideHapticFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        30, VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}