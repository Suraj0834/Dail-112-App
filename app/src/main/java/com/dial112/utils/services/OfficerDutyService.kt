package com.dial112.utils.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.dial112.MainActivity
import com.dial112.R
import com.dial112.utils.SocketManager
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * OfficerDutyService - Foreground Service for live officer location tracking.
 *
 * When an officer goes on duty:
 * 1. Shows a persistent "On Duty" notification
 * 2. Requests GPS location every 1 second
 * 3. Emits `update_location` socket event for dashboard monitoring
 * 4. If a PCR van ID is passed, also emits `pcr_update_location`
 * 5. Listens for `sos_assigned` socket events and triggers system notifications
 *    (delegated entirely to SocketManager.showSosNotification)
 */
@AndroidEntryPoint
class OfficerDutyService : Service() {

    @Inject lateinit var socketManager: SocketManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** PCR van ID set via intent extra when officer is assigned to a van */
    private var pcrVanId: String? = null

    companion object {
        const val CHANNEL_ID = "officer_duty_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_STOP_DUTY = "ACTION_STOP_DUTY"
        const val EXTRA_PCR_VAN_ID = "extra_pcr_van_id"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_DUTY) {
            stopDutyService()
            return START_NOT_STICKY
        }

        pcrVanId = intent?.getStringExtra(EXTRA_PCR_VAN_ID)

        startForeground(NOTIFICATION_ID, buildNotification())

        // Connect socket and start location tracking concurrently
        serviceScope.launch {
            socketManager.connect(applicationContext)
        }
        startLocationTracking()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopDutyService()
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Location tracking (1-second interval)
    // -------------------------------------------------------------------------

    private fun startLocationTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1_000L  // 1-second update interval
        ).apply {
            setMinUpdateIntervalMillis(1_000L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val lat = location.latitude
                val lng = location.longitude

                // Send officer live location via socket
                socketManager.emitLocation(lat, lng)

                // If assigned to a PCR van, also update van location
                pcrVanId?.let { vanId ->
                    socketManager.emitPcrLocation(vanId, lat, lng)
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

    private fun stopDutyService() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        socketManager.emitGoOffDuty()
        socketManager.disconnect()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // -------------------------------------------------------------------------
    // Notification
    // -------------------------------------------------------------------------

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OfficerDutyService::class.java).apply {
            action = ACTION_STOP_DUTY
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

        val pcrText = if (pcrVanId != null) " • PCR Van tracking active" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_badge)
            .setContentTitle("On Duty — Location Sharing Active")
            .setContentText("Your live location is visible on the dashboard$pcrText")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Go Off Duty", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Officer Duty Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live location tracking while officer is on duty"
        }
        manager.createNotificationChannel(channel)
    }
}
