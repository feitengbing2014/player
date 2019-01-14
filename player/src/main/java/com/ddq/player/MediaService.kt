package com.ddq.player

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.ddq.player.data.MediaInfo
import com.ddq.player.data.TimerAction
import com.ddq.player.data.toMediaSource
import com.ddq.player.data.toPendingActions
import com.ddq.player.util.MediaPreference
import com.ddq.player.util.MusicTimer
import com.ddq.player.util.Timer
import com.google.android.exoplayer2.*
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

    private var timer: Timer? = null
    private var mediaSource: ConcatenatingMediaSource? = null

    private lateinit var musicNotification: MusicNotification
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private val cmder = Cmder(this)
    private val listener = object : Player.EventListener {
        /******************************* callbacks ***********************/

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            Log.d("MediaService", "onTimelineChanged")
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            Log.d("MediaService", "onTracksChanged")

        }

        override fun onLoadingChanged(isLoading: Boolean) {
            Log.d("MediaService", "onLoadingChanged:$isLoading")
            val intent = Intent(Commands.ACTION_LOADING_CHANGED)
            intent.putExtra("loading", isLoading)
            sendBroadcast(intent)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.d("MediaService", "onPlayerStateChanged:$playWhenReady,playbackState:$playbackState")
            if (playWhenReady) {
                timer?.resume()
            } else {
                timer?.pause()
            }

            val intent = Intent(Commands.ACTION_PLAY_STATE_CHANGED)
            intent.putExtra("play", playWhenReady)
            sendBroadcast(intent)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.d("MediaService", "onRepeatModeChanged:$repeatMode")
            val intent = Intent(Commands.ACTION_REPEAT_MODE_CHANGED)
            intent.putExtra("repeat_mode", repeatMode)
            sendBroadcast(intent)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Log.d("MediaService", "onShuffleModeEnabledChanged:$shuffleModeEnabled")

        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Log.e("MediaService", "onPlayerError:${error?.printStackTrace()}")

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

        PlayerNotificationManager.createWithNotificationChannel(this, "com.ddq.player.media.NOW_PLAYING", R.string.app_name, 1, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Commands.SET_PLAYER_PLAY == intent?.action) {
            play(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cmder.unregister()
        player.release()
        super.onDestroy()
    }

    fun setTimer(intent: Intent) {
        timer?.cancel()

        @Suppress("UNCHECKED_CAST")
        val actions = intent.getSerializableExtra("actions") as List<TimerAction>?

        timer = MusicTimer(this, intent.getLongExtra("mills", 0), actions?.toPendingActions())

        if (player.playWhenReady) {
            timer?.start()
        } else {
            timer?.pause()
        }
    }

    fun setRepeatMode(intent: Intent) {
        player.repeatMode = intent.getIntExtra("repeat_mode", Player.REPEAT_MODE_OFF)
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
        player.playWhenReady = true
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

    fun isPlaying(): Boolean {
        return player.playWhenReady
    }

    fun currentMedia(): MediaInfo? {
        return player.currentTag as MediaInfo
    }

    /**
     * prepare data to play
     */
    private fun prepare(intent: Intent?) {
        if (intent != null) {
            @Suppress("UNCHECKED_CAST")
            val medias = intent.getSerializableExtra("medias") as ArrayList<MediaInfo>?
            if (medias != null) {
                mediaSource = medias.toMediaSource(dataSourceFactory)
                player.prepare(mediaSource)
            }
        }
    }

    inner class ServiceBinder : Binder() {
        fun getService(): MediaService = this@MediaService
    }
}