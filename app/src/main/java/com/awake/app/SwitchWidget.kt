package com.awake.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SwitchWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, build(context))
    }

    companion object {
        fun build(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget)
            // drawn with the same code as the app, so the widget always matches the chosen model
            views.setImageViewBitmap(
                R.id.widget_img,
                SwitchArt.bitmap(SwitchStyle.current(context), AwakeService.running, 320)
            )
            val intent = Intent(context, AwakeService::class.java).setAction(AwakeService.ACTION_TOGGLE)
            val pending = PendingIntent.getForegroundService(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_img, pending)
            return views
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SwitchWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, build(context))
        }
    }
}
