package com.awake.app

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class SwitchTileService : TileService() {

    override fun onStartListening() {
        update()
    }

    override fun onClick() {
        val service = Intent(this, AwakeService::class.java)
        if (AwakeService.running) {
            stopService(service)
            AwakeService.running = false
        } else {
            try {
                startForegroundService(service)
                AwakeService.running = true
            } catch (e: Exception) {
                openApp()
                return
            }
        }
        update()
    }

    @Suppress("DEPRECATION")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            startActivityAndCollapse(intent)
        }
    }

    private fun update() {
        val tile = qsTile ?: return
        val on = AwakeService.running
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = getString(if (on) R.string.tile_on else R.string.tile_off)
        }
        tile.updateTile()
    }
}
