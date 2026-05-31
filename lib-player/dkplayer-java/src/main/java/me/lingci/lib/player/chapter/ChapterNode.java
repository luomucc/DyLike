package me.lingci.lib.player.chapter;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

/**
 * 视频章节数据模型
 * 支持 MKV、MP4、ID3v2 等格式的章节信息
 *
 * @param index       章节索引 (从 0 开始)
 * @param title       章节标题
 * @param startTimeMs 章节开始时间 (毫秒)
 * @param endTimeMs   章节结束时间 (毫秒)，可能为 null
 * @param language    语言代码 (如 "zh", "en", "ja")，可能为 null
 */
public record ChapterNode(
        int index,
        String title,
        long startTimeMs,
        Long endTimeMs,
        String language) {

    public ChapterNode(int index, String title, long startTimeMs) {
        this(index, title, startTimeMs, null, null);
    }

    /**
     * 根据当前播放位置判断是否在该章节范围内
     */
    public boolean containsPosition(long positionMs) {
        if (positionMs < startTimeMs) return false;
        if (endTimeMs == null) return true;
        return positionMs < endTimeMs;
    }

    /**
     * 格式化时间显示 (mm:ss)
     */
    public String getFormattedStartTime() {
        return formatTime(startTimeMs);
    }

    public String getFormattedEndTime() {
        return endTimeMs != null ? formatTime(endTimeMs) : "";
    }

    @SuppressLint("DefaultLocale")
    private static String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @NonNull
    @Override
    public String toString() {
        return "ChapterNode{index=" + index + ", title='" + title + "', startTimeMs=" + startTimeMs + "}";
    }

}
