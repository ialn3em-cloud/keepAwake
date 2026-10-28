package com.awake.app

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
import android.widget.Toast

/** Pro: choose the apps that keep the screen on automatically. Shows the upgrade offer until Pro is bought. */
class AutoAppsActivity : Activity() {

    private class App(val pkg: String, val label: String, val icon: Drawable)

    private val bg = Color.parseColor("#1B1C1F")
    private val fg = Color.parseColor("#E6E8EB")
    private val dim = Color.parseColor("#8B9097")
    private val accent = Color.parseColor("#F0463F")

    private lateinit var master: Switch
    private lateinit var permBox: LinearLayout
    private lateinit var offer: LinearLayout
    private lateinit var buyButton: TextView
    private lateinit var pickLabel: TextView
    private lateinit var list: ListView
    private val proListener: () -> Unit = { render() }
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
            padForSystemBars()
        }

        root.addView(text(getString(R.string.auto_title), 22f, fg))
        root.addView(text(getString(R.string.auto_desc), 14f, dim).apply { setPadding(0, dp(6), 0, dp(12)) })

        offer = buildOffer()
        root.addView(offer)

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

        pickLabel = text(getString(R.string.auto_pick), 14f, dim).apply { setPadding(0, dp(12), 0, dp(4)) }
        root.addView(pickLabel)

        list = ListView(this).apply {
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
        Pro.addListener(proListener)
        Pro.start(this)
        // the user may come back from a settings screen after granting a permission
        render()
    }

    override fun onPause() {
        Pro.removeListener(proListener)
        super.onPause()
    }

    /** Locked: show the offer and grey out the settings. Unlocked: hide the offer. */
    private fun render() {
        val unlocked = Pro.isUnlocked(this)
        offer.visibility = if (unlocked) View.GONE else View.VISIBLE
        buyButton.text = Pro.price?.let { getString(R.string.pro_buy_price, it) } ?: getString(R.string.pro_buy)
        for (v in listOf(master, pickLabel, list)) {
            v.isEnabled = unlocked
            v.alpha = if (unlocked) 1f else 0.4f
        }
        refresh()
    }

    private fun buildOffer() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(16))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(Color.parseColor("#26282C"))
            setStroke(dp(2), Color.parseColor("#F5B700"))
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(12) }
        addView(text(getString(R.string.pro_title), 18f, fg).apply { setTypeface(typeface, Typeface.BOLD) })
        addView(text(getString(R.string.pro_points), 14f, fg).apply {
            setPadding(0, dp(8), 0, dp(12))
            setLineSpacing(dp(4).toFloat(), 1f)
        })
        buyButton = text("", 16f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                colors = intArrayOf(Color.parseColor("#D24435"), Color.parseColor("#B03226"))
            }
            setOnClickListener {
                if (!Pro.buy(this@AutoAppsActivity)) {
                    Toast.makeText(this@AutoAppsActivity, R.string.pro_unavailable, Toast.LENGTH_LONG).show()
                }
            }
        }
        addView(buyButton)
    }

    private fun refresh() {
        permBox.removeAllViews()
        if (master.isChecked && Pro.isUnlocked(this)) {
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
                    // Android's own "let app always run in background?" dialog; some phones lack it
                    try {
                        startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                        )
                    } catch (_: Exception) {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                })
            }
            // Android requires the notification while watching; the user may still hide it
            if (WatcherService.ensureChannel(this).importance != NotificationManager.IMPORTANCE_NONE) {
                permBox.addView(action(getString(R.string.auto_hide_notif), warning = false) {
                    startActivity(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, WatcherService.CHANNEL_ID)
                    )
                })
            }
        }
        WatcherService.sync(this)
    }

    private fun toggleApp(pkg: String) {
        if (!Pro.isUnlocked(this)) return
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

    private fun action(s: String, warning: Boolean = true, onClick: () -> Unit) =
        text(if (warning) "⚠  $s" else "🔕  $s", 14f, if (warning) accent else fg).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.parseColor(if (warning) "#2A1A1A" else "#26282C"))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(6)
            layoutParams = lp
            setOnClickListener { onClick() }
        }

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
