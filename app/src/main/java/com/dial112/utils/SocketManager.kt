package com.dial112.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dial112.BuildConfig
import com.dial112.R
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SocketManager - Singleton that owns the Socket.IO connection.
 *
 * Responsibilities:
 * - Connect / disconnect with JWT authentication
 * - Emit `update_location` every second from OfficerDutyService
 * - Emit `pcr_update_location` for PCR van tracking
 * - Listen for `sos_assigned` and fire a system notification + expose a SharedFlow
 */
@Singleton
class SocketManager @Inject constructor(
    private val sessionManager: SessionManager
) {
    companion object {
        const val SOS_NOTIFICATION_CHANNEL_ID = "sos_assignment_channel"
        const val SOS_NOTIFICATION_ID = 2001
    }

    private var socket: Socket? = null

    /** Emits SOS assignment data when server assigns an officer */
    private val _sosAssigned = MutableSharedFlow<SosAssignmentEvent>(extraBufferCapacity = 4)
    val sosAssigned: SharedFlow<SosAssignmentEvent> = _sosAssigned

    // -------------------------------------------------------------------------
    // Connection management
    // -------------------------------------------------------------------------

    suspend fun connect(context: Context) {
        if (socket?.connected() == true) return

        val token = sessionManager.getToken() ?: return

        try {
            val options = IO.Options.builder()
                .setTransports(arrayOf("websocket"))
                .setAuth(mapOf("token" to token))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(2000L)
                .build()

            socket = IO.socket(BuildConfig.SOCKET_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "connected")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.w("SocketManager", "connect error ${args.firstOrNull()}")
            }

            socket?.on("sos_assigned") { args ->
                val json = args.firstOrNull() as? JSONObject ?: return@on
                val event = SosAssignmentEvent(
                    sosId = json.optString("sosId"),
                    latitude = json.optDouble("latitude"),
                    longitude = json.optDouble("longitude"),
                    address = json.optString("address"),
                    type = json.optString("type", "SOS"),
                    callerName = json.optJSONObject("triggeredBy")?.optString("name") ?: "Unknown",
                    callerPhone = json.optJSONObject("triggeredBy")?.optString("phone") ?: ""
                )
                _sosAssigned.tryEmit(event)
                showSosNotification(context, event)
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("SocketManager", "failed to connect", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    val isConnected: Boolean get() = socket?.connected() == true

    // -------------------------------------------------------------------------
    // Emit events
    // -------------------------------------------------------------------------

    /** Send officer live location (1-second cadence) */
    fun emitLocation(latitude: Double, longitude: Double) {
        if (socket?.connected() != true) return
        val payload = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
        }
        socket?.emit("update_location", payload)
    }

    /** Send PCR van live location */
    fun emitPcrLocation(vanId: String, latitude: Double, longitude: Double) {
        if (socket?.connected() != true) return
        val payload = JSONObject().apply {
            put("vanId", vanId)
            put("latitude", latitude)
            put("longitude", longitude)
        }
        socket?.emit("pcr_update_location", payload)
    }

    fun emitGoOffDuty() {
        socket?.emit("go_off_duty")
    }

    // -------------------------------------------------------------------------
    // System notification for SOS assignment
    // -------------------------------------------------------------------------

    private fun showSosNotification(context: Context, event: SosAssignmentEvent) {
        createSosNotificationChannel(context)

        // Deep-link intent: open Google Maps with directions to SOS location
        val mapsUri = Uri.parse(
            "google.navigation:q=${event.latitude},${event.longitude}&mode=d"
        )
        val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        val mapsPendingIntent = PendingIntent.getActivity(
            context,
            event.sosId.hashCode(),
            mapsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fallback web maps intent
        val webUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${event.latitude},${event.longitude}&travelmode=driving"
        )
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        val webPending = PendingIntent.getActivity(
            context,
            event.sosId.hashCode() + 1,
            webIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SOS_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sos)
            .setContentTitle("🚨 SOS Assigned to You")
            .setContentText("From: ${event.callerName} • ${event.address.ifBlank { "See map" }}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Caller: ${event.callerName} (${event.callerPhone})\nLocation: ${event.address}\nType: ${event.type}")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .addAction(
                R.drawable.ic_map_pin,
                "Navigate",
                if (isMapsInstalled(context)) mapsPendingIntent else webPending
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SOS_NOTIFICATION_ID, notification)
    }

    private fun createSosNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(SOS_NOTIFICATION_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            SOS_NOTIFICATION_CHANNEL_ID,
            "SOS Assignment Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies officer when an SOS is assigned"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
        }
        manager.createNotificationChannel(channel)
    }

    private fun isMapsInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.maps", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Data class holding all info from a `sos_assigned` socket event
 */
data class SosAssignmentEvent(
    val sosId: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String,
    val callerName: String,
    val callerPhone: String
)
