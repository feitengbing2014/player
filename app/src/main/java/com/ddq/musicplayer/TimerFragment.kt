package com.ddq.musicplayer

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ddq.player.util.MediaTimer
import kotlinx.android.synthetic.main.recycler_timer_item.view.*

/**
 * created by dongdaqing 19-1-16 上午11:15
 */

interface Callback {
    fun onSelect(mode: Int, mills: Long)
}

class TimerFragment : AppCompatDialogFragment() {

    val list = arrayListOf(-1, 10000L, 60000L)
    var callback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.BottomDialog)
        isCancelable = true
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window.setGravity(Gravity.BOTTOM)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val recyclerView = RecyclerView(context)
        recyclerView.setBackgroundColor(Color.WHITE)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = Adapter(ArrayList(list), context)
        return recyclerView
    }

    inner class Adapter(private val list: ArrayList<Long>?, context: Context?) : RecyclerView.Adapter<Item>() {

        val inflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Item {
            return Item(inflater.inflate(R.layout.recycler_timer_item, parent, false))
        }

        override fun getItemCount(): Int {
            if (list == null)
                return 0
            return list.size
        }

        override fun onBindViewHolder(holder: Item, position: Int) {
            holder.bind(list!![position])
        }
    }

    inner class Item(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener {
                val time = list[adapterPosition]
                callback?.onSelect(if (time == -1L) MediaTimer.TYPE_CURRENT else MediaTimer.TYPE_NORMAL, time)
                dismiss()
            }
        }

        fun bind(time: Long) {
            itemView.name.text = when (time) {
                -1L -> "播放完当前"
                else -> {
                    val seconds = time / 1000
                    if (seconds % 60 == 0L)
                        "${seconds / 60}分钟"
                    else
                        "${seconds}秒"
                }
            }
        }
    }
}