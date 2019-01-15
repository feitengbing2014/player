package com.ddq.musicplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.ddq.musicplayer.R.layout.activity_player
import com.ddq.player.*
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.ProgressChanged
import com.google.android.exoplayer2.Player
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_player.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), ServiceConnection, ProgressChanged {

    override fun onProgressChanged(played: Long, duration: Long) {
        media_played.text = played.toMediaTime()
        media_progress.progress = (played * 1000 / duration).toInt()
    }

    private var mService: MediaService? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MediaService.ServiceBinder
        mService = binder.getService()
        val stream = assets.open("data.json")
        val reader = BufferedReader(InputStreamReader(stream))
        val builder = StringBuilder()
        var line: String?
        do {
            line = reader.readLine()
            if (line != null)
                builder.append(line.trim())
        } while (line != null)

        val str = builder.toString()
        val type = object : TypeToken<List<MediaInfo>>() {}.type
        val list = Gson().fromJson<List<MediaInfo>>(str, type)
        mService?.prepare(list)
        mService?.track(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mService?.unTrack()
        mService = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_player)

        play.setOnClickListener {
            play(it)
        }

        playing_pre.setOnClickListener {
            previous(it)
        }

        playing_next.setOnClickListener {
            next(it)
        }

        playlist.setOnClickListener {
            val fragment = PlayListFragment()
            fragment.list = mService?.playlist()
            fragment.service = mService
            fragment.show(supportFragmentManager, "play_list")
        }

        media_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                mService?.pauseTracker()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mService?.seekTo(seekBar.progress)
                mService?.resumeTracker()
            }
        })

        EventListener(this).apply {
            count = object : Count {
                override fun onCounting(mills: Long) {
                    if (mills > 0) {
                        counting.visibility = View.VISIBLE
                    } else
                        counting.visibility = View.GONE
                    counting.text = mills.toMediaTime()
                }
            }

            trackChanged = object : TrackChanged {
                override fun onTrackChange(mediaInfo: MediaInfo, duration: Long) {
                    media_name.text = mediaInfo.mediaName
                    media_duration.text = duration.toMediaTime()
                    Glide.with(this@MainActivity).load(mediaInfo.mediaCover).into(cover)
                }
            }

            playModeChanged = object : PlayModeChanged {
                override fun onPlayModeChanged(mode: Int) {
                    when (mode) {
                        Player.REPEAT_MODE_ONE -> playing_mode.setImageResource(R.drawable.ics_repeat)
                        Player.REPEAT_MODE_ALL -> playing_mode.setImageResource(R.drawable.ics_loop)
                        Player.REPEAT_MODE_OFF -> playing_mode.setImageResource(R.drawable.ics_random)
                    }
                }
            }

            playStateChanged = object : PlayStateChanged {
                override fun onPlayStateChanged(playing: Boolean) {
                    if (playing)
                        play.setImageResource(R.drawable.ics_pause)
                    else
                        play.setImageResource(R.drawable.ics_play)
                }
            }

            loadingChanged = object : LoadingChanged {
                override fun onLoadingChanged(loading: Boolean) {
                    state.text = "loading:$loading"
                }
            }
        }
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

        mService!!.play(null)
    }

    fun previous(view: View) {
        mService!!.previous()
    }

    fun next(view: View) {
        mService!!.next()
    }
}
