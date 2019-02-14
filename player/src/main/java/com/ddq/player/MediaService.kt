package com.ddq.player

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
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
import kotlin.math.max

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

    val lockScreenTarget: String? by lazy {
        preference.getLockScreenTarget()
    }

    private var timer: MediaTimer? = null
    private var mediaSource: ConcatenatingMediaSource? = null
    private var tracker: ProgressTracker? = null
    private var durationSeeker: DurationSeeker? = null
    private var startId: Int = -1
    private var buffering: Boolean = false
    private var clearQueue: Boolean = false

    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private val handler = Handler()
    private val cmder = Cmder(this)
    private val bufferDispatcher = Runnable {
        Log.d("MediaService", "start dispatch buffering")
        val intent = Intent(Commands.ACTION_BUFFERING)
        buffering = true
        intent.putExtra("buffering", buffering)
        intent.putExtra("position", player.bufferedPosition)
        intent.putExtra("percent", player.bufferedPercentage)
        sendBroadcast(intent)
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

                val intent = Intent(Commands.ACTION_TIMELINE_CHANGED)
                intent.putExtra("duration", player.duration)
                sendBroadcast(intent)
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

            if (playbackState == Player.STATE_BUFFERING) {
                //如果缓冲持续400ms以上，就发送缓冲通知
                handler.postDelayed(bufferDispatcher, 400)
            } else {
                if (buffering) {
                    buffering = false
                    //发送缓冲结束广播
                    sendBroadcast(Intent(Commands.ACTION_BUFFERING))
                }
                //（网络出错，缓冲成功）
                handler.removeCallbacks(bufferDispatcher)
            }

            if (playWhenReady && playbackState == Player.STATE_READY) {
                //只有当数据能够播放的时候才开始倒计时，在buffer和ready之间会有一段缓冲时间，
                //不能在缓冲的时候进行倒计时操作
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

        @SuppressLint("SwitchIntDef")
        override fun onPositionDiscontinuity(reason: Int) {
            Log.d("MediaService", "onPositionDiscontinuity:$reason")

            if (timer != null) {
                when (reason) {
                    DISCONTINUITY_REASON_PERIOD_TRANSITION -> {
                        //曲目切换（当前文件播放完，播放器内部自动切换）
                        //如果是播放完当前就停止的计时器，那么就要暂停
                        if (timer!!.isCountForCurrent()) {
                            pause()
                            cancelTimer()
                        }
                    }
                    DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> {
                        //这是手动切换上一曲，下一曲的回调
                        if (timer!!.isCountForCurrent()) {
                            //取消倒计时
                            cancelTimer()
                        } else {
                            //正常倒计时，暂停倒计时，待下一曲开始播放之后在开始
                            timer!!.pause()
                        }
                    }
                }
            }

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
        val repeatMode = preference.getRepeatMode()
        player.repeatMode = if (repeatMode == -1) playMode[0] else repeatMode
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.startId = startId
        sendBroadcast(Intent(Commands.ACTION_SERVICE_STARTED))
        if (Commands.SET_PLAYER_PLAY == intent?.action) {
            play(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("MediaService", "onDestroy")
        val intent = Intent(Commands.ACTION_SERVICE_DESTROYED)
        if (!clearQueue) {
            val playlist = playlist()
            if (playlist != null) {
                intent.putExtra("medias", playlist)
                intent.putExtra("data", getCurrentMedia())
                intent.putExtra("contentPosition", position())
            }
        }
        sendBroadcast(intent)

        cmder.unregister()
        stop()
        player.release()
        super.onDestroy()
    }

    override fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    override fun setTimer(intent: Intent) {
        val countTime = intent.getSerializableExtra("data") as CountTime
        if (countTime.type == CountTime.TYPE_CURRENT && player.duration <= 0) {
            durationSeeker = DurationSeeker(intent, this)
            return
        }

        timer?.cancel()

        @Suppress("UNCHECKED_CAST")
        val actions = intent.getSerializableExtra("actions") as List<TimerAction>?

        timer = MediaTimer(
            this,
            actions?.toPendingActions(),
            countTime.type,
            if (countTime.type == CountTime.TYPE_CURRENT)
                (player.duration - player.currentPosition)
            else
                countTime.millSeconds
        )

        if (isPlaying()) {
            timer!!.start()
        } else {
            timer!!.pause()
        }
    }

    fun setRepeatMode(mode: Int) {
        player.repeatMode = mode
        preference.setRepeatMode(mode)
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

    override fun add(media: MediaInfo, index: Int) {
        if (mediaSource == null) {
            prepare(arrayListOf(media))
            return
        }

        mediaSource?.addMediaSource(index, media.toMediaSource(dataSourceFactory)) {
            val intent = Intent(Commands.ACTION_ITEM_ADDED)
            intent.putExtra("media", media)
            sendBroadcast(intent)
        }
    }

    /**
     * remove item from playlist
     */
    override fun remove(index: Int) {
        val media = mediaSource?.getMediaSource(index)?.tag as MediaInfo
        mediaSource?.removeMediaSource(index) {
            val intent = Intent(Commands.ACTION_ITEM_REMOVED)
            intent.putExtra("media", media)
            sendBroadcast(intent)
        }
    }

    override fun remove(mediaInfo: MediaInfo) {
        mediaSource?.removeMediaSource(mediaInfo, Runnable {
            val intent = Intent(Commands.ACTION_ITEM_REMOVED)
            intent.putExtra("media", mediaInfo)
            sendBroadcast(intent)
        })
    }

    /**
     * prepare data to play
     */
    override fun prepare(intent: Intent?) {
        if (intent != null) {
            @Suppress("UNCHECKED_CAST")
            val medias = intent.getParcelableArrayListExtra<MediaInfo>("medias")
            val position = intent.getIntExtra("position", 0)
            val media = intent.getParcelableExtra<MediaInfo>("data")
            prepare(medias, position, media, null)
        }
    }

    override fun prepare(medias: List<MediaInfo>?) {
        prepare(medias, 0, null, null)
    }

    private fun prepare(medias: List<MediaInfo>?, position: Int, media: MediaInfo?, runnable: Runnable?) {
        if (medias != null) {
            mediaSource = medias.toMediaSource(dataSourceFactory, runnable)
            player.prepare(mediaSource)

            var pos = position
            if (media != null)
                pos = max(pos, medias.findItemIndex(media))

            seekToWindow(pos)

            sendBroadcast(Intent(Commands.ACTION_QUEUE_CHANGED))
        }
    }

    fun seekToWindow(intent: Intent?) {
        if (intent != null)
            seekToWindow(intent.getIntExtra("position", 0))
    }

    override fun seekToWindow(position: Int) {
        player.seekToDefaultPosition(position)
    }

    override fun seekToWindow(position: Int, ms: Long) {
        player.seekTo(position, ms)
    }

    fun pause() {
        player.playWhenReady = false
    }

    override fun playOrPause() {

        /**
         * 由于数据错误或者网络错误导致播放器进入停止状态，这时候必须重新prepare
         */
        if (player.playbackState == STATE_IDLE) {
            prepare(mediaSource?.getMediaInfos(), 0, player.currentTag as MediaInfo?, null)
            play(null)
            return
        }

        if (isPlaying())
            pause()
        else
            play(null)
    }

    fun stop() {
        cancelTimer()
        playerNotification.stopNotification()
        tracker?.release()
        player.stop()
    }

    fun destroy(intent: Intent) {
        /**
         * 这里不做关闭操作，真正的结束操作在[MediaServiceManager]里面
         */
        clearQueue = intent.getBooleanExtra("clear", false)
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

        val ms = player.duration * pc / 1000

        if (timer != null && timer!!.isCountForCurrent()) {
            //如果是仅限当前曲目的倒计时操作，在进行seek操作时，要将倒计时进行修改
            timer!!.pause()
            timer!!.mMillisInFuture = player.duration - ms

            if (isPlaying())
                timer!!.resume()
            else {
                timer!!.onTick(timer!!.mMillisInFuture)
            }
        }

        player.seekTo(ms)
    }

    /**
     * track progress
     * [unTrack] manually when tracker is no long useful
     */
    override fun track(progressChanged: ProgressChanged) {
        unTrack()
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

    /***************************  query **********************************/
    fun queryTrackInfo() {
        val intent = Intent(Commands.ACTION_TRACK_CHANGED)
        intent.putExtra("media", getCurrentMedia())
        intent.putExtra("position", player.contentPosition)
        intent.putExtra("duration", player.duration)
        sendBroadcast(intent)
    }

    fun queryPlayState() {
        val intent = Intent(Commands.ACTION_PLAY_STATE_CHANGED)
        intent.putExtra("play", isPlaying())
        sendBroadcast(intent)
    }

    fun queryRepeatMode() {
        val intent = Intent(Commands.ACTION_REPEAT_MODE_CHANGED)
        intent.putExtra("mode", player.repeatMode)
        sendBroadcast(intent)
    }

    fun queryPlayerCurrentState() {
        val intent = Intent(Commands.ACTION_PLAYER_CURRENT_STATE)
        intent.putExtra("media", getCurrentMedia())
        intent.putExtra("position", player.contentPosition)
        intent.putExtra("duration", player.duration)
        intent.putExtra("play", isPlaying())
        intent.putExtra("playMode", player.repeatMode)

        if (timer != null)
            intent.putExtra("timer", timer!!.mMillisInFuture)
        sendBroadcast(intent)
    }

    override fun isPlaying(): Boolean {
        return player.playWhenReady && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
    }

    /**
     * get current [MediaInfo] in player
     */
    override fun getCurrentMedia(): MediaInfo? {
        if (player.currentTag == null)
            return null
        return player.currentTag as MediaInfo
    }

    override fun playlist(): ArrayList<MediaInfo>? {
        return mediaSource?.getMediaInfos()
    }

    override fun isNavigationEnabled(): Boolean {
        return playerNotification.navigationEnable
    }

    override fun setNavigationEnable(enable: Boolean) {
        playerNotification.navigationEnable = enable
    }

    override fun position(): Long {
        return player.currentPosition
    }

    fun lock() {
        if (lockScreenTarget != null && isPlaying()) {
            val intent = Intent()
            intent.setPackage(packageName)
            intent.component = ComponentName(this, lockScreenTarget!!)
            startActivity(intent)
        }
    }
}