package com.example.screen_block

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ScreenBlockService : Service(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isLocked = false

    private lateinit var sensorManager: SensorManager

    private var lastShakeTime = 0L

    companion object {
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "screen_block_channel"
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 3000
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("mina", "onCreate:ScreenBlockService")

        createNotificationChannel()
        startForegroundService()
        setupOverlay()
        setupShakeDetection()
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Screen Block Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps screen lock active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val unlockIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenBlockService::class.java).apply {
                action = "UNLOCK_ACTION"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Lock Active")
            .setContentText("Shake to lock/unlock")
            .setSmallIcon(com.google.android.material.R.drawable.abc_btn_borderless_material)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "Unlock",
                    unlockIntent
                ).build()
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            when (it) {
                "UNLOCK_ACTION" -> performUnlock()
            }
        }
        return START_STICKY
    }

    private fun setupOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            alpha = 0.1f
            dimAmount = 0f
        }

        windowManager.addView(overlayView, params)
    }

    private fun setupShakeDetection() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            if (lastShakeTime + SHAKE_SLOP_TIME_MS > now) return
            lastShakeTime = now


            if (isLocked) {
                performUnlock()
            } else {
                performLock()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun performUnlock() {
        if (!isLocked) return
        isLocked = false

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "Screen Unlocked", Toast.LENGTH_SHORT).show()
        }

        try {
            overlayView?.let {
                windowManager.removeView(it)
                overlayView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performLock() {
        if (isLocked) return
        isLocked = true

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "Screen Locked", Toast.LENGTH_SHORT).show()
        }

        setupOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            overlayView?.let {
                windowManager.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        sensorManager.unregisterListener(this)
    }
}
