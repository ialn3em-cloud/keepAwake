package com.awake.app

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.View
import android.view.WindowManager

/**
 * Pro: keeps the screen on while one of the chosen apps is in front.
 * Independent of the manual switch, so the two never turn each other off.
 */
class WatcherService : Service() {

    companion object {
        private const val PREFS = "auto_apps"
        const val KEY_ENABLED = "enabled"
        const val KEY_APPS = "apps"
        private const val TICK_MS = 1500L
        const val CHANNEL_ID = "watch"

        /** The channel of the "watching your apps" notification, so the user can hide it in settings. */
        fun ensureChannel(context: Context): NotificationChannel {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.getNotificationChannel(CHANNEL_ID)?.let { return it }
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.auto_title), NotificationManager.IMPORTANCE_MIN)
            manager.createNotificationChannel(channel)
            return channel
        }

        fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun chosenApps(context: Context): Set<String> =
            prefs(context).getStringSet(KEY_APPS, emptySet()) ?: emptySet()

        @Suppress("DEPRECATION")
        fun hasUsageAccess(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java)
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        /** Start or stop the watcher to match the settings and permissions. */
        fun sync(context: Context) {
            val wanted = prefs(context).getBoolean(KEY_ENABLED, false) &&
                chosenApps(context).isNotEmpty() &&
                hasUsageAccess(context) &&
                Settings.canDrawOverlays(context)
            val intent = Intent(context, WatcherService::class.java)
            try {
                if (wanted) context.startForegroundService(intent) else context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var foreground: String? = null
    private var lastQuery = 0L

    private val tick = object : Runnable {
        override fun run() {
            check()
            handler.postDelayed(this, TICK_MS)
        }
    }

    // no need to look at apps while the screen is off
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handler.removeCallbacks(tick)
            if (intent.action == Intent.ACTION_SCREEN_ON) handler.post(tick) else release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, AutoAppsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.auto_notif_title))
            .setContentText(getString(R.string.auto_notif_text, chosenApps(this).size))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(2, notification)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        // look back a little so an app that is already open is found right away
        lastQuery = System.currentTimeMillis() - 60 * 60_000L
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    @Suppress("DEPRECATION")
    private fun check() {
        val now = System.currentTimeMillis()
        try {
            val events = getSystemService(UsageStatsManager::class.java).queryEvents(lastQuery, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) foreground = e.packageName
            }
            // small overlap so an event stamped right at the edge is not missed
            lastQuery = now - 3000
        } catch (_: Exception) {}

        val app = foreground
        val wanted = app != null && app in chosenApps(this) && AwakeService.lowBattery(this) == null
        if (wanted) hold() else release()
    }

    @Suppress("DEPRECATION")
    private fun hold() {
        if (overlay != null) return
        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        val view = View(this)
        try {
            getSystemService(WindowManager::class.java).addView(view, params)
        } catch (_: Exception) {
            // overlay permission was taken away
            stopSelf()
            return
        }
        overlay = view
        try {
            val lock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "ScreenSwitch:auto")
            lock.setReferenceCounted(false)
            lock.acquire()
            wakeLock = lock
        } catch (_: Exception) {}
    }

    private fun release() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        wakeLock = null
        overlay?.let {
            try { getSystemService(WindowManager::class.java).removeView(it) } catch (_: Exception) {}
        }
        overlay = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        release()
        super.onDestroy()
    }
}
