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
    var queueChanged: QueueChanged? = null

    init {
        with(filter) {
            addAction(Commands.ACTION_TRACK_CHANGED)
            addAction(Commands.ACTION_LOADING_CHANGED)
            addAction(Commands.ACTION_PLAY_STATE_CHANGED)
            addAction(Commands.ACTION_REPEAT_MODE_CHANGED)
            addAction(Commands.ACTION_SHUFFLE_MODE_CHANGED)
            addAction(Commands.ACTION_POSITION_DISCONTINUITY_CHANGED)
            addAction(Commands.ACTION_COUNTING)
            addAction(Commands.ACTION_COUNT_CANCEL)
            addAction(Commands.ACTION_PLAYER_CURRENT_STATE)
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
            Commands.ACTION_PLAYER_CURRENT_STATE -> {
                onTrackChanged(intent)
                onPlayModeChanged(intent)
                onPlayStateChanged(intent)
                onCountChanged(intent.getLongExtra("seconds_left", 0))
            }
            Commands.ACTION_TRACK_CHANGED -> onTrackChanged(intent)
            Commands.ACTION_LOADING_CHANGED -> onLoadingChanged(intent)
            Commands.ACTION_PLAY_STATE_CHANGED -> onPlayStateChanged(intent)
            Commands.ACTION_REPEAT_MODE_CHANGED -> onPlayModeChanged(intent)
            Commands.ACTION_SHUFFLE_MODE_CHANGED -> {
            }
            Commands.ACTION_POSITION_DISCONTINUITY_CHANGED -> {
            }
            Commands.ACTION_COUNTING -> onCountChanged(intent.getLongExtra("seconds_left", 0))
            Commands.ACTION_COUNT_CANCEL -> onCountChanged(0)
            Commands.ACTION_ITEM_REMOVED -> queueChanged?.onQueueChanged()
        }
    }

    private fun onTrackChanged(intent: Intent) {
        trackChanged?.onTrackChange(
            intent.getSerializableExtra("media") as MediaInfo,
            intent.getLongExtra("duration", 0),
            intent.getLongExtra("position", -1)
        )
    }

    private fun onPlayStateChanged(intent: Intent) {
        playStateChanged?.onPlayStateChanged(
            intent.getBooleanExtra(
                "play",
                false
            )
        )
    }

    private fun onPlayModeChanged(intent: Intent) {
        playModeChanged?.onPlayModeChanged(
            intent.getIntExtra(
                "mode",
                Player.REPEAT_MODE_OFF
            )
        )
    }

    private fun onCountChanged(mills: Long) {
        count?.onCounting(mills)
    }

    private fun onLoadingChanged(intent: Intent) {
        loadingChanged?.onLoadingChanged(
            intent.getBooleanExtra(
                "loading",
                false
            )
        )
    }
}

interface Count {
    fun onCounting(mills: Long)
}

interface TrackChanged {
    /**
     * use [duration] instead of [MediaInfo.duration]
     * @param position 只有在[Commands.ACTION_PLAYER_CURRENT_STATE]时才有用，其他的action下值是-1
     */
    fun onTrackChange(mediaInfo: MediaInfo, duration: Long, position: Long)
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

interface QueueChanged {
    fun onQueueChanged()
}