package com.awake.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var sw: SwitchView
    private lateinit var content: android.view.View
    private lateinit var chips: TimerChips
    private val ticker = Handler(Looper.getMainLooper())

    // shows the time left on the switch once a second while the screen is open
    private val tick = object : Runnable {
        override fun run() {
            val left = AwakeService.endAt - System.currentTimeMillis()
            sw.countdown = if (AwakeService.running && AwakeService.endAt > 0 && left > 0) {
                val s = (left + 999) / 1000
                "%d:%02d".format(s / 60, s % 60)
            } else {
                null
            }
            ticker.postDelayed(this, 1000)
        }
    }
    private lateinit var pool: SoundPool
    private var soundOn = 0
    private var soundOff = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#1B1C1F")
        window.navigationBarColor = Color.parseColor("#1B1C1F")

        pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        soundOn = pool.load(this, R.raw.switch_on, 1)
        soundOff = pool.load(this, R.raw.switch_off, 1)

        sw = SwitchView(this)
        sw.onToggle = { toggle() }
        chips = TimerChips(this)
        chips.select(AwakeService.timerMinutes(this))
        chips.onPick = { pickTimer(it) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(sw, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(chips, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        // the switch and the timer, with the automatic apps (Pro) card under them
        content = ProEntry.wrap(this, column)
        setContentView(content)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        sw.setOn(AwakeService.running, false)
        ProEntry.refresh(content)
        Pro.start(this)
        ticker.post(tick)
        // the service can stop by itself (low battery, missing permission) or be toggled from the widget/tile
        AwakeService.onStateChanged = {
            if (sw.isOn != AwakeService.running) sw.setOn(AwakeService.running, true)
        }
    }

    override fun onPause() {
        ticker.removeCallbacks(tick)
        AwakeService.onStateChanged = null
        super.onPause()
    }

    override fun onDestroy() {
        pool.release()
        super.onDestroy()
    }

    private fun pickTimer(minutes: Int) {
        AwakeService.setTimerMinutes(this, minutes)
        chips.select(minutes)
        Haptics.click(this)
        if (AwakeService.running) {
            // already on: count the new duration from now
            startService(Intent(this, AwakeService::class.java).setAction(AwakeService.ACTION_RESTART_TIMER))
        }
    }

    private fun feedback(on: Boolean) {
        pool.play(if (on) soundOn else soundOff, 1f, 1f, 1, 0, 1f)
        Haptics.click(this)
    }

    private fun toggle() {
        val service = Intent(this, AwakeService::class.java)
        if (AwakeService.running) {
            stopService(service)
            AwakeService.running = false
            sw.setOn(false, true)
            feedback(false)
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.need_permission, Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
                return
            }
            AwakeService.lowBattery(this)?.let {
                AwakeService.showLowBattery(this, it)
                return
            }
            startForegroundService(service)
            AwakeService.running = true
            sw.setOn(true, true)
            feedback(true)
        }
    }
}
