package com.dial112.utils.services

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.dial112.R
import com.dial112.domain.repository.SosRepository
import com.dial112.MainActivity
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * SosEmergencyService - Foreground Service for Emergency SOS
 *
 * When SOS is triggered, this service:
 * 1. Starts a persistent foreground notification (user cannot dismiss)
 * 2. Continuously sends GPS location updates every 10 seconds
 * 3. Notifies the backend socket for real-time tracking
 * 4. Keeps running even when app is backgrounded/killed
 */
@AndroidEntryPoint
class SosEmergencyService : Service() {

    @Inject lateinit var sosRepository: SosRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "sos_emergency_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SOS = "STOP_SOS"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SOS) {
            stopSosService()
            return START_NOT_STICKY
        }

        // Show persistent foreground notification
        startForeground(NOTIFICATION_ID, buildNotification())

        // Start continuous location tracking
        startLocationTracking()

        return START_STICKY // Restart if killed by system
    }

    /**
     * Start GPS location updates every 10 seconds
     * and send each update to the SOS backend
     */
    private fun startLocationTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L  // Update interval: 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5_000L)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                serviceScope.launch {
                    // Send location update to backend via SOS repository
                    sosRepository.triggerSos(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = "Auto-tracked: ${location.latitude}, ${location.longitude}",
                        type = "SOS_UPDATE"
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    /** Stop service and remove foreground notification */
    private fun stopSosService() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Build the persistent SOS notification */
    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, SosEmergencyService::class.java).apply {
            action = ACTION_STOP_SOS
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 SOS ACTIVE - Emergency Alert Sent")
            .setContentText("Your location is being shared with police. Tap to open app.")
            .setSmallIcon(R.drawable.ic_sos)
            .setColor(0xFFE53935.toInt())
            .setOngoing(true)                 // Cannot be dismissed
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_stop, "Cancel SOS", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    /** Create high-priority notification channel for SOS alerts */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SOS Emergency Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent notification during active SOS emergency"
            enableVibration(true)
            enableLights(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
