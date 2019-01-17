package com.ddq.player

import android.content.*
import android.os.IBinder
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.ProgressChanged

/**
 * created by dongdaqing 19-1-17 上午11:40
 */

class MediaServiceManager private constructor(context: Context) : BroadcastReceiver(), ServiceConnection, Controls {

    private var binder: ServiceBinder? = null

    companion object {
        lateinit var instance: MediaServiceManager
        fun get(context: Context): MediaServiceManager {
            if (!this::instance.isInitialized) {
                instance = MediaServiceManager(context)
            }
            return instance
        }
    }

    init {
        val filter = IntentFilter()
        with(filter) {
            addAction(Commands.ACTION_SERVICE_DESTROYED)
        }
        context.applicationContext.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        binder = null
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        binder = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binder = service as ServiceBinder?
    }

    override fun next() {
    }

    override fun previous() {
    }

    override fun remove(index: Int) {
    }

    override fun prepare(medias: List<MediaInfo>?) {
    }

    override fun seekToWindow(position: Int) {
    }

    override fun playOrPause() {
    }

    override fun nextPlayMode() {
    }

    override fun seekTo(percent: Int) {
    }

    override fun track(progressChanged: ProgressChanged) {
    }

    override fun pauseTracker() {
    }

    override fun resumeTracker() {
    }

    override fun unTrack() {
    }

    override fun isPlaying(): Boolean {
    }

    override fun getCurrentMedia(): MediaInfo? {
    }

    override fun playlist(): List<MediaInfo>? {
    }

    override fun setTimer(intent: Intent) {
    }
}