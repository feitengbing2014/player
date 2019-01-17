package com.ddq.player.util

import android.content.Intent
import com.ddq.player.Commands
import com.ddq.player.MediaService
import java.util.*

/**
 * created by dongdaqing 19-1-11 下午3:26
 */
internal class MediaTimer(
    private val service: MediaService,
    private val pending: Queue<Intent>?,
    val type: Int,
    millsInFuture: Long
) : Timer(millsInFuture, 200) {

    companion object {
        const val TYPE_NORMAL = 1
        const val TYPE_CURRENT = 2
    }

    fun isCountForCurrent(): Boolean = type == TYPE_CURRENT

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