# ============================================================
# player-ui Consumer ProGuard Rules for AGP 9 & R8
# ============================================================

# ============================================================
# 1. DKPlayer 核心
# ============================================================
-keep class xyz.doikki.videoplayer.player.AbstractPlayer { *; }
-keep class * extends xyz.doikki.videoplayer.player.AbstractPlayer { *; }
-keep class xyz.doikki.videoplayer.player.BaseVideoView { *; }
-keep class xyz.doikki.videoplayer.player.VideoView { *; }
-keep class xyz.doikki.videoplayer.controller.BaseVideoController { *; }
-keep class * extends xyz.doikki.videoplayer.controller.BaseVideoController { *; }
-keep interface xyz.doikki.videoplayer.controller.IControlComponent { *; }
-keep class * implements xyz.doikki.videoplayer.controller.IControlComponent { *; }
-keep class xyz.doikki.videoplayer.player.PlayerFactory { *; }

# ============================================================
# 2. 播放器 UI
# ============================================================
-keep class me.lingci.lib.player.widget.videoview.** { *; }
-keep class me.lingci.lib.player.videocontroller.** { *; }

# ============================================================
# 3. DKPlayer VideoCache
# ============================================================
-keep class com.danikula.videocache.** { *; }
-keep interface com.danikula.videocache.** { *; }

# ============================================================
# 4. 播放器实体类 (Kotlinx Serialization)
# ============================================================
-keep @kotlinx.serialization.Serializable class me.lingci.lib.player.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class me.lingci.lib.player.** {
    <fields>;
    <init>(...);
}
-keep class me.lingci.lib.player.model.** { *; }

# ============================================================
# 5. 弹幕视图
# ============================================================
-keep class me.lingci.lib.player.view.DanMuView { *; }

# ============================================================
# 6. 通用规则
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
