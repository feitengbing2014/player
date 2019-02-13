package com.ddq.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.NotificationTarget
import com.bumptech.glide.request.target.Target
import com.ddq.player.data.MediaInfo
import com.ddq.player.drawable.RoundedCornersTransformation
import com.google.android.exoplayer2.Player

/**
 * created by dongdaqing 19-1-16 下午4:47
 */

const val NOW_PLAYING_CHANNEL: String = "com.ddq.player.media.NOW_PLAYING"

internal class PlayerNotification(
    private val service: MediaService,
    private val player: Player
) {

    private val notificationId = 1
    private val notificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val playPauseIntent =
        PendingIntent.getBroadcast(
            service,
            0,
            Intent(Commands.SET_PLAYER_PLAY_OR_PAUSE),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

    private val closeIntent =
        PendingIntent.getBroadcast(service, 0, Intent(Commands.SET_PLAYER_DESTROY), PendingIntent.FLAG_CANCEL_CURRENT)
    private val nextIntent =
        PendingIntent.getBroadcast(service, 0, Intent(Commands.SET_PLAYER_NEXT), PendingIntent.FLAG_CANCEL_CURRENT)
    private val previousIntent =
        PendingIntent.getBroadcast(service, 0, Intent(Commands.SET_PLAYER_PREVIOUS), PendingIntent.FLAG_CANCEL_CURRENT)

    private val handler = Handler(Looper.getMainLooper())
    private val listener = PlayerListener()

    private val imageCorner = service.resources.getDimensionPixelSize(R.dimen.notification_image_corner)
    private val imageSize = service.resources.getDimensionPixelSize(R.dimen.notification_image_size)
    private val imageSizeSmall = service.resources.getDimensionPixelSize(R.dimen.notification_image_size_small)

    private var wasPlayWhenReady = false
    private var lastPlaybackState = -1
    private var isNotificationStarted = false
    var navigationEnable = true

    private var notification: Notification? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel =
            NotificationChannel(NOW_PLAYING_CHANNEL, "ddq.music.player", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isPlayingChannelExist() = notificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    private fun shouldCreatePlayingChannel() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isPlayingChannelExist()

    private fun updateNotification() {
        if (player.currentTag == null)
            return

        if (notification == null) {
            notification = createNotification()
            service.startForeground(notificationId, notification)
        }
    }

    fun startOrUpdateNotification() {
        updateNotification()
        if (!isNotificationStarted) {
            isNotificationStarted = true
            player.addListener(listener)
        }
    }

    fun stopNotification() {
        if (isNotificationStarted) {
            player.removeListener(listener)
            notificationManager.cancel(notificationId)
            isNotificationStarted = false
        }
    }

    /**
     * Creates the notification_expand given the current player state.
     *
     * @param player The player for which state to build a notification_expand.
     * @param largeIcon The large icon to be used.
     * @return The [Notification] which has been built.
     */
    private fun createNotification(): Notification {
        if (shouldCreatePlayingChannel())
            createNotificationChannel()

        val mediaInfo = player.currentTag as MediaInfo
        Log.d("MediaService", "update notification_expand:$mediaInfo")

        //big style view
        val bigViews = RemoteViews(service.packageName, R.layout.notification_expand)
        bigViews.setTextViewText(R.id.media_name, mediaInfo.mediaName)
        bigViews.setImageViewResource(
            R.id.media_play,
            if (service.isPlaying()) R.drawable.ics_player_nf_pause else R.drawable.ics_player_nf_play
        )
        bigViews.setOnClickPendingIntent(R.id.media_previous, previousIntent)
        bigViews.setOnClickPendingIntent(R.id.media_play, playPauseIntent)
        bigViews.setOnClickPendingIntent(R.id.media_next, nextIntent)
        bigViews.setOnClickPendingIntent(R.id.media_close, closeIntent)

        changeCover(false, mediaInfo.mediaCover)

        //normal style view
        val normalViews = RemoteViews(service.packageName, R.layout.notification_normal)
        normalViews.setTextViewText(R.id.media_name, mediaInfo.mediaName)
        normalViews.setImageViewResource(
            R.id.media_play,
            if (service.isPlaying()) R.drawable.ics_player_nf_pause else R.drawable.ics_player_nf_play
        )
        normalViews.setOnClickPendingIntent(R.id.media_play, playPauseIntent)
        normalViews.setOnClickPendingIntent(R.id.media_close, closeIntent)

        val intent = if (service.targetPage == null) null else Intent()
        intent?.setPackage(service.packageName)
        intent?.component = ComponentName(service, service.targetPage)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return NotificationCompat.Builder(service, NOW_PLAYING_CHANNEL)
            .setCustomBigContentView(bigViews)
            .setCustomContentView(normalViews)
            .setContentIntent(
                if (intent == null) null else PendingIntent.getActivity(
                    service,
                    0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
            .setSmallIcon(service.smallIcon)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
    }

    private fun changeCover(bigContentView: Boolean, string: String?) {
        handler.post {
            val target = NotificationTarget(
                service.applicationContext,
                R.id.media_cover,
                if (bigContentView) notification?.bigContentView else notification?.contentView,
                notification,
                notificationId
            )
            val size = if (bigContentView) imageSize else imageSizeSmall

            Glide.with(service)
                .asBitmap()
                .load(string)
                .apply(RequestOptions.centerCropTransform())
                .apply(RequestOptions.overrideOf(size, size))
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        bitmap: Bitmap,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        val transformation =
                            RoundedCornersTransformation(
                                imageCorner,
                                0,
                                RoundedCornersTransformation.CornerType.ALL
                            )
                        val out = transformation.transform(
                            service,
                            Glide.get(service).bitmapPool,
                            bitmap,
                            bitmap.width,
                            bitmap.height
                        )
                        target?.onResourceReady(out, null)
                        //加载大图
                        if (!bigContentView)
                            changeCover(true, string)
                        return true
                    }
                })
                .into(target)
        }
    }

    private inner class PlayerListener : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            //这里只是状态发生了改变，没必要全局更新
            if (wasPlayWhenReady != playWhenReady && playbackState != Player.STATE_IDLE || lastPlaybackState != playbackState) {
                if (notification != null) {
                    updatePlayState()
                    notificationManager.notify(notificationId, notification)
                } else {
                    startOrUpdateNotification()
                }
            }
            wasPlayWhenReady = playWhenReady
            lastPlaybackState = playbackState
        }

        override fun onPositionDiscontinuity(reason: Int) {
            if (notification == null) {
                startOrUpdateNotification()
            } else if (player.currentTag != null) {
                updatePlayState()
                val mediaInfo = player.currentTag as MediaInfo
                notification?.bigContentView?.setTextViewText(R.id.media_name, mediaInfo.mediaName)
                notification?.contentView?.setTextViewText(R.id.media_name, mediaInfo.mediaName)
                changeCover(false, mediaInfo.mediaCover)
            }
        }

        private fun updatePlayState() {
            notification?.bigContentView?.setImageViewResource(
                R.id.media_play,
                if (service.isPlaying()) R.drawable.ics_player_nf_pause else R.drawable.ics_player_nf_play
            )
            notification?.contentView?.setImageViewResource(
                R.id.media_play,
                if (service.isPlaying()) R.drawable.ics_player_nf_pause else R.drawable.ics_player_nf_play
            )
        }
    }
}