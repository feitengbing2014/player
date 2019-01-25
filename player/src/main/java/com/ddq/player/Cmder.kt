package com.ddq.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.google.android.exoplayer2.Player

/**
 * created by dongdaqing 19-1-11 下午2:16
 * 用来处理各项广播事件
 */
internal class Cmder(private val service: MediaService) : BroadcastReceiver() {

    fun register() {
        val filter = IntentFilter()
        with(filter) {
            addAction(Commands.SET_COUNTDOWN_TIMER)
            addAction(Commands.SET_REPEAT_MODE)

            addAction(Commands.SET_PLAYER_PAUSE)
            addAction(Commands.SET_PLAYER_PLAY)
            addAction(Commands.SET_PLAYER_STOP)
            addAction(Commands.SET_PLAYER_PLAY_OR_PAUSE)
            addAction(Commands.SET_PLAYER_PREVIOUS)
            addAction(Commands.SET_PLAYER_NEXT)
            addAction(Commands.SET_PLAYER_DESTROY)

            addAction(Commands.QUERY_TRACK_INFO)
            addAction(Commands.QUERY_PLAY_STATE)
            addAction(Commands.QUERY_REPEAT_MODE)
            addAction(Commands.QUERY_PLAYER_CURRENT_STATE)

            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        service.registerReceiver(this, filter)
    }

    fun unregister() = service.unregisterReceiver(this)

    override
    fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Commands.SET_COUNTDOWN_TIMER -> service.setTimer(intent)
            Commands.SET_REPEAT_MODE -> service.setRepeatMode(intent.getIntExtra("repeat_mode", Player.REPEAT_MODE_OFF))

            Commands.QUERY_TRACK_INFO -> service.queryTrackInfo()
            Commands.QUERY_PLAY_STATE -> service.queryPlayState()
            Commands.QUERY_REPEAT_MODE -> service.queryRepeatMode()
            Commands.QUERY_PLAYER_CURRENT_STATE -> service.queryPlayerCurrentState()

            Commands.SET_PLAYER_PLAY -> service.play(intent)
            Commands.SET_PLAYER_PAUSE -> service.pause()
            Commands.SET_PLAYER_STOP -> service.stop()
            Commands.SET_PLAYER_PLAY_OR_PAUSE -> service.playOrPause()
            Commands.SET_PLAYER_PREVIOUS -> service.previous()
            Commands.SET_PLAYER_NEXT -> service.next()
            Commands.SET_PLAYER_DESTROY -> service.destroy(intent)

            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> service.pause()
        }
    }
}