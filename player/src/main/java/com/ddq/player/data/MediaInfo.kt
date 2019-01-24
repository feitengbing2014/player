package com.ddq.player.data

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource

/**
 * created by dongdaqing 19-1-14 下午1:57
 */
data class MediaInfo(
    val mediaCode: String,
    val mediaName: String?,
    val mediaDesc: String?,
    val mediaCover: String?,
    val mediaType: String?,
    val duration: Long,
    val uri: String?,
    val extras: Bundle?
) : Parcelable {

    fun isSameCode(mediaInfo: MediaInfo): Boolean {
        return mediaCode == mediaInfo.mediaCode
    }

    fun isSameCodeAndExtra(mediaInfo: MediaInfo, vararg names: String): Boolean {
        if (!isSameCode(mediaInfo))
            return false

        if (extras == null && mediaInfo.extras == null)
            return true

        for (name in names) {
            if (extras?.getString(name) != mediaInfo.extras?.getString(name))
                return false
        }
        return true
    }

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readString(),
        parcel.readBundle(Bundle::class.java.classLoader)
    )

    constructor(mediaCode: String) : this(mediaCode, null, null, null, null, 0, null, null)
    constructor(mediaCode: String, extras: Bundle?) : this(mediaCode, null, null, null, null, 0, null, extras)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mediaCode)
        parcel.writeString(mediaName)
        parcel.writeString(mediaDesc)
        parcel.writeString(mediaCover)
        parcel.writeString(mediaType)
        parcel.writeLong(duration)
        parcel.writeString(uri)
        parcel.writeBundle(extras)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaInfo> {
        override fun createFromParcel(parcel: Parcel): MediaInfo {
            return MediaInfo(parcel)
        }

        override fun newArray(size: Int): Array<MediaInfo?> {
            return arrayOfNulls(size)
        }
    }
}

interface Comparator {
    fun isSame(a: MediaInfo?, b: MediaInfo?): Boolean
}

fun MediaInfo.toMediaSource(dataSourceFactory: DataSource.Factory) =
    ExtractorMediaSource.Factory(dataSourceFactory)
        .setTag(this)
        .createMediaSource(Uri.parse(uri))

fun List<MediaInfo>.toMediaSource(
    dataSourceFactory: DataSource.Factory,
    runnable: Runnable?
): ConcatenatingMediaSource {
    val source = ConcatenatingMediaSource()
    forEach {
        source.addMediaSource(it.toMediaSource(dataSourceFactory), runnable)
    }
    return source
}

fun List<MediaInfo>.findItemIndex(mediaInfo: MediaInfo): Int {
    var postion = -1
    forEachIndexed { index, it ->
        if (mediaInfo.mediaCode == it.mediaCode) {
            postion = index
            return@forEachIndexed
        }
    }
    return postion
}

fun ConcatenatingMediaSource.getMediaInfos(): ArrayList<MediaInfo> {
    val count = size
    val list = ArrayList<MediaInfo>(count)
    for (i in 0..(count - 1)) {
        list.add(getMediaSource(i).tag as MediaInfo)
    }
    return list
}

@Synchronized
fun ConcatenatingMediaSource.removeMediaSource(media: MediaInfo, runnable: Runnable?) {
    for (i in 0 until size) {
        val tag = getMediaSource(i).tag as MediaInfo
        if (tag.mediaCode == media.mediaCode) {
            removeMediaSource(i, runnable)
            break
        }
    }
}