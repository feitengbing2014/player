package com.ddq.player

import android.content.Intent
import android.os.Binder
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.ProgressChanged

internal class ServiceBinder(private val mediaService: MediaService) : Binder(), Controls {
    override fun position(): Long {
        return mediaService.position()
    }

    override fun isNavigationEnabled(): Boolean {
        return mediaService.isNavigationEnabled()
    }

    override fun setNavigationEnable(enable: Boolean) {
        mediaService.setNavigationEnable(enable)
    }

    override fun cancelTimer() {
        mediaService.cancelTimer()
    }

    override fun setTimer(intent: Intent) {
        mediaService.setTimer(intent)
    }

    override fun next() {
        mediaService.next()
    }

    override fun previous() {
        mediaService.previous()
    }

    override fun add(media: MediaInfo, index: Int) {
        mediaService.add(media, index)
    }

    override fun remove(index: Int) {
        mediaService.remove(index)
    }

    override fun remove(mediaInfo: MediaInfo) {
        mediaService.remove(mediaInfo)
    }

    override fun prepare(intent: Intent?) {
        mediaService.prepare(intent)
    }

    override fun prepare(medias: List<MediaInfo>?) {
        mediaService.prepare(medias)
    }

    override fun seekToWindow(position: Int) {
        mediaService.seekToWindow(position)
    }

    override fun playOrPause() {
        mediaService.playOrPause()
    }

    override fun nextPlayMode() {
        mediaService.nextPlayMode()
    }

    override fun seekTo(percent: Int) {
        mediaService.seekTo(percent)
    }

    override fun track(progressChanged: ProgressChanged) {
        mediaService.track(progressChanged)
    }

    override fun pauseTracker() {
        mediaService.pauseTracker()
    }

    override fun resumeTracker() {
        mediaService.resumeTracker()
    }

    override fun unTrack() {
        mediaService.unTrack()
    }

    override fun isPlaying(): Boolean {
        return mediaService.isPlaying()
    }

    override fun getCurrentMedia(): MediaInfo? {
        return mediaService.getCurrentMedia()
    }

    override fun playlist(): List<MediaInfo>? {
        return mediaService.playlist()
    }
}