package com.awake.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.service.quicksettings.TileService
import android.view.View
import android.view.WindowManager
import android.widget.Toast

class AwakeService : Service() {

    companion object {
        const val ACTION_TOGGLE = "com.awake.app.TOGGLE"
        const val ACTION_RESTART_TIMER = "com.awake.app.RESTART_TIMER"
        private const val KEY_MINUTES = "timer_minutes"
        const val LOW_BATTERY = 10
        @Volatile var running = false

        /** When the timer will turn the screen off (System.currentTimeMillis), or 0 when there is no timer. */
        @Volatile var endAt = 0L

        /** The chosen timer: 10, 15 or 30 minutes, or 0 for "always". Used wherever the switch is turned on. */
        fun timerMinutes(context: Context): Int = settings(context).getInt(KEY_MINUTES, 0)

        fun setTimerMinutes(context: Context, minutes: Int) {
            settings(context).edit().putInt(KEY_MINUTES, minutes).apply()
        }

        private fun settings(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        // set by MainActivity while it is on screen, so the switch follows the real state
        var onStateChanged: (() -> Unit)? = null

        /** Battery percent when it is at or below LOW_BATTERY and not charging, otherwise null. */
        fun lowBattery(context: Context): Int? {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return null
            return lowBattery(intent)
        }

        private fun lowBattery(intent: Intent): Int? {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            if (level < 0 || scale <= 0) return null
            val percent = level * 100 / scale
            return if (plugged == 0 && percent <= LOW_BATTERY) percent else null
        }

        fun showLowBattery(context: Context, percent: Int) {
            Toast.makeText(context, context.getString(R.string.battery_low_start, percent), Toast.LENGTH_LONG).show()
        }

        /** Refresh every place that shows the switch: widget, quick settings tile and the open app. */
        fun notifyState(context: Context) {
            SwitchWidget.refresh(context)
            try {
                TileService.requestListeningState(context, ComponentName(context, SwitchTileService::class.java))
            } catch (_: Exception) {}
            onStateChanged?.invoke()
        }
    }

    private var overlay: View? = null
    private var fresh = false
    private var batteryRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val handler = Handler(Looper.getMainLooper())
    private val timerDone = Runnable { stopSelf() }
    private var pool: SoundPool? = null
    private var soundOn = 0
    private var soundOff = 0
    private var pendingSound = 0
    private val loadedSounds = mutableSetOf<Int>()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (lowBattery(intent) != null) {
                Toast.makeText(context, R.string.battery_stop, Toast.LENGTH_LONG).show()
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fresh = true
        initSounds()

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

        // the widget starts the service directly, so check the battery here too
        lowBattery(this)?.let {
            showLowBattery(this, it)
            stopSelf()
            return
        }

        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        overlay = View(this)
        try {
            getSystemService(WindowManager::class.java).addView(overlay, params)
        } catch (e: Exception) {
            overlay = null
            Toast.makeText(this, R.string.need_permission, Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        running = true
        startTimer()
        notifyState(this)
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

    private fun initSounds() {
        val p = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        p.setOnLoadCompleteListener { sp, id, status ->
            if (status == 0) {
                loadedSounds.add(id)
                if (id == pendingSound) {
                    sp.play(id, 1f, 1f, 1, 0, 1f)
                    pendingSound = 0
                }
            }
        }
        soundOn = p.load(this, R.raw.switch_on, 1)
        soundOff = p.load(this, R.raw.switch_off, 1)
        pool = p
    }

    private fun playSound(id: Int) {
        val p = pool ?: return
        if (loadedSounds.contains(id)) {
            p.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            pendingSound = id
        }
    }

    /** (Re)start the countdown from the chosen duration; "always" just clears it. */
    private fun startTimer() {
        handler.removeCallbacks(timerDone)
        val minutes = timerMinutes(this)
        if (minutes > 0) {
            val ms = minutes * 60_000L
            endAt = System.currentTimeMillis() + ms
            handler.postDelayed(timerDone, ms)
        } else {
            endAt = 0L
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESTART_TIMER && running) {
            // the user picked another duration while the switch is on
            startTimer()
        } else if (intent?.action == ACTION_TOGGLE) {
            if (!fresh) {
                // the service was already running: this tap turns it off
                playSound(soundOff)
                Haptics.click(this)
                handler.postDelayed({ stopSelf() }, 300)
            } else if (running) {
                // the service was just created by this tap: it is turning on
                playSound(soundOn)
                Haptics.click(this)
            }
        }
        fresh = false
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        endAt = 0L
        releaseWake()
        handler.removeCallbacksAndMessages(null)
        pool?.release()
        pool = null
        if (batteryRegistered) {
            try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
            batteryRegistered = false
        }
        overlay?.let {
            try { getSystemService(WindowManager::class.java).removeView(it) } catch (_: Exception) {}
        }
        overlay = null
        notifyState(this)
        super.onDestroy()
    }
}
