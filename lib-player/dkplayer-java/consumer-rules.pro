# ============================================================
# dkplayer-java Consumer ProGuard Rules for AGP 9 & R8
# ============================================================

# ============================================================
# 1. DKPlayer 核心类 (反射调用)
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
# 2. 播放器状态和监听器
# ============================================================
-keep class xyz.doikki.videoplayer.player.VideoView$** { *; }
-keep interface xyz.doikki.videoplayer.player.VideoView$** { *; }

# ============================================================
# 3. 通用规则
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
