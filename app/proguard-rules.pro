# Benzo Proguard Rules
-keep class com.frostwire.jlibtorrent.** { *; }
-keep class com.frostwire.jlibtorrent.swig.** { *; }

-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-dontwarn okhttp3.**
-dontwarn okio.**
