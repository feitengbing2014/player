package com.ddq.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * created by dongdaqing 19-1-11 下午2:16
 * 用来处理各项广播事件
 */
class Cmder(private val service: MediaService) : BroadcastReceiver() {

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

            addAction(Commands.QUERY_TIMELINE_POSITION)
            addAction(Commands.QUERY_TRACK_INFO)
            addAction(Commands.QUERY_PLAY_STATE)
            addAction(Commands.QUERY_REPEAT_MODE)
            addAction(Commands.QUERY_COUNTDOWN_TIMER)
        }
        service.registerReceiver(this, filter)
    }

    fun unregister() = service.unregisterReceiver(this)

    override
    fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Commands.SET_COUNTDOWN_TIMER -> service.setTimer(intent)
            Commands.SET_REPEAT_MODE -> service.setRepeatMode(intent)

            Commands.QUERY_TIMELINE_POSITION -> queryTimeLinePosition()
            Commands.QUERY_TRACK_INFO -> queryTrackInfo()
            Commands.QUERY_PLAY_STATE -> queryPlayState()
            Commands.QUERY_REPEAT_MODE -> queryRepeatMode()
            Commands.QUERY_COUNTDOWN_TIMER -> queryCountdownTimer()

            Commands.SET_PLAYER_PLAY -> service.play(intent)
            Commands.SET_PLAYER_PAUSE -> service.pause()
            Commands.SET_PLAYER_STOP -> service.stop()
            Commands.SET_PLAYER_PLAY_OR_PAUSE -> service.playOrPause()
            Commands.SET_PLAYER_PREVIOUS -> service.previous()
            Commands.SET_PLAYER_NEXT -> service.next()
        }
    }

    private fun queryTimeLinePosition() {

    }

    private fun queryTrackInfo() {

    }

    private fun queryPlayState() {

    }

    private fun queryRepeatMode() {

    }

    private fun queryCountdownTimer() {

    }
}