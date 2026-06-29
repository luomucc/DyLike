package me.lingci.dy.player.util

import me.lingci.dy.player.entity.VideoData

/**
 * 短视频喜好度推荐排序工具类
 *
 * 采用"探索-利用"平衡策略：
 * - 已播放视频按喜好度排序（利用已知偏好）
 * - 未播放视频获得探索分，均匀混入推荐列表（探索新内容）
 *
 * 评分公式：
 * score = likeWeight * isLike + countWeight * playCount + durationWeight * (playDuration / totalDuration)
 *
 * 默认权重：
 * - 点赞：10分
 * - 播放次数：2分/次（上限20分）
 * - 播放完成率：5分
 * - 未播放探索分：20-35分（基于文件名哈希，确保同一视频分数稳定）
 */
object RecommendSorter {

    // 权重配置
    private const val LIKE_WEIGHT = 10.0
    private const val COUNT_WEIGHT = 3.0 // 播放次数权重提高，反复播放更能说明喜好
    private const val DURATION_WEIGHT = 2.0 // 时长权重降低，避免长视频天然优势

    // 播放次数上限
    private const val MAX_PLAY_COUNT_BONUS = 10

    // 最小播放时长阈值（秒），低于此值视为无效播放
    private const val MIN_PLAY_DURATION = 3L

    // 最大播放时长参考值（秒），用于归一化
    private const val MAX_PLAY_DURATION_REF = 300L

    // 未播放视频探索分配置
    private const val UNPLAYED_BASE_SCORE = 20.0
    private const val UNPLAYED_SCORE_VARIANCE = 15 // 范围：20-35分

    /**
     * 判断视频是否从未被播放过（也未点赞）
     */
    private fun isUnplayed(video: VideoData): Boolean {
        return video.playCount == 0 && video.playDuration == 0L && !video.like
    }

    /**
     * 根据喜好度对视频列表进行排序推荐
     *
     * @param list 原始视频列表
     * @return 排序后的视频列表
     */
    fun sortByPreference(list: List<VideoData>): List<VideoData> {
        if (list.size <= 1) return list.toList()

        // 计算每个视频的喜好度得分
        val scoredList = list.map { video ->
            val score = calculateScore(video)
            video to score
        }

        // 按得分降序排列
        return scoredList.sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * 计算单个视频的喜好度得分
     *
     * @param video 视频数据
     * @return 喜好度得分（0-100）
     */
    fun calculateScore(video: VideoData): Double {
        // 从未播放且未点赞：赋予探索分，确保有机会被推荐
        if (isUnplayed(video)) {
            // 使用文件名哈希产生稳定分数，避免每次排序结果跳动
            val hash = video.name.hashCode()
            return UNPLAYED_BASE_SCORE + (Math.abs(hash) % UNPLAYED_SCORE_VARIANCE)
        }

        var score = 0.0

        // 1. 点赞权重（最高优先级）
        if (video.like) {
            score += LIKE_WEIGHT
            // 如果是最近点赞的，额外加分
            if (video.likeTime > 0) {
                val daysSinceLike = (System.currentTimeMillis() - video.likeTime) / (1000 * 60 * 60 * 24)
                // 7天内点赞额外加分，越近越高
                score += (7 - daysSinceLike.coerceIn(0, 7)) * 0.5
            }
        }

        // 2. 播放次数权重
        val playCountBonus = video.playCount.coerceAtMost(MAX_PLAY_COUNT_BONUS)
        score += playCountBonus * COUNT_WEIGHT

        // 3. 播放时长权重（对数映射，让短视频也能获得合理分数）
        if (video.playDuration >= MIN_PLAY_DURATION) {
            // 使用对数映射：播放 3秒->0.4分, 10秒->0.8分, 30秒->1.2分, 60秒封顶2分
            // 这样短视视频看完也能拿到接近满分，不会被长视频压制
            val durationScore = (Math.log(1 + video.playDuration.toDouble()) / Math.log(61.0))
                .coerceIn(0.0, 1.0) * DURATION_WEIGHT
            score += durationScore
        }

        return score.coerceIn(0.0, 100.0)
    }

    /**
     * 智能随机排序：采用"探索-利用"混合策略
     *
     * 已播放视频按喜好度排序后分组打乱（利用已知偏好），
     * 未播放视频均匀插入到推荐列表中（探索新内容）。
     * 这样既能优先推荐用户喜欢的视频，又能确保新内容被看到。
     */
    fun smartShuffle(list: List<VideoData>): List<VideoData> {
        if (list.size <= 1) return list.toList()

        // 分离未播放和已播放视频
        val unplayed = list.filter { isUnplayed(it) }.shuffled().toMutableList()
        val played = list.filter { !isUnplayed(it) }

        // 已播放视频：按喜好度排序后分组，组内打乱
        val sortedPlayed = sortByPreference(played)
        val playedGroups = mutableListOf<List<VideoData>>()
        val groupSize = 3 // 小组大小，增加随机性
        var index = 0

        while (index < sortedPlayed.size) {
            val end = (index + groupSize).coerceAtMost(sortedPlayed.size)
            val group = sortedPlayed.subList(index, end).toMutableList()
            group.shuffle()
            playedGroups.add(group)
            index = end
        }

        // 混合策略：将未播放视频均匀插入到已播放视频组之间
        // 每2个已播放组插入1个未播放视频
        val result = mutableListOf<VideoData>()
        var playedIndex = 0
        var unplayedIndex = 0
        val insertInterval = 2

        while (playedIndex < playedGroups.size) {
            result.addAll(playedGroups[playedIndex])
            playedIndex++

            // 按间隔插入未播放视频
            if (playedIndex % insertInterval == 0 && unplayedIndex < unplayed.size) {
                result.add(unplayed[unplayedIndex])
                unplayedIndex++
            }
        }

        // 将剩余的未播放视频随机分散到结果中
        while (unplayedIndex < unplayed.size) {
            val insertPos = if (result.isEmpty()) 0 else (Math.random() * result.size).toInt()
            result.add(insertPos, unplayed[unplayedIndex])
            unplayedIndex++
        }

        return result
    }
}
