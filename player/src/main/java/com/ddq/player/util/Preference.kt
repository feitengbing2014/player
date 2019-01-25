package com.ddq.player.util

import android.content.Context
import com.google.android.exoplayer2.Player

/*
* dongdaqing 2019/1/13 0013 21:17
*/
private const val PREFERENCE = "player.preference"

class Preference(context: Context) {

    private val preference = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)

    fun setSmallIcon(icon: Int): Preference {
        preference.edit().putInt("n_s_i", icon).apply()
        return this
    }

    fun setPlayMode(modes: Array<Int>): Preference {
        val values = modes.joinToString(",")
        preference.edit().putString("n_p_m", values).apply()
        return this
    }

    fun setTargetPage(string: String) {
        preference.edit().putString("n_t_p", string).apply()
    }

    fun getSmallIcon(): Int {
        return preference.getInt("n_s_i", 0)
    }

    fun getTargetPage(): String? {
        return preference.getString("n_t_p", null)
    }

    fun getPlayMode(): Array<Int> {
        val modes =
            preference.getString(
                "n_p_m",
                "${Player.REPEAT_MODE_OFF},${Player.REPEAT_MODE_ONE},${Player.REPEAT_MODE_ALL}"
            )

        return modes!!.toIntArray(',')
    }

    fun setRepeatMode(mode: Int) {
        preference.edit().putInt("n_r_m", mode).apply()
    }

    fun getRepeatMode(): Int {
        return preference.getInt("n_r_m", -1)
    }

    fun String.toIntArray(separator: Char): Array<Int> {
        val sps = split(separator)

        val array = Array(sps.size) { 0 }
        for (i in 0..(sps.size - 1)) {
            array[i] = sps[i].toInt()
        }

        return array
    }
}
