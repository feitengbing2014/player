package com.ddq.player.util

import android.content.Intent
import com.ddq.player.Commands
import com.ddq.player.MediaService
import java.util.*

/**
 * created by dongdaqing 19-1-11 下午3:26
 */
class MusicTimer(private val service: MediaService, millsInFuture: Long, private val pending: Queue<Intent>?) :
    Timer(millsInFuture, 500) {

    override
    fun onTick(millisUntilFinished: Long) {
        if (pending != null && pending.size > 0) {
            val fireTime = pending.peek().getIntExtra("fire", 0).toLong()

            if (fireTime >= millisUntilFinished) {
                sendBroadcast(pending.poll())
            }
        }

        sendCountBroadcast(millisUntilFinished)
    }

    override fun onFinish() {
        sendCountBroadcast(0)
    }

    private fun sendCountBroadcast(left: Long) {
        val intent = Intent(Commands.ACTION_COUNTING)
        intent.putExtra("seconds_left", left)
        service.sendBroadcast(intent)
    }

    private fun sendBroadcast(intent: Intent) = service.sendBroadcast(intent)
}