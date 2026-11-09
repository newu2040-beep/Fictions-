# Add project specific ProGuard rules here.
# Room ProGuard rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Hilt / Dagger ProGuard rules
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Fictions entities and models
-keep class com.editingcells.fictions.data.local.entity.** { *; }
-keep class com.editingcells.fictions.domain.model.** { *; }

# Backdrop shaders & graphics
-keep class com.kyant.backdrop.** { *; }
