# ProGuard 规则
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-keep class androidx.compose.** { *; }
