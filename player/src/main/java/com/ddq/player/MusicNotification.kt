package com.ddq.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.ddq.player.util.MediaPreference

const val NOW_PLAYING_CHANNEL: String = "com.ddq.player.media.NOW_PLAYING"

/*
* dongdaqing 2019/1/13 0013 20:58
*/
class MusicNotification(private val context: Context) {
    private lateinit var notification: Notification
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var navigationEnabled: Boolean = false

    fun getNotification() {
        if (shouldCreatePlayingChannel())
            createNotificationChannel()

        val remoteViews = RemoteViews(context.packageName, R.layout.player_notification)
        if (!this::notification.isInitialized) {
            val intent = Intent()
            val target = MediaPreference.getNotificationTargetPage(context)
            if (target != null)
                intent.component = ComponentName(context, target)
            notification = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)
                .setContent(remoteViews)
                .setSmallIcon(MediaPreference.getNotificationSmallIcon(context))
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .build()
        }
        notification.contentView = remoteViews
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
}