package com.awake.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Pro: bring the app watcher back after a restart or an update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WatcherService.sync(context)
    }
}
