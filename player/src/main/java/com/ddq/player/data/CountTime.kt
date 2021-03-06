package com.ddq.player.data

import java.io.Serializable

/*
* dongdaqing 2019/1/13 0013 16:58
*/
data class CountTime(val type: Int, val millSeconds: Long) : Serializable {
    fun getDesc(): String {
        val com = millSeconds % 60000
        if (com == 0L)
            return "${millSeconds / 60000}分钟"
        return "${millSeconds / 1000}秒"
    }

    companion object {
        const val TYPE_NORMAL = 1
        const val TYPE_CURRENT = 2
    }
}