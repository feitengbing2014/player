package com.ddq.player.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * created by dongdaqing 19-1-11 下午2:40
 */
abstract class Timer(millisInFuture: Long, countDownInterval: Long) {
    /**
     * Millis since epoch when alarm should stop.
     */
    private val mMillisInFuture = millisInFuture

    /**
     * The interval in millis that the user receives callbacks
     */
    private val mCountdownInterval = countDownInterval

    private var mStopTimeInFuture: Long = 0

    /**
     * boolean representing if the timer was cancelled
     */
    private var mCancelled = false

    private var mPaused = true

    private var millisLeft: Long = 0

    /**
     * Cancel the countdown.
     */
    @Synchronized
    fun cancel() {
        mCancelled = true
        mHandler.removeMessages(MSG)
    }

    @Synchronized
    fun pause() {
        mPaused = true
        mHandler.removeMessages(MSG)
    }

    /**
     * Cancel the countdown after it is initialized.
     */
    @Synchronized
    fun cancelAfterCreate() {
        mCancelled = true

    }

    /**
     * Start the countdown.
     */
    @Synchronized
    fun start(): Timer {
        mCancelled = false
        mPaused = false
        if (mMillisInFuture <= 0) {
            onFinish()
            return this
        }
        millisLeft = mMillisInFuture
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture
        mHandler.sendMessage(mHandler.obtainMessage(MSG))
        return this
    }

    @Synchronized
    fun resume(): Timer {
        if (!mPaused || mCancelled)
            return this

        mPaused = false
        if (millisLeft <= 0) {
            onFinish()
            return this
        }
        mStopTimeInFuture = SystemClock.elapsedRealtime() + millisLeft
        mHandler.sendMessage(mHandler.obtainMessage(MSG))
        return this
    }


    /**
     * Callback fired on regular interval.
     *
     * @param millisUntilFinished The amount of time until finished.
     */
    abstract fun onTick(millisUntilFinished: Long)

    /**
     * Callback fired when the time is up.
     */
    abstract fun onFinish()


    private val MSG = 1

    // handles counting down
    private val mHandler = Handler(Looper.getMainLooper()) {
        synchronized(this@Timer) {
            if (mCancelled || mPaused) {
                true
            }

            millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime()
            if (millisLeft <= 0) {
                onFinish()
            } else {
                val lastTickStart = SystemClock.elapsedRealtime()
                onTick(millisLeft)

                // take into account user's onTick taking time to execute
                val lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart
                var delay: Long

                if (millisLeft < mCountdownInterval) {
                    // just delay until done
                    delay = millisLeft - lastTickDuration

                    // special case: user's onTick took more than interval to
                    // complete, trigger onFinish without delay
                    if (delay < 0) delay = 0
                } else {
                    delay = mCountdownInterval - lastTickDuration

                    // special case: user's onTick took more than interval to
                    // complete, skip to next interval
                    while (delay < 0) delay += mCountdownInterval
                }

                postDelay(delay)
            }
        }
        true
    }

    private fun postDelay(delay: Long) {
        val msg = mHandler.obtainMessage(MSG)
        mHandler.sendMessageDelayed(msg, delay)
    }

}