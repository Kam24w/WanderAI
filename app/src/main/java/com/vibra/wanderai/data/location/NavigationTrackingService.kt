package com.vibra.wanderai.data.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.vibra.wanderai.MainActivity
import com.vibra.wanderai.R

/**
 * Foreground service (type `location`) that keeps GPS fixes flowing while a route is
 * active and the phone is in a pocket with the screen off. It does no work itself:
 * [com.vibra.wanderai.domain.usecase.NavigationGuidanceController] collects the
 * locations in the application scope; this service only keeps the process allowed
 * to receive them and shows the ongoing "guiding your route" notification.
 */
class NavigationTrackingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: RuntimeException) {
            // Sin permiso de ubicación (SecurityException) o arrancado desde segundo plano.
            Log.w(TAG, "Could not start location tracking in the foreground", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.guidance_tracking_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.guidance_tracking_title))
            .setContentText(getString(R.string.guidance_tracking_body))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "NavigationTracking"
        private const val CHANNEL_ID = "route_guidance"
        private const val NOTIFICATION_ID = 4201

        /** Starts or stops tracking; failures (background start, no permission) are logged, never thrown. */
        fun setActive(context: Context, active: Boolean) {
            val intent = Intent(context, NavigationTrackingService::class.java)
            runCatching {
                if (active) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
            }.onFailure { Log.w(TAG, "Tracking service ${if (active) "start" else "stop"} failed", it) }
        }
    }
}
