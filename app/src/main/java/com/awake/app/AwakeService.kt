package com.awake.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast

class AwakeService : Service() {

    companion object {
        const val ACTION_TOGGLE = "com.awake.app.TOGGLE"
        const val LOW_BATTERY = 15
        @Volatile var running = false
    }

    private var fresh = false
    private var batteryRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            if (level < 0 || scale <= 0) return
            val percent = level * 100 / scale
            if (plugged == 0 && percent <= LOW_BATTERY) {
                Toast.makeText(context, R.string.battery_stop, Toast.LENGTH_LONG).show()
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fresh = true

        val channelId = "awake"
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(open)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        running = true
        SwitchWidget.refresh(this)
        acquireWake()

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, filter)
        }
        batteryRegistered = true
    }

    @Suppress("DEPRECATION")
    private fun acquireWake() {
        try {
            val lock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "ScreenSwitch:keepAwake")
            lock.setReferenceCounted(false)
            lock.acquire()
            wakeLock = lock
        } catch (_: Exception) {}
    }

    private fun releaseWake() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        wakeLock = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE && !fresh) {
            stopSelf()
        }
        fresh = false
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        releaseWake()
        if (batteryRegistered) {
            try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
            batteryRegistered = false
        }
        SwitchWidget.refresh(this)
        super.onDestroy()
    }
}
