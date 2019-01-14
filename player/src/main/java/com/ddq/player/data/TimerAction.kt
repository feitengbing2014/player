package com.ddq.player.data

import android.content.Intent
import java.io.Serializable
import java.util.*

/**
 * created by dongdaqing 19-1-14 上午11:13
 */
data class TimerAction(
    val fire: Long,
    val action: String
) : Serializable

fun List<TimerAction>.toPendingActions(): Queue<Intent> {
    val list = LinkedList<Intent>()
    forEach {
        val intent = Intent(it.action)
        intent.putExtra("fire", it.fire)
        list.add(intent)
    }
    return list
}