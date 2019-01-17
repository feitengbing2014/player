package com.ddq.player.util

import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.Player

/**
 * created by dongdaqing 19-1-15 下午4:16
 *
 * query content position every 200ms
 */
class ProgressTracker(private val player: Player, private val progressChanged: ProgressChanged) : Runnable,
    Player.EventListener {

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var canceled: Boolean = false

    fun release() {
        canceled = true
        handler.removeCallbacks(this)
    }

    fun track() {
        canceled = false
        handler.removeCallbacks(this)
        handler.post(this)
    }

    override fun run() {
        if (!canceled) {
            progressChanged.onProgressChanged(player.currentPosition, player.duration)
            handler.postDelayed(this, 200)
        }
    }

    /**
     * listen playback state,stop tracking when player is no longer play
     * resume tracking when player start playing
     */
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playWhenReady && playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            track()
        } else {
            handler.removeCallbacks(this)
        }
    }
}

interface ProgressChanged {
    fun onProgressChanged(played: Long, duration: Long)
}