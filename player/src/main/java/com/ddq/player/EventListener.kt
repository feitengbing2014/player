package com.ddq.player

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import com.ddq.player.data.MediaInfo
import com.google.android.exoplayer2.Player

/**
 * created by dongdaqing 19-1-15 上午11:33
 */
class EventListener(private val activity: FragmentActivity?) : BroadcastReceiver() {

    private val filter: IntentFilter = IntentFilter()

    var count: Count? = null
    var trackChanged: TrackChanged? = null
    var playStateChanged: PlayStateChanged? = null
    var loadingChanged: LoadingChanged? = null
    var playModeChanged: PlayModeChanged? = null

    init {
        with(filter) {
            addAction(Commands.ACTION_TIMELINE_CHANGED)
            addAction(Commands.ACTION_TRACK_CHANGED)
            addAction(Commands.ACTION_LOADING_CHANGED)
            addAction(Commands.ACTION_PLAY_STATE_CHANGED)
            addAction(Commands.ACTION_REPEAT_MODE_CHANGED)
            addAction(Commands.ACTION_SHUFFLE_MODE_CHANGED)
            addAction(Commands.ACTION_POSITION_DISCONTINUITY_CHANGED)
            addAction(Commands.ACTION_COUNTING)
        }

        activity?.lifecycle!!.addObserver(GenericLifecycleObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> activity.registerReceiver(this, filter)
                Lifecycle.Event.ON_STOP -> activity.unregisterReceiver(this)
            }
        })
    }

    constructor(fragment: Fragment) : this(null) {
        fragment.lifecycle.addObserver(GenericLifecycleObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> fragment.activity!!.registerReceiver(this, filter)
                Lifecycle.Event.ON_STOP -> fragment.activity!!.unregisterReceiver(this)
            }
        })
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Commands.ACTION_TIMELINE_CHANGED -> {
            }
            Commands.ACTION_TRACK_CHANGED -> trackChanged?.onTrackChange(
                intent.getSerializableExtra("media") as MediaInfo,
                intent.getLongExtra("duration", 0)
            )
            Commands.ACTION_LOADING_CHANGED -> loadingChanged?.onLoadingChanged(
                intent.getBooleanExtra(
                    "loading",
                    false
                )
            )
            Commands.ACTION_PLAY_STATE_CHANGED -> playStateChanged?.onPlayStateChanged(
                intent.getBooleanExtra(
                    "play",
                    false
                )
            )
            Commands.ACTION_REPEAT_MODE_CHANGED -> playModeChanged?.onPlayModeChanged(
                intent.getIntExtra(
                    "mode",
                    Player.REPEAT_MODE_OFF
                )
            )
            Commands.ACTION_SHUFFLE_MODE_CHANGED -> {
            }
            Commands.ACTION_POSITION_DISCONTINUITY_CHANGED -> {
            }
            Commands.ACTION_COUNTING -> count?.onCounting(intent.getLongExtra("seconds_left", 0))
        }
    }
}

interface Count {
    fun onCounting(mills: Long)
}

interface TrackChanged {
    /**
     * use [duration] instead of [MediaInfo.duration]
     */
    fun onTrackChange(mediaInfo: MediaInfo, duration: Long)
}

interface PlayStateChanged {
    fun onPlayStateChanged(playing: Boolean)
}

interface LoadingChanged {
    fun onLoadingChanged(loading: Boolean)
}

interface PlayModeChanged {
    fun onPlayModeChanged(mode: Int)
}