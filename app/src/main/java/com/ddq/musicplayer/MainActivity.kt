package com.ddq.musicplayer

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.ddq.musicplayer.R.layout.activity_player
import com.ddq.player.*
import com.ddq.player.data.CountTime
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.Preference
import com.ddq.player.util.ProgressChanged
import com.google.android.exoplayer2.Player
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_player.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), ProgressChanged {

    override fun onProgressChanged(played: Long, duration: Long) {
        media_played.text = played.toMediaTime()
        media_progress.progress = (played * 1000 / duration).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_player)

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
        MediaServiceManager.prepare(list)
        MediaServiceManager.track(this, this)

        with(Preference(this)) {
            setSmallIcon(R.drawable.ic_logo_transparent)
            setPlayMode(arrayOf(Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_ONE))
        }

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
            fragment.list = MediaServiceManager.playlist()
            fragment.show(supportFragmentManager, "play_list")
        }

        playing_mode.setOnClickListener {
            MediaServiceManager.nextPlayMode()
        }

        clock.setOnClickListener {
            val fragment = TimerFragment()
            fragment.callback = object : Callback {
                override fun onSelect(mode: Int, mills: Long) {
                    val intent = Intent()
                    intent.putExtra("data", CountTime(mode, mills))
                    MediaServiceManager.setTimer(intent)
                }
            }
            fragment.show(supportFragmentManager, "clock")
        }

        media_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                MediaServiceManager.pauseTracker()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                MediaServiceManager.seekTo(seekBar.progress)
                MediaServiceManager.resumeTracker()
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
                override fun onTrackChange(mediaInfo: MediaInfo, duration: Long, position: Long) {
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

    fun play(view: View) {
        MediaServiceManager.playOrPause()
    }

    fun previous(view: View) {
        MediaServiceManager.previous()
    }

    fun next(view: View) {
        MediaServiceManager.next()
    }
}
