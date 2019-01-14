package com.ddq.musicplayer

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import com.ddq.player.Commands
import com.ddq.player.MediaService
import com.ddq.player.data.MediaInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity(), ServiceConnection {

    private var mService: MediaService? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MediaService.ServiceBinder
        mService = binder.getService()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mService = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, MediaService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(this)
    }

    fun play(view: View) {
        if (mService!!.isPlaying()) {
            mService!!.pause()
            return
        }

        val stream = assets.open("data.json")
        val reader = BufferedReader(InputStreamReader(stream))
        val builder = StringBuilder()
        var line: String? = null
        do {
            line = reader.readLine()
            if (line != null)
                builder.append(line.trim())
        } while (line != null)

        val str = builder.toString()
        val type = object : TypeToken<List<MediaInfo>>() {}.type
        val list = Gson().fromJson<List<MediaInfo>>(str, type)

        val intent = Intent(Commands.SET_PLAYER_PLAY)
        intent.putExtra("medias", ArrayList(list))
        mService!!.play(intent)
    }

    fun previous(view: View) {
        mService!!.previous()
    }

    fun next(view: View) {
        mService!!.next()
    }
}
