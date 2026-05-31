package me.lingci.dy.player.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import me.lingci.dy.player.R
import java.io.File

// 圆
var circleOptions: RequestOptions = RequestOptions.circleCropTransform()

// 设置圆角大小为 8dp
var rd8doOptions = RequestOptions.bitmapTransform(RoundedCorners(8))

fun roundedOptions(rounded: Int): RequestOptions {
    return RequestOptions.bitmapTransform(RoundedCorners(rounded))
}

val test = RequestOptions.bitmapTransform(GranularRoundedCorners(0f, 0f, 0f, 0f))

fun mediaLoad(url: String, imageView: ImageView) {
    Glide.with(imageView.context).load(url)
        .apply(RequestOptions()
            .placeholder(R.drawable.ic_media_default)
            .fallback(R.drawable.ic_media_default)
            .error(R.drawable.ic_media_default)
        )
        .transform(RoundedCorners(16))
        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
        .into(imageView)
}

fun mediaLoad(file: File, imageView: ImageView) {
    Glide.with(imageView.context).load(file)
        .apply(RequestOptions()
            .placeholder(R.drawable.ic_media_default)
            .fallback(R.drawable.ic_media_default)
            .error(R.drawable.ic_media_default)
        )
        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
        .into(imageView)
}