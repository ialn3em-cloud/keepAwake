package com.awake.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** The on/off click of each switch model. Sounds load on first use; one asked for while loading plays once ready. */
class ClickSounds(context: Context) {

    private val app = context.applicationContext
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val ids = mutableMapOf<Int, Int>()
    private val loaded = mutableSetOf<Int>()
    private var pending = 0

    init {
        pool.setOnLoadCompleteListener { sp, id, status ->
            if (status != 0) return@setOnLoadCompleteListener
            loaded.add(id)
            if (id == pending) {
                sp.play(id, 1f, 1f, 1, 0, 1f)
                pending = 0
            }
        }
    }

    /** Load a model's sounds ahead of time so the first tap is not silent. */
    fun preload(style: SwitchStyle) {
        idFor(res(style, true))
        idFor(res(style, false))
    }

    fun play(style: SwitchStyle, on: Boolean) {
        val id = idFor(res(style, on))
        if (id in loaded) pool.play(id, 1f, 1f, 1, 0, 1f) else pending = id
    }

    fun release() = pool.release()

    private fun idFor(res: Int) = ids.getOrPut(res) { pool.load(app, res, 1) }

    private fun res(style: SwitchStyle, on: Boolean): Int = when (style) {
        SwitchStyle.CLASSIC -> if (on) R.raw.switch_on else R.raw.switch_off
        SwitchStyle.GOLD -> if (on) R.raw.switch_gold_on else R.raw.switch_gold_off
        SwitchStyle.ROUND -> if (on) R.raw.switch_round_on else R.raw.switch_round_off
        SwitchStyle.FACTORY -> if (on) R.raw.switch_factory_on else R.raw.switch_factory_off
        SwitchStyle.MARBLE -> if (on) R.raw.switch_marble_on else R.raw.switch_marble_off
        SwitchStyle.KIDS -> if (on) R.raw.switch_kids_on else R.raw.switch_kids_off
        SwitchStyle.GLASS -> if (on) R.raw.switch_glass_on else R.raw.switch_glass_off
        SwitchStyle.BLACK -> if (on) R.raw.switch_black_on else R.raw.switch_black_off
        SwitchStyle.OAK -> if (on) R.raw.switch_oak_on else R.raw.switch_oak_off
    }
}
