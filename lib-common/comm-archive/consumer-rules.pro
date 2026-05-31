# ============================================================
# comm-archive Consumer ProGuard Rules for AGP 9 & R8
# ============================================================

# ============================================================
# 1. 7-Zip JBinding (JNI 核心)
# ============================================================
-keep class net.sf.sevenzipjbinding.** { *; }
-keep interface net.sf.sevenzipjbinding.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class * implements net.sf.sevenzipjbinding.IArchiveExtractCallback { *; }
-keep class * implements net.sf.sevenzipjbinding.IArchiveOpenCallback { *; }
-keep class * implements net.sf.sevenzipjbinding.ICryptoGetTextPassword { *; }

-keep enum net.sf.sevenzipjbinding.** { *; }

# ============================================================
# 2. 通用规则
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
