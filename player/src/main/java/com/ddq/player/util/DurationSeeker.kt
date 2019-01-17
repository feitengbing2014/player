package com.ddq.player.util

import android.content.Intent
import com.ddq.player.MediaService

/**
 * created by dongdaqing 19-1-16 下午2:11
 */
internal class DurationSeeker(private val intent: Intent, private val service: MediaService) {

    fun start() {
        service.setTimer(intent)
    }
}