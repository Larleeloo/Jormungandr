# Keep Gson model classes (used for JSON serialization)
-keep class com.larleeloo.jormungandr.model.** { *; }
-keep class com.larleeloo.jormungandr.cloud.SyncResult { *; }

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
