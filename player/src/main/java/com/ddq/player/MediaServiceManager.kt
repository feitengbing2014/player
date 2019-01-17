package com.ddq.player

import android.annotation.SuppressLint
import android.content.*
import android.os.IBinder
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.ProgressChanged

/**
 * created by dongdaqing 19-1-17 上午11:40
 */

class MediaServiceManager private constructor(private val context: Context) : BroadcastReceiver(), ServiceConnection {

    private var binder: ServiceBinder? = null
    private var started: Boolean = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MediaServiceManager

        fun get(context: Context): MediaServiceManager {
            if (!this::instance.isInitialized) {
                instance = MediaServiceManager(context.applicationContext)
            }
            return instance
        }
    }

    init {
        val filter = IntentFilter()
        with(filter) {
            addAction(Commands.ACTION_SERVICE_CREATED)
            addAction(Commands.ACTION_SERVICE_DESTROYED)
        }
        context.applicationContext.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Commands.ACTION_SERVICE_CREATED -> {
                started = true

            }
            Commands.ACTION_SERVICE_DESTROYED -> {
                started = false
                binder = null
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        binder = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binder = service as ServiceBinder?
    }

    fun next(any: Any) {
        action { binder?.next() }
    }

    fun previous() {
        action { binder?.previous() }
    }

    fun remove(index: Int) {
        action { binder?.remove(index) }
    }

    fun prepare(medias: List<MediaInfo>?) {
        action { binder?.prepare(medias) }
    }

    fun seekToWindow(position: Int) {
        action { binder?.seekToWindow(position) }
    }

    fun playOrPause() {
        action { binder?.playOrPause() }
    }

    fun nextPlayMode() {
        action { binder?.nextPlayMode() }
    }

    fun seekTo(percent: Int) {
        action { binder?.seekTo(percent) }
    }

    fun track(progressChanged: ProgressChanged) {
        action { binder?.track(progressChanged) }
    }

    fun pauseTracker() {
        action { binder?.pauseTracker() }
    }

    fun resumeTracker() {
        action { binder?.resumeTracker() }
    }

    fun unTrack() {
        action { binder?.unTrack() }
    }

    fun isPlaying(): Boolean? {
        return action { binder?.isPlaying() }
    }

    fun getCurrentMedia(): MediaInfo? {
        return action { binder?.getCurrentMedia() }
    }

    fun playlist(): List<MediaInfo>? {
        return action { binder?.playlist() }
    }

    fun setTimer(intent: Intent) {
        action { binder?.setTimer(intent) }
    }

    private fun <R> action(runnable: () -> R?): R? {
        if (binder == null) {
            if (started) {
                bindService(context)
            } else {
                startService(context)
            }
        } else {
            return runnable.invoke()
        }
        return null
    }

    private fun startService(context: Context) {
        context.startService(Intent(context, MediaService::class.java))
    }

    private fun bindService(context: Context) {
        context.bindService(Intent(context, MediaService::class.java), this, Context.BIND_AUTO_CREATE)
    }
}