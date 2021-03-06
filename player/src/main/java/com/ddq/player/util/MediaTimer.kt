package com.ddq.player.util

import android.content.Intent
import android.util.Log
import com.ddq.player.Commands
import com.ddq.player.MediaService
import com.ddq.player.data.CountTime.Companion.TYPE_CURRENT
import java.util.*

/**
 * created by dongdaqing 19-1-11 下午3:26
 */
internal class MediaTimer(
    private val service: MediaService,
    private val pending: Queue<Intent>?,
    private val type: Int,
    millsInFuture: Long
) : Timer(millsInFuture, 200) {

    fun isCountForCurrent(): Boolean = type == TYPE_CURRENT

    override
    fun onTick(millisUntilFinished: Long) {
        if (pending != null && pending.size > 0) {
            val fireTime = pending.peek().getLongExtra("fire", 0)

            if (fireTime >= millisUntilFinished) {
                sendBroadcast(pending.poll())
            }
        }

        sendCountBroadcast(millisUntilFinished)
    }

    override fun pause() {
        super.pause()
        sendCountBroadcast(mMillisInFuture)
    }

    override fun cancel() {
        super.cancel()
        sendBroadcast(Intent(Commands.ACTION_COUNT_CANCEL))
    }

    override fun onFinish() {
        sendCountBroadcast(0)
        service.pause()
        service.releaseTimer()
    }

    private fun sendCountBroadcast(left: Long) {
        val intent = Intent(Commands.ACTION_COUNTING)
        intent.putExtra("seconds_left", left)
        service.sendBroadcast(intent)
    }

    private fun sendBroadcast(intent: Intent) = service.sendBroadcast(intent)
}