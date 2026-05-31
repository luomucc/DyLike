package me.lingci.dy.player.video;

import android.view.ViewGroup;

import xyz.doikki.videoplayer.player.VideoView;

/**
 * @author : happyc
 * time    : 2024/09/26
 * desc    :
 * version : 1.0
 */
public interface VideoViewFactory {

    VideoView createVideoView(ViewGroup parent, Object o);

}
