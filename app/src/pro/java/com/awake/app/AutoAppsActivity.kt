package com.awake.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView

/** Pro: choose the apps that keep the screen on automatically. */
class AutoAppsActivity : Activity() {

    private class App(val pkg: String, val label: String, val icon: Drawable)

    private val bg = Color.parseColor("#1B1C1F")
    private val fg = Color.parseColor("#E6E8EB")
    private val dim = Color.parseColor("#8B9097")
    private val accent = Color.parseColor("#F0463F")

    private lateinit var master: Switch
    private lateinit var permBox: LinearLayout
    private val apps = mutableListOf<App>()
    private val adapter = AppsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(16), dp(16), 0)
        }

        root.addView(text(getString(R.string.auto_title), 22f, fg))
        root.addView(text(getString(R.string.auto_desc), 14f, dim).apply { setPadding(0, dp(6), 0, dp(12)) })

        master = Switch(this).apply {
            text = getString(R.string.auto_enable)
            setTextColor(fg)
            textSize = 16f
            isChecked = WatcherService.prefs(this@AutoAppsActivity).getBoolean(WatcherService.KEY_ENABLED, false)
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, on ->
                WatcherService.prefs(this@AutoAppsActivity).edit().putBoolean(WatcherService.KEY_ENABLED, on).apply()
                refresh()
            }
        }
        root.addView(master)

        permBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(permBox)

        root.addView(text(getString(R.string.auto_pick), 14f, dim).apply { setPadding(0, dp(12), 0, dp(4)) })

        val list = ListView(this).apply {
            this.adapter = this@AutoAppsActivity.adapter
            divider = null
            setOnItemClickListener { _, _, position, _ -> toggleApp(apps[position].pkg) }
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        // the user may come back from a settings screen after granting a permission
        refresh()
    }

    private fun refresh() {
        permBox.removeAllViews()
        if (master.isChecked) {
            if (!WatcherService.hasUsageAccess(this)) {
                permBox.addView(action(getString(R.string.auto_need_usage)) {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                })
                // installed outside Google Play: Android greys the option out until this is allowed
                permBox.addView(action(getString(R.string.auto_restricted)) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                })
            }
            if (!Settings.canDrawOverlays(this)) {
                permBox.addView(action(getString(R.string.need_permission)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                })
            }
            if (!getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)) {
                permBox.addView(action(getString(R.string.auto_need_battery)) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                })
            }
        }
        WatcherService.sync(this)
    }

    private fun toggleApp(pkg: String) {
        val chosen = HashSet(WatcherService.chosenApps(this))
        if (!chosen.add(pkg)) chosen.remove(pkg)
        WatcherService.prefs(this).edit().putStringSet(WatcherService.KEY_APPS, chosen).apply()
        adapter.notifyDataSetChanged()
        WatcherService.sync(this)
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val found = pm.queryIntentActivities(launcher, 0)
                .map { it.activityInfo }
                .filter { it.packageName != packageName }
                .distinctBy { it.packageName }
                .map { App(it.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            // chosen apps first, then alphabetical
            val chosen = WatcherService.chosenApps(this)
            val sorted = found.sortedWith(compareBy({ it.pkg !in chosen }, { it.label.lowercase() }))
            runOnUiThread {
                apps.clear()
                apps.addAll(sorted)
                adapter.notifyDataSetChanged()
            }
        }.start()
    }

    private inner class AppsAdapter : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(position: Int): Any = apps[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView as? LinearLayout ?: LinearLayout(this@AutoAppsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                addView(ImageView(context), LinearLayout.LayoutParams(dp(40), dp(40)))
                addView(text("", 16f, fg).apply { setPadding(dp(12), 0, dp(12), 0) },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(CheckBox(context).apply {
                    isClickable = false
                    isFocusable = false
                    buttonTintList = android.content.res.ColorStateList.valueOf(accent)
                })
            }
            val app = apps[position]
            (row.getChildAt(0) as ImageView).setImageDrawable(app.icon)
            (row.getChildAt(1) as TextView).text = app.label
            (row.getChildAt(2) as CheckBox).isChecked = app.pkg in WatcherService.chosenApps(this@AutoAppsActivity)
            return row
        }
    }

    private fun text(s: String, size: Float, color: Int) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
    }

    private fun action(s: String, onClick: () -> Unit) = text("⚠  $s", 14f, accent).apply {
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setBackgroundColor(Color.parseColor("#2A1A1A"))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(6)
        layoutParams = lp
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
