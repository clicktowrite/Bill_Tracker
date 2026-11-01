# Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-dontwarn androidx.room.paging.**

# DataStore Keep Rules
-keep class androidx.datastore.** { *; }

# Compose Material Icons Keep Rules
-dontwarn androidx.compose.material.icons.**

# General Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
