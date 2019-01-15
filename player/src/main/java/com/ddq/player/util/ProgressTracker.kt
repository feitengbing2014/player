package com.ddq.player.util

import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.Player

/**
 * created by dongdaqing 19-1-15 下午4:16
 *
 * query playing time every 200ms
 */
class ProgressTracker(private val player: Player, private val progressChanged: ProgressChanged) : Runnable {

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var canceled: Boolean = false

    fun release() {
        canceled = true
        handler.removeCallbacks(this)
    }

    fun track() {
        canceled = false
        handler.post(this)
    }

    override fun run() {
        if (!canceled) {
            progressChanged.onProgressChanged(player.currentPosition, player.duration)
            handler.postDelayed(this, 200)
        }
    }
}

interface ProgressChanged {
    fun onProgressChanged(played: Long, duration: Long)
}