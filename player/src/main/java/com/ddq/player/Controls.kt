package com.ddq.player

import android.content.Intent
import android.os.Bundle
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.ProgressChanged

/**
 * created by dongdaqing 19-1-17 上午11:13
 */
interface Controls {
    fun next()
    fun previous()
    fun add(bundle: Bundle)
    fun remove(index: Int)
    fun remove(mediaInfo: MediaInfo)
    fun prepare(intent: Intent?)
    fun prepare(medias: List<MediaInfo>?)
    fun seekToWindow(position: Int)
    fun seekToWindow(position: Int, ms: Long)
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
    fun cancelTimer()
    fun isNavigationEnabled(): Boolean
    fun setNavigationEnable(enable: Boolean)
    fun position(): Long
}