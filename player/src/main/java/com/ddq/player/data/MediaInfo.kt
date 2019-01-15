package com.ddq.player.data

import android.net.Uri
import android.os.Bundle
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import java.io.Serializable

/**
 * created by dongdaqing 19-1-14 下午1:57
 */
data class MediaInfo(
    val mediaCode: String,
    val mediaName: String,
    val mediaDesc: String?,
    val mediaCover: String?,
    val duration: Long,
    val uri: String,
    val extras: Bundle?
) : Serializable

fun MediaInfo.toMediaSource(dataSourceFactory: DataSource.Factory) =
    ExtractorMediaSource.Factory(dataSourceFactory)
        .setTag(this)
        .createMediaSource(Uri.parse(uri))

fun List<MediaInfo>.toMediaSource(dataSourceFactory: DataSource.Factory, runnable: Runnable): ConcatenatingMediaSource {
    val source = ConcatenatingMediaSource()
    forEach {
        source.addMediaSource(it.toMediaSource(dataSourceFactory), runnable)
    }
    return source
}