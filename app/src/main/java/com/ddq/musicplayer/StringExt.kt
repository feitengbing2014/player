package com.ddq.musicplayer

/**
 * created by dongdaqing 19-1-15 下午2:01
 */

/**
 * format xx:xx
 */
fun Long.toMediaTime(): String {
    val seconds = this / 1000
    val minute = seconds / 60
    val second = seconds % 60

    return if (minute < 10)
        if (second < 10)
            "0$minute:0$second"
        else
            "0$minute:$second"
    else
        if (second < 10)
            "$minute:0$second"
        else
            "$minute:$second"
}