# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.projectorbit.data.db.entity.** { *; }

# Keep domain models
-keep class com.projectorbit.domain.model.** { *; }

# Keep Hilt generated code
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep coroutine metadata
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
