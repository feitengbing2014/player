package com.ddq.player

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.ddq.player.data.*
import com.ddq.player.util.MediaPreference
import com.ddq.player.util.MediaTimer
import com.ddq.player.util.ProgressChanged
import com.ddq.player.util.ProgressTracker
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

/**
 * created by dongdaqing 19-1-11 下午1:46
 */
class MediaService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return ServiceBinder()
    }

    private var timer: MediaTimer? = null
    private var mediaSource: ConcatenatingMediaSource? = null
    private var tracker: ProgressTracker? = null

    private lateinit var musicNotification: MusicNotification
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private val cmder = Cmder(this)
    private val mediaCompleteListener = Runnable {
        /**
         * timer is counting for current media
         */
        if (timer != null && timer!!.isCountForCurrent()) {
            pause()
        }
    }

    private val listener = object : Player.EventListener {
        /******************************* callbacks ***********************/

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            Log.d(
                "MediaService", "onTimelineChanged:${
                when (reason) {
                    TIMELINE_CHANGE_REASON_PREPARED -> "TIMELINE_CHANGE_REASON_PREPARED"
                    TIMELINE_CHANGE_REASON_RESET -> "TIMELINE_CHANGE_REASON_RESET"
                    TIMELINE_CHANGE_REASON_DYNAMIC -> "TIMELINE_CHANGE_REASON_DYNAMIC"
                    else -> "UNKNOWN"
                }
                }"
            )
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            Log.d("MediaService", "onTracksChanged,track info:${player.currentTag}")
            val mediaInfo = player.currentTag

            if (mediaInfo != null) {
                val intent = Intent(Commands.ACTION_TRACK_CHANGED)
                val media = mediaInfo as MediaInfo
                val duration = player.duration
                intent.putExtra("media", media)
                intent.putExtra("duration", if (duration > 0L) duration else media.duration)
                sendBroadcast(intent)
            }
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            Log.d(
                "MediaService",
                "onLoadingChanged:$isLoading,buffer:${player.bufferedPercentage}"
            )
            val intent = Intent(Commands.ACTION_LOADING_CHANGED)
            intent.putExtra("loading", isLoading)
            sendBroadcast(intent)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.d(
                "MediaService", "onPlayerStateChanged:$playWhenReady,playbackState:${
                when (playbackState) {
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    Player.STATE_BUFFERING -> "STATE_BUFFERING"
                    Player.STATE_READY -> "STATE_READY"
                    else -> "STATE_UNKNOWN"
                }
                }"
            )
            if (isPlaying()) {
                timer?.resume()
            } else {
                timer?.pause()
            }

            val intent = Intent(Commands.ACTION_PLAY_STATE_CHANGED)
            intent.putExtra("play", isPlaying())
            sendBroadcast(intent)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.d("MediaService", "onRepeatModeChanged:$repeatMode")
            val intent = Intent(Commands.ACTION_REPEAT_MODE_CHANGED)
            intent.putExtra("mode", repeatMode)
            intent.putExtra("shuffle", player.shuffleModeEnabled)
            sendBroadcast(intent)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Log.d("MediaService", "onShuffleModeEnabledChanged:$shuffleModeEnabled")
            val intent = Intent(Commands.ACTION_REPEAT_MODE_CHANGED)
            intent.putExtra("mode", player.repeatMode)
            intent.putExtra("shuffle", shuffleModeEnabled)
            sendBroadcast(intent)
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Log.e(
                "MediaService", "onPlayerError:${
                when (error?.type) {
                    ExoPlaybackException.TYPE_SOURCE -> "TYPE_SOURCE"
                    ExoPlaybackException.TYPE_RENDERER -> "TYPE_RENDERER"
                    ExoPlaybackException.TYPE_UNEXPECTED -> "TYPE_UNEXPECTED"
                    else -> {
                        "TYPE_UNKNOWN"
                    }
                }
                }"
            )
        }

        override fun onPositionDiscontinuity(reason: Int) {
            Log.d("MediaService", "onPositionDiscontinuity:$reason")
            val intent = Intent(Commands.ACTION_POSITION_DISCONTINUITY_CHANGED)
            intent.putExtra("reason", reason)
            sendBroadcast(intent)
        }
    }

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val player: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            addListener(listener)
            musicNotification.player = this
            playerNotificationManager.setPlayer(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        //注册事件处理器
        cmder.register()
        musicNotification = MusicNotification(this)
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, packageName))
        playerNotificationManager = PlayerNotificationManager(
            this,
            "com.ddq.player.media.NOW_PLAYING",
            1,
            object : MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                    val intent = Intent()
                    val target = MediaPreference.getNotificationTargetPage(this@MediaService)
                    if (target != null)
                        intent.component = ComponentName(this@MediaService, target)
                    return PendingIntent.getActivity(this@MediaService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                }

                override fun getCurrentContentText(player: Player?): String? {
                    if (player?.currentTag != null) {
                        val mediaInfo = player.currentTag as MediaInfo
                        return mediaInfo.mediaDesc
                    }
                    return null
                }

                override fun getCurrentContentTitle(player: Player?): String? {
                    if (player?.currentTag != null) {
                        val mediaInfo = player.currentTag as MediaInfo
                        return mediaInfo.mediaName
                    }
                    return null
                }

                override fun getCurrentLargeIcon(
                    player: Player?,
                    callback: PlayerNotificationManager.BitmapCallback?
                ): Bitmap? {
                    return null
                }
            }).apply {
            setSmallIcon(MediaPreference.getNotificationSmallIcon(this@MediaService))
        }

        PlayerNotificationManager.createWithNotificationChannel(
            this,
            "com.ddq.player.media.NOW_PLAYING",
            R.string.app_name,
            1,
            null
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Commands.SET_PLAYER_PLAY == intent?.action) {
            play(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("MediaService", "onDestroy")
        cmder.unregister()
        player.release()
        super.onDestroy()
    }

    fun setTimer(intent: Intent) {
        timer?.cancel()

        @Suppress("UNCHECKED_CAST")
        val actions = intent.getSerializableExtra("actions") as List<TimerAction>?

        timer = MediaTimer(
            this,
            actions?.toPendingActions(),
            intent.getIntExtra("type", MediaTimer.TYPE_NORMAL),
            intent.getLongExtra("mills", 0)
        )

        if (player.playWhenReady) {
            timer?.start()
        } else {
            timer?.pause()
        }
    }

    fun setRepeatMode(mode: Int) {
        player.repeatMode = mode
    }

    fun setShuffleModeEnable(enable: Boolean) {
        player.shuffleModeEnabled = enable
    }

    /******************************* controls ***********************/
    fun next() {
        player.next()
    }

    fun previous() {
        player.previous()
    }

    fun play(intent: Intent?) {
        prepare(intent)
        seekToWindow(intent)
        player.playWhenReady = true
    }

    fun remove(index: Int) {
        mediaSource?.removeMediaSource(index)
    }

    /**
     * prepare data to play
     */
    fun prepare(intent: Intent?) {
        if (intent != null) {
            @Suppress("UNCHECKED_CAST")
            val medias = intent.getSerializableExtra("medias") as ArrayList<MediaInfo>?
            prepare(medias)
        }
    }

    fun prepare(medias: List<MediaInfo>?) {
        if (medias != null) {
            mediaSource = medias.toMediaSource(dataSourceFactory, mediaCompleteListener)
            player.prepare(mediaSource)
        }
    }

    fun seekToWindow(intent: Intent?) {
        if (intent != null)
            seekToWindow(intent.getIntExtra("position", 0))
    }

    fun seekToWindow(position: Int) {
        player.seekToDefaultPosition(position)
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun playOrPause() {
        if (player.playWhenReady)
            pause()
        else
            play(null)
    }

    fun stop() {
        player.stop()
    }

    /**
     * seek in range 0-1000
     */
    fun seekTo(percent: Int) {
        var pc = percent
        if (percent > 1000)
            pc = 1000
        else if (percent < 0)
            pc = 0
        player.seekTo(player.duration * pc / 1000)
    }

    /**
     * track progress
     *
     */
    fun track(progressChanged: ProgressChanged) {
        if (tracker != null)
            throw RuntimeException("last tracker is not released")

        tracker = ProgressTracker(player, progressChanged)
        tracker?.track()
    }

    /**
     * pause running tracker
     *
     * if u want to release tracker reference, use [unTrack] instead
     */
    fun pauseTracker() {
        tracker!!.release()
    }

    /**
     * resume stopped tracker
     */
    fun resumeTracker() {
        tracker!!.track()
    }

    fun unTrack() {
        tracker?.release()
        tracker = null
    }

    fun isPlaying(): Boolean {
        return player.playWhenReady && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
    }

    /**
     * get current [MediaInfo] in player
     */
    fun getCurrentMedia(): MediaInfo? {
        return player.currentTag as MediaInfo
    }

    fun playlist(): List<MediaInfo>? {
        return mediaSource?.getMediaInfos()
    }

    inner class ServiceBinder : Binder() {
        fun getService(): MediaService = this@MediaService
    }
}