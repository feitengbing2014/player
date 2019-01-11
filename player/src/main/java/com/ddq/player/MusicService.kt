package com.ddq.player

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ddq.player.util.Timer
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerNotificationManager

/**
 * created by dongdaqing 19-1-11 下午1:46
 */
class MusicService : Service(), Player.EventListener {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private lateinit var source: MediaSource
    private lateinit var notificationManager: PlayerNotificationManager
    private lateinit var timer: Timer

    private val cmder = Cmder(this)

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val player: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            addListener(this@MusicService)
        }
    }

    override fun onCreate() {
        super.onCreate()
        //注册事件处理器
        cmder.register()
    }

    override fun onDestroy() {
        cmder.unregister()
        super.onDestroy()
    }

    fun setTimer(intent: Intent) {

    }

    fun setPlayerTarget(intent: Intent) {

    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

    }

    override fun onLoadingChanged(isLoading: Boolean) {

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

    }

    override fun onPlayerError(error: ExoPlaybackException?) {

    }

    override fun onPositionDiscontinuity(reason: Int) {

    }
}