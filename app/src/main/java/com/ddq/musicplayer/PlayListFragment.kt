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
import com.bumptech.glide.Glide
import com.ddq.player.MediaService
import com.ddq.player.ServiceBinder
import com.ddq.player.data.MediaInfo
import kotlinx.android.synthetic.main.recycler_play_list_item.view.*

/*
* dongdaqing 2019/1/15 0015 22:44
*/
class PlayListFragment : AppCompatDialogFragment() {

    var list: List<MediaInfo>? = null
    var service: ServiceBinder? = null
    var adapter: Adapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.BottomDialog)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val recyclerView = RecyclerView(context)
        recyclerView.setBackgroundColor(Color.WHITE)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = Adapter(ArrayList(list), context)
        recyclerView.adapter = adapter
        return recyclerView
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        dialog.window.setGravity(Gravity.BOTTOM)
    }

    inner class Adapter(private val list: ArrayList<MediaInfo>?, context: Context?) : RecyclerView.Adapter<Item>() {

        val inflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Item {
            return Item(inflater.inflate(R.layout.recycler_play_list_item, parent, false))
        }

        override fun getItemCount(): Int {
            if (list == null)
                return 0
            return list.size
        }

        override fun onBindViewHolder(holder: Item, position: Int) {
            holder.bind(list!![position])
        }

        fun remove(index: Int) {
            list?.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class Item(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener {
                service?.seekToWindow(adapterPosition)
            }

            itemView.delete.setOnClickListener {
                service?.remove(adapterPosition)
                adapter?.remove(adapterPosition)
            }
        }

        fun bind(mediaInfo: MediaInfo) {
            itemView.name.text = mediaInfo.mediaName
            Glide.with(itemView).load(mediaInfo.mediaCover).into(itemView.image)
        }
    }
}