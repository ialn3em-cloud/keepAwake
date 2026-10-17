package com.awake.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        button = Button(this).apply {
            textSize = 30f
            setTextColor(Color.WHITE)
            isAllCaps = false
            setOnClickListener { toggle() }
        }
        val root = FrameLayout(this)
        val h = (160 * resources.displayMetrics.density).toInt()
        val m = (32 * resources.displayMetrics.density).toInt()
        root.addView(
            button,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h, Gravity.CENTER)
                .apply { setMargins(m, 0, m, 0) }
        )
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun toggle() {
        val service = Intent(this, AwakeService::class.java)
        if (AwakeService.running) {
            stopService(service)
            AwakeService.running = false
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.need_permission, Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
                return
            }
            startForegroundService(service)
            AwakeService.running = true
        }
        refresh()
    }

    private fun refresh() {
        val on = AwakeService.running
        button.setText(if (on) R.string.btn_off else R.string.btn_on)
        button.background = GradientDrawable().apply {
            cornerRadius = 48f
            setColor(if (on) Color.parseColor("#E53935") else Color.parseColor("#43A047"))
        }
    }
}
