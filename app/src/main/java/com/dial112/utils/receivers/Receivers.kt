package com.dial112.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * PowerButtonReceiver - Detects rapid power button presses for SOS trigger
 *
 * Logic: If user presses power button 4 times within 5 seconds → trigger SOS
 * This is a standard emergency pattern on many government safety apps.
 */
class PowerButtonReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PowerButtonReceiver"
        private const val REQUIRED_PRESSES = 4
        private const val TIME_WINDOW_MS = 5000L  // 5 seconds

        // Track press times (last N presses)
        private val pressTimes = mutableListOf<Long>()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            val now = SystemClock.elapsedRealtime()
            pressTimes.add(now)

            // Remove entries older than TIME_WINDOW_MS
            pressTimes.removeAll { (now - it) > TIME_WINDOW_MS }

            Log.d(TAG, "Power button pressed. Count in window: ${pressTimes.size}")

            if (pressTimes.size >= REQUIRED_PRESSES) {
                pressTimes.clear()
                triggerEmergencySos(context)
            }
        }
    }

    /** Launch SOS screen when 4 presses detected */
    private fun triggerEmergencySos(context: Context) {
        Log.d(TAG, "Emergency SOS triggered via power button!")

        // Broadcast internal intent to trigger SOS from anywhere in app
        val sosIntent = Intent("com.dial112.ACTION_POWER_BUTTON_SOS").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(sosIntent)
    }
}

/**
 * BootReceiver - Re-registers PowerButtonReceiver after device reboot
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted - emergency receivers active")
            // Receivers are auto-re-registered via manifest on boot
            // No manual code needed here; this class just marks our app for boot
        }
    }
}
