package com.ddq.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * created by dongdaqing 19-1-11 下午2:16
 * 用来处理各项广播事件
 */
class Cmder(musicService: MusicService) : BroadcastReceiver() {

    private val service: MusicService = musicService

    fun register() {
        val filter = IntentFilter()
        with(filter) {
            addAction(Commands.SET_PLAYER_TARGET)
            addAction(Commands.SET_COUNTDOWN_TIMER)

            addAction(Commands.QUERY_TIMELINE_POSITION)
            addAction(Commands.QUERY_TRACK_INFO)
            addAction(Commands.QUERY_PLAYSTATE)
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
            Commands.SET_PLAYER_TARGET -> service.setPlayerTarget(intent)
            Commands.QUERY_TIMELINE_POSITION -> queryTimeLinePosition()
            Commands.QUERY_TRACK_INFO -> queryTrackInfo()
            Commands.QUERY_PLAYSTATE -> queryPlayState()
            Commands.QUERY_REPEAT_MODE -> queryRepeatMode()
            Commands.QUERY_COUNTDOWN_TIMER -> queryCountdownTimer()
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