package com.ddq.musicplayer

import android.app.Application
import com.ddq.player.MediaServiceManager

/**
 * created by dongdaqing 19-1-21 下午3:05
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MediaServiceManager.initialize(this)
    }
}