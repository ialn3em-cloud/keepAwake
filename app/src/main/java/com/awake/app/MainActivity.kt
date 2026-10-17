package com.awake.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var sw: SwitchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#1B1C1F")
        window.navigationBarColor = Color.parseColor("#1B1C1F")

        sw = SwitchView(this)
        sw.onToggle = { toggle() }
        setContentView(sw)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        sw.setOn(AwakeService.running, false)
    }

    private fun toggle() {
        val service = Intent(this, AwakeService::class.java)
        if (AwakeService.running) {
            stopService(service)
            AwakeService.running = false
            sw.setOn(false, true)
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
            sw.setOn(true, true)
        }
    }
}
