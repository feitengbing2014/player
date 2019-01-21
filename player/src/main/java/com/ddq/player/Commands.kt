package com.ddq.player

/**
 * created by dongdaqing 19-1-11 下午2:17
 */
class Commands {
    companion object {
        //播放器的动作广播
        const val ACTION_TIMELINE_CHANGED = "ddq.player.action.timeline"
        const val ACTION_TRACK_CHANGED = "ddq.player.action.track"
        const val ACTION_LOADING_CHANGED = "ddq.player.action.loading"
        const val ACTION_PLAY_STATE_CHANGED = "ddq.player.action.play_state"
        const val ACTION_REPEAT_MODE_CHANGED = "ddq.player.action.repeat_mode"
        const val ACTION_SHUFFLE_MODE_CHANGED = "ddq.player.action.shuffle_mode"
        const val ACTION_POSITION_DISCONTINUITY_CHANGED = "ddq.player.action.position_discontinuity"
        const val ACTION_COUNTING = "ddq.player.action.counting"//正在倒计时
        const val ACTION_COUNT_CANCEL = "ddq.player.action.count_cancel"
        const val ACTION_SERVICE_DESTROYED = "ddq.player.action.service_destroyed"
        /**
         * 播放器进入缓冲状态，几种情况
         * 1.普通播放器开始播放会首先缓冲数据
         * 2.用户通过播放的进度跳转到位缓冲的数据块，播放器进入缓冲状态
         * 3.缓冲了一部分数据，网络异常，后面的数据没成功缓冲，播放完已缓冲数据，进入缓冲状态
         */
        const val ACTION_BUFFERING = "ddq.player.action.buffering"

        const val ACTION_SERVICE_STARTED = "ddq.player.action.service_started"

        //设置参数
        const val SET_COUNTDOWN_TIMER = "ddq.player.set.countdown_timer" //设置定时器
        const val SET_REPEAT_MODE = "ddq.player.set.repeat_mode" //设置播放模式

        //播放器动作控制
        const val SET_PLAYER_PLAY = "ddq.player.play"//开始播放
        const val SET_PLAYER_PAUSE = "ddq.player.pause"//暂停播放
        const val SET_PLAYER_STOP = "ddq.player.stop"//停止播放
        const val SET_PLAYER_PREVIOUS = "ddq.player.previous"
        const val SET_PLAYER_NEXT = "ddq.player.next"
        const val SET_PLAYER_PLAY_OR_PAUSE = "ddq.player.play_or_pause"//（暂停状态下）开始或者（播放状态下）暂停
        const val SET_PLAYER_DESTROY = "ddq.player.destroy"//停止service

        //查询参数(查询相关状态，返回相应的内容)
        const val QUERY_TIMELINE_POSITION = "ddq.player.query.timeline"
        const val QUERY_TRACK_INFO = "ddq.player.query.track_info"
        const val QUERY_PLAY_STATE = "ddq.player.query.play_state"
        const val QUERY_REPEAT_MODE = "ddq.player.query.repeat_mode"
        const val QUERY_COUNTDOWN_TIMER = "ddq.player.query.countdown_timer"
    }
}