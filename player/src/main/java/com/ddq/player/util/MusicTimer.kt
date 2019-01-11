package com.ddq.player.util

import android.content.Intent
import com.ddq.player.Commands
import com.ddq.player.MusicService
import java.util.*

/**
 * created by dongdaqing 19-1-11 下午3:26
 */
class MusicTimer(service: MusicService, millsInFuture: Long, pending: Queue<Intent>?) : Timer(millsInFuture, 500) {

    private val mService = service
    private val mPendingAction = pending

    override
    fun onTick(millisUntilFinished: Long) {
        if (mPendingAction != null && mPendingAction.size > 0) {
            val fireTime = mPendingAction.peek().getIntExtra("fire", 0).toLong()

            if (fireTime >= millisUntilFinished) {
                sendBroadcast(mPendingAction.poll())
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
        mService.sendBroadcast(intent)
    }

    private fun sendBroadcast(intent: Intent) = mService.sendBroadcast(intent)
}