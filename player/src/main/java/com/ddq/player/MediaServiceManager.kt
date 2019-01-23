package com.ddq.player

import android.annotation.SuppressLint
import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.content.*
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.ddq.player.data.Comparator
import com.ddq.player.data.MediaInfo
import com.ddq.player.util.ProgressChanged

/**
 * created by dongdaqing 19-1-17 上午11:40
 */

class MediaServiceManager private constructor(private val context: Context) : BroadcastReceiver(), ServiceConnection {
    private val runner: ArrayList<Runnable> = ArrayList()

    private var binder: ServiceBinder? = null
    private var starting: Boolean = false
    private var history: Intent? = null
    private var progressChanged: ProgressChanged? = null

    private val observer = GenericLifecycleObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY){
            progressChanged = null
            unTrack()
        }
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        var instance: MediaServiceManager? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context

        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        fun next() {
            action(Runnable { instance!!.binder?.next() })
        }

        fun previous() {
            action(Runnable { instance!!.binder?.previous() })
        }

        fun remove(index: Int) {
            action(Runnable { instance!!.binder?.remove(index) })
        }

        fun remove(mediaInfo: MediaInfo) {
            action(Runnable { instance!!.binder?.remove(mediaInfo) })
        }

        fun prepare(medias: List<MediaInfo>?) {
            action(Runnable { instance!!.binder?.prepare(medias) })
        }

        fun seekToWindow(position: Int) {
            action(Runnable { instance!!.binder?.seekToWindow(position) })
        }

        fun playOrPause() {
            action(Runnable { instance!!.binder?.playOrPause() })
        }

        fun nextPlayMode() {
            action(Runnable { instance!!.binder?.nextPlayMode() })
        }

        fun seekTo(percent: Int) {
            action(Runnable { instance!!.binder?.seekTo(percent) })
        }

        fun track(activity: FragmentActivity, progressChanged: ProgressChanged) {
            if (instance != null) {
                instance!!.progressChanged = progressChanged
                activity.lifecycle.addObserver(instance!!.observer)
            }
            action(true, Runnable { instance!!.binder?.track(progressChanged) })
        }

        fun track(fragment: Fragment, progressChanged: ProgressChanged) {
            if (instance != null) {
                instance!!.progressChanged = progressChanged
                fragment.lifecycle.addObserver(instance!!.observer)
            }
            action(true, Runnable { instance!!.binder?.track(progressChanged) })
        }

        fun pauseTracker() {
            action(Runnable { instance!!.binder?.pauseTracker() })
        }

        fun resumeTracker() {
            action(Runnable { instance!!.binder?.resumeTracker() })
        }

        fun unTrack() {
            action(Runnable { instance!!.binder?.unTrack() })
        }

        fun setTimer(intent: Intent) {
            action(Runnable { instance!!.binder?.setTimer(intent) })
        }

        fun isPlaying(): Boolean {
            return if (instance!!.binder == null) false else instance!!.binder!!.isPlaying()
        }

        fun getCurrentMedia(): MediaInfo? {
            return instance!!.binder?.getCurrentMedia()
        }

        fun sameAsCurrentMedia(mediaInfo: MediaInfo, comparator: Comparator): Boolean {
            val current = getCurrentMedia()
            return comparator.isSame(current, mediaInfo)
        }

        fun sameAsCurrentMedia(mediaInfo: MediaInfo): Boolean {
            return sameAsCurrentMedia(mediaInfo, object : Comparator {
                override fun isSame(a: MediaInfo?, b: MediaInfo?): Boolean {
                    return a != null && a.mediaCode == b?.mediaCode
                }
            })
        }

        fun playlist(): List<MediaInfo>? {
            if (instance?.binder == null && instance?.history != null) {
                return instance?.history!!.getSerializableExtra("medias") as ArrayList<MediaInfo>?
            }
            return instance?.binder?.playlist()
        }

        private fun get(context: Context): MediaServiceManager? {
            if (instance == null) {
                instance = MediaServiceManager(context.applicationContext)
            }
            return instance
        }

        private fun action(ignore: Boolean, runnable: Runnable) {
            if (get(appContext)!!.binder == null) {
                Log.d("MediaService", "start service")
                if (!instance!!.starting) {
                    instance!!.starting = true
                    startService(instance!!.context)
                }
                if (!ignore)
                    instance!!.runner.add(runnable)
            } else {
                runnable.run()
            }
        }

        private fun action(runnable: Runnable) {
            action(false, runnable)
        }

        private fun startService(context: Context) {
            context.startService(Intent(context, MediaService::class.java))
        }
    }

    init {
        val filter = IntentFilter()
        with(filter) {
            addAction(Commands.ACTION_SERVICE_STARTED)
            addAction(Commands.ACTION_SERVICE_DESTROYED)
            addAction(Commands.SET_PLAYER_DESTROY)
        }
        context.applicationContext.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Commands.ACTION_SERVICE_STARTED -> {
                Log.d("MediaService", "service started confirmed, start binding")
                instance!!.starting = false
                bindService(context!!.applicationContext)
            }
            Commands.ACTION_SERVICE_DESTROYED -> {
                Log.d("MediaService", "service destroyed, reset binder reference")
                history = intent
                binder = null
            }
            Commands.SET_PLAYER_DESTROY -> {
                if (intent.getBooleanExtra("clear", false)) {
                    //clear history
                    history = null
                }

                appContext.unbindService(this)
                appContext.stopService(Intent(appContext, MediaService::class.java))
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d("MediaService", "service disconnected")
        binder = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d("MediaService", "service connected")
        binder = service as ServiceBinder?
        binder!!.prepare(history)
        for (run in runner) {
            run.run()
        }
        runner.clear()

        if (progressChanged != null)
            binder!!.track(progressChanged!!)
    }

    private fun bindService(context: Context) {
        context.bindService(Intent(context, MediaService::class.java), this, Context.BIND_AUTO_CREATE)
    }
}