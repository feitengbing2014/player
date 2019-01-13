package com.ddq.player

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
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
class MediaService : Service(), Player.EventListener {

    override fun onBind(intent: Intent?): IBinder? {
        return ServiceBinder()
    }

    private lateinit var timer: Timer
    private lateinit var source: MediaSource
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var musicNotification: MusicNotification
    private lateinit var notification: PlayerNotificationManager

    private val cmder = Cmder(this)

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val player: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            addListener(this@MediaService)
        }
    }

    override fun onCreate() {
        super.onCreate()
        //注册事件处理器
        cmder.register()
        musicNotification = MusicNotification(this)
    }

    override fun onDestroy() {
        cmder.unregister()
        player.release()
        super.onDestroy()
    }

    fun setTimer(intent: Intent) {

    }

    fun setPlayerTarget(intent: Intent) {
    }

    fun setRepeatMode(intent: Intent) {

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

    /******************************* controls ***********************/
    fun next() {
        player.next()
    }

    fun previous() {
        player.previous()
    }

    fun play(intent: Intent?) {
        player.playWhenReady = true
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun stop() {
        player.stop()
    }

    inner class ServiceBinder : Binder() {
        fun getService(): MediaService = this@MediaService
    }
}