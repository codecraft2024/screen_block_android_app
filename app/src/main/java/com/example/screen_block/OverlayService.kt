package com.example.screen_block

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var tapCount = 0
    private var lastTapTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        startForeground()
        setupOverlay()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("Overlay Service")
            .setContentText("Service is running")
            .setSmallIcon(androidx.core.R.drawable.notification_bg) // Make sure you have this icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

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
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        
        windowManager.addView(overlayView, params)
        
        overlayView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isInTargetCorner(event.rawX, event.rawY)) {
                    val currentTime = System.currentTimeMillis()
                    
                    if (currentTime - lastTapTime > 10000) {
                        tapCount = 0
                    }
                    
                    tapCount++
                    lastTapTime = currentTime
                    
                    if (tapCount >=2) {
                        tapCount = 0
                        performUnlockAction()
                    }
                }
            }
            true
        }
    }

    private fun isInTargetCorner(x: Float, y: Float): Boolean {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        val cornerSize = 300 // pixels
        return x >= metrics.widthPixels - cornerSize && y <= cornerSize
    }

    private fun performUnlockAction() {
        Toast.makeText(applicationContext,"screen unlocked now",Toast.LENGTH_LONG).show()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}