package com.ddq.player

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ddq.player.data.*
import com.ddq.player.util.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

/**
 * created by dongdaqing 19-1-11 下午1:46
 *
 * this service must start by calling Context#startService(packName,intent)
 *
 */
internal class MediaService : Service(), Controls {

    override fun onBind(intent: Intent?): IBinder? {
        return ServiceBinder(this)
    }

    private val preference: Preference by lazy {
        Preference(this)
    }

    val playMode: Array<Int> by lazy {
        preference.getPlayMode()
    }

    val targetPage: String? by lazy {
        preference.getTargetPage()
    }

    val smallIcon: Int by lazy {
        preference.getSmallIcon()
    }

    private var timer: MediaTimer? = null
    private var mediaSource: ConcatenatingMediaSource? = null
    private var tracker: ProgressTracker? = null
    private var durationSeeker: DurationSeeker? = null

    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private val cmder = Cmder(this)
    private val mediaCompleteListener = Runnable {
        /**
         * timer is counting for current media
         */
        Log.d("MediaService", "mediaComplete:${timer?.type}")
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
                },duration ${player.duration}"
            )

            //start timer
            if (player.duration > 0) {
                durationSeeker?.start()
                durationSeeker = null
            }
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            Log.d("MediaService", "onTracksChanged, duration ${player.duration}")
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
        }
    }

    private val playerNotification: PlayerNotification by lazy {
        PlayerNotification(this, player)
    }

    override fun onCreate() {
        super.onCreate()
        //注册事件处理器
        cmder.register()
        playerNotification.startOrUpdateNotification()
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, packageName))
        sendBroadcast(Intent(Commands.ACTION_SERVICE_CREATED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Commands.SET_PLAYER_PLAY == intent?.action) {
            play(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("MediaService", "onDestroy")
        val intent = Intent(Commands.ACTION_SERVICE_DESTROYED)
        sendBroadcast(intent)
        cmder.unregister()
        stop()
        player.release()
        super.onDestroy()
    }

    override fun setTimer(intent: Intent) {
        val type = intent.getIntExtra("type", MediaTimer.TYPE_NORMAL)
        if (type == MediaTimer.TYPE_CURRENT && player.duration <= 0) {
            durationSeeker = DurationSeeker(intent, this)
            return
        }

        timer?.cancel()

        @Suppress("UNCHECKED_CAST")
        val actions = intent.getSerializableExtra("actions") as List<TimerAction>?

        timer = MediaTimer(
            this,
            actions?.toPendingActions(),
            type,
            if (type == MediaTimer.TYPE_CURRENT)
                (player.duration - player.currentPosition)
            else
                intent.getLongExtra("mills", 0)
        )

        if (isPlaying()) {
            timer!!.start()
        } else {
            timer!!.pause()
        }
    }

    fun setRepeatMode(mode: Int) {
        player.repeatMode = mode
    }

    fun setShuffleModeEnable(enable: Boolean) {
        player.shuffleModeEnabled = enable
    }

    /******************************* controls ***********************/
    override fun next() {
        player.next()
    }

    override fun previous() {
        player.previous()
    }

    /**
     * start playing
     *
     */
    fun play(intent: Intent?) {
        prepare(intent)
        seekToWindow(intent)
        player.playWhenReady = true
    }

    /**
     * remove item from playlist
     */
    override fun remove(index: Int) {
        mediaSource?.removeMediaSource(index)
    }

    /**
     * prepare data to play
     */
    fun prepare(intent: Intent?) {
        if (intent != null) {
            @Suppress("UNCHECKED_CAST")
            val medias = intent.getSerializableExtra("medias") as ArrayList<MediaInfo>?
            prepare(medias, mediaCompleteListener)
        }
    }

    override fun prepare(medias: List<MediaInfo>?) {
        prepare(medias, mediaCompleteListener)
    }

    fun prepare(medias: List<MediaInfo>?, runnable: Runnable) {
        if (medias != null) {
            mediaSource = medias.toMediaSource(dataSourceFactory, runnable)
            player.prepare(mediaSource)
        }
    }

    fun seekToWindow(intent: Intent?) {
        if (intent != null)
            seekToWindow(intent.getIntExtra("position", 0))
    }

    override fun seekToWindow(position: Int) {
        player.seekToDefaultPosition(position)
    }

    fun pause() {
        player.playWhenReady = false
    }

    override fun playOrPause() {
        if (isPlaying())
            pause()
        else
            play(null)
    }

    fun stop() {
        playerNotification.stopNotification()
        timer?.cancel()
        player.stop()
    }

    fun destroy() {
        stopSelf()
    }

    /**
     * switch play mode
     */
    override fun nextPlayMode() {
        var index = 0
        for (i in 0..(playMode.size - 1)) {
            if (playMode[i] == player.repeatMode) {
                index = i + 1
                break
            }
        }

        player.repeatMode = playMode[index % playMode.size]
    }

    /**
     * seek in range 0-1000
     */
    override fun seekTo(percent: Int) {
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
    override fun track(progressChanged: ProgressChanged) {
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
    override fun pauseTracker() {
        tracker!!.release()
    }

    /**
     * resume stopped tracker
     */
    override fun resumeTracker() {
        tracker!!.track()
    }

    override fun unTrack() {
        tracker?.release()
        tracker = null
    }

    override fun isPlaying(): Boolean {
        return player.playWhenReady && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
    }

    /**
     * get current [MediaInfo] in player
     */
    override fun getCurrentMedia(): MediaInfo? {
        return player.currentTag as MediaInfo
    }

    override fun playlist(): List<MediaInfo>? {
        return mediaSource?.getMediaInfos()
    }

}