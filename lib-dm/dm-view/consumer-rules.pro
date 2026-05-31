# ============================================================
# dm-view Consumer ProGuard Rules for AGP 9 & R8
# ============================================================

# ============================================================
# 1. 弹幕视图核心
# ============================================================
-keep class me.lingci.lib.dm.view.DmFlowView { *; }
-keep class me.lingci.lib.dm.view.DmFlowView$** { *; }
-keep class me.lingci.lib.dm.view.render.** { *; }

# ============================================================
# 2. 弹幕解析器
# ============================================================
-keep class me.lingci.lib.dm.view.parser.** { *; }
-keep interface me.lingci.lib.dm.view.parser.** { *; }

# ============================================================
# 3. 弹幕转换器
# ============================================================
-keep class me.lingci.lib.dm.view.converter.** { *; }

# ============================================================
# 4. DanmakuFlameMaster
# ============================================================
-keep class master.flame.danmaku.** { *; }
-keep interface master.flame.danmaku.** { *; }
-keep class tv.cjump.jni.** { *; }

# ============================================================
# 5. 弹幕实体类 (Kotlinx Serialization)
# ============================================================
-keep @kotlinx.serialization.Serializable class me.lingci.lib.dm.view.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class me.lingci.lib.dm.view.** {
    <fields>;
    <init>(...);
}
-keep class me.lingci.lib.dm.view.entity.** { *; }

# ============================================================
# 6. 通用规则
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
