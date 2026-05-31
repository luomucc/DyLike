# ============================================================
# player-mpv Consumer ProGuard Rules for AGP 9 & R8
# ============================================================

# ============================================================
# 1. MPV Android Library (JNI 核心)
# ============================================================
-keep class is.xyz.mpv.** { *; }
-keep interface is.xyz.mpv.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class * implements is.xyz.mpv.MPVLib$EventObserver { *; }
-keep class * implements is.xyz.mpv.MPVLib$LogObserver { *; }

-keep class is.xyz.mpv.MPVLib {
    public *;
    private *;
}

-keep class is.xyz.mpv.MPVView { *; }

# ============================================================
# 2. JNI 通用规则
# ============================================================
-keepclasseswithmembers class * {
    native <methods>;
}
-keepclassmembers class * {
    native <methods>;
}

# ============================================================
# 3. 自定义 MPV 封装
# ============================================================
-keep class me.lingci.lib.player.mpv.** { *; }

# ============================================================
# 4. 通用规则
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
