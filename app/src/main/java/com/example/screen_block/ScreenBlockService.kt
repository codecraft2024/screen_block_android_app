package com.example.screen_block

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat


class ScreenBlockService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var unlockTapCount = 0
    private var lockTapCount = 0
    private var lastUnlockTapTime = 0L
    private var lastLockTapTime = 0L
    private var isLocked = true // Start in locked state by default
    private var OSUtil = OSUtil(this)
    companion object {
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "screen_block_channel"
        private const val CORNER_SIZE_DP = 100 // Size of the sensitive corner areas
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        setupOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Make service persistent
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Block Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps screen lock active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Lock Active")
            .setContentText("Tap corners to lock/unlock")
            .setSmallIcon(com.google.android.material.R.drawable.abc_btn_borderless_material)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            alpha = 0.1f  // Nearly transparent but still receives touches
            dimAmount = 0f
        }

        windowManager.addView(overlayView, params)
        setupTouchListener()
    }

    private fun setupTouchListener() {
        overlayView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> handleTouchEvent(event)
            }
            true
        }
    }

    private fun handleTouchEvent(event: MotionEvent) {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val cornerSizePx = (CORNER_SIZE_DP * metrics.density).toInt()

        when {
            isInTopRightCorner(event.rawX, event.rawY, metrics, cornerSizePx) ->
                handleUnlockTaps()

            isInTopLeftCorner(event.rawX, event.rawY, cornerSizePx) ->
                handleLockTaps()
        }
    }

    private fun isInTopRightCorner(x: Float, y: Float, metrics: DisplayMetrics, cornerSize: Int): Boolean {
        return x >= metrics.widthPixels - cornerSize && y <= cornerSize
    }

    private fun isInTopLeftCorner(x: Float, y: Float, cornerSize: Int): Boolean {
        return x <= cornerSize && y <= cornerSize
    }

    private fun handleUnlockTaps() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastUnlockTapTime > 10000) {
            unlockTapCount = 0
        }

        unlockTapCount++
        lastUnlockTapTime = currentTime

        if (unlockTapCount >= 5 && isLocked) {
            unlockTapCount = 0
            performUnlock()
        } else {
            provideHapticFeedback()
        }
    }

    private fun handleLockTaps() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastLockTapTime > 10000) {
            lockTapCount = 0
        }

        lockTapCount++
        lastLockTapTime = currentTime

        if (lockTapCount >= 5 && !isLocked) {
            lockTapCount = 0
            performLock()
        } else {
            provideHapticFeedback()
        }
    }

    private fun performUnlock() {
        isLocked = false
        OSUtil.showToast("Screen Unlocked")
        windowManager.removeView(overlayView)
        stopSelf()
    }

    private fun performLock() {
        isLocked = true
        OSUtil.showToast("Screen Locked")
        if (::overlayView.isInitialized) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) { /* Ignore */ }
        }
        setupOverlay()
    }



    private fun provideHapticFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::overlayView.isInitialized && ::windowManager.isInitialized) {
                windowManager.removeView(overlayView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}