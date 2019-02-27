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
) : Serializable, Comparable<TimerAction> {
    override fun compareTo(other: TimerAction): Int {
        if (fire < other.fire)
            return -1
        else if (fire > other.fire)
            return 1
        return 0
    }
}

fun List<TimerAction>.toPendingActions(): Queue<Intent> {
    val list = LinkedList<Intent>()
    sortedDescending().forEach {
        val intent = Intent(it.action)
        intent.putExtra("fire", it.fire)
        list.add(intent)
    }

    return list
}