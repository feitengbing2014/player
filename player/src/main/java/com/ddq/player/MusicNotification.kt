package com.ddq.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.view.View
import android.widget.RemoteViews
import com.ddq.player.util.MediaPreference
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline

const val NOW_PLAYING_CHANNEL: String = "com.ddq.player.media.NOW_PLAYING"

/*
* dongdaqing 2019/1/13 0013 20:58
*/
class MusicNotification(private val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var wasPlayWhenReady = false
    private var lastPlaybackState = -1

    var player: ExoPlayer? = null
    var navigationEnabled: Boolean = false

    private fun startOrUpdateNotification(): Notification? {
        if (shouldCreatePlayingChannel())
            createNotificationChannel()

        val remoteViews = RemoteViews(context.packageName, R.layout.player_notification).apply {
            setViewVisibility(R.id.next, (if (navigationEnabled) View.VISIBLE else View.GONE))
            setOnClickPendingIntent(R.id.pause, getPauseIntent())
            setOnClickPendingIntent(R.id.stop, getCloseIntent())
            setOnClickPendingIntent(R.id.next, getNextIntent())
        }

        return NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)
            .setContent(remoteViews)
            .setSmallIcon(MediaPreference.getNotificationSmallIcon(context))
            .setContentIntent(getContentIntent())
            .setOngoing(true)
            .build()
    }

    private fun getPauseIntent(): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(Commands.SET_PLAYER_PLAY_OR_PAUSE),
            PendingIntent.FLAG_ONE_SHOT
        )
    }

    private fun getCloseIntent(): PendingIntent {
        return PendingIntent.getBroadcast(context, 0, Intent(Commands.SET_PLAYER_STOP), PendingIntent.FLAG_ONE_SHOT)
    }

    private fun getNextIntent(): PendingIntent {
        return PendingIntent.getBroadcast(context, 0, Intent(Commands.SET_PLAYER_NEXT), PendingIntent.FLAG_ONE_SHOT)
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent()
        val target = MediaPreference.getNotificationTargetPage(context)
        if (target != null)
            intent.component = ComponentName(context, target)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

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

    private inner class PlayerListener : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (wasPlayWhenReady != playWhenReady && playbackState != Player.STATE_IDLE || lastPlaybackState != playbackState) {
                startOrUpdateNotification()
            }
            wasPlayWhenReady = playWhenReady
            lastPlaybackState = playbackState
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            if (player == null || player!!.playbackState == Player.STATE_IDLE) {
                return
            }
            startOrUpdateNotification()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            if (player == null || player!!.playbackState == Player.STATE_IDLE) {
                return
            }
            startOrUpdateNotification()
        }

        override fun onPositionDiscontinuity(reason: Int) {
            startOrUpdateNotification()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            if (player == null || player!!.playbackState == Player.STATE_IDLE) {
                return
            }
            startOrUpdateNotification()
        }
    }
}