package com.ddq.player.util

import android.content.Context

/*
* dongdaqing 2019/1/13 0013 21:17
*/
class MediaPreference {
    companion object {
        const val PREFERENCE = "player.preference"

        fun setNotificationSmallIcon(context: Context, icon: Int) {
            val editor = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE).edit()
            editor.putInt("n_s_i", icon)
            editor.apply()
        }

        fun getNotificationSmallIcon(context: Context): Int {
            val sp = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
            return sp.getInt("n_s_i", -1)
        }

        fun setNotificationTargetPage(context: Context, string: String) {
            val editor = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE).edit()
            editor.putString("n_t_p", string)
            editor.apply()
        }

        fun getNotificationTargetPage(context: Context): String? {
            val sp = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
            return sp.getString("n_t_p", null)
        }
    }
}
