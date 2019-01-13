package com.ddq.player.data

/*
* dongdaqing 2019/1/13 0013 16:58
*/
data class CountTime(val millSeconds: Long) {
    fun getDesc(): String {
        val com = millSeconds % 60000
        if (com == 0L)
            return "${millSeconds / 60000}分钟"
        return "${millSeconds / 1000}秒"
    }
}