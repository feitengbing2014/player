package com.ddq.player

import android.content.Intent
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.ProgressChanged

/**
 * created by dongdaqing 19-1-17 上午11:13
 */
interface Controls {
    fun next()
    fun previous()
    fun remove(index: Int)
    fun prepare(medias: List<MediaInfo>?)
    fun seekToWindow(position: Int)
    fun playOrPause()
    fun nextPlayMode()
    fun seekTo(percent: Int)
    fun track(progressChanged: ProgressChanged)
    fun pauseTracker()
    fun resumeTracker()
    fun unTrack()
    fun isPlaying(): Boolean
    fun getCurrentMedia(): MediaInfo?
    fun playlist(): List<MediaInfo>?
    fun setTimer(intent: Intent)
}