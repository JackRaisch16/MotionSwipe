package com.example.motionswipes

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class CameraForegroundService : Service() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        Log.d("CameraService", "✅ Foreground camera service created")
        startForegroundServiceWithNotification()
        startCameraAnalysis()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "motion_swipe_channel"
        val channelName = "MotionSwipe Detection"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MotionSwipe Running")
            .setContentText("Detecting gestures in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
        Log.d("CameraService", "✅ Notification started")
    }

    private fun startCameraAnalysis() {
        Log.d("CameraService", "🔄 Attempting to bind camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, MotionAnalyzer(applicationContext))
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    DummyLifecycleOwner(),
                    cameraSelector,
                    imageAnalysis
                )
                Log.d("CameraService", "✅ Camera bound successfully")
            } catch (e: Exception) {
                Log.e("CameraService", "❌ Failed to bind camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
