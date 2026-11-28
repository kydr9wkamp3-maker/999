# Chromecore X ProGuard Rules

# Keep CameraX
-keep class androidx.camera.** { *; }

# Keep Room entities
-keep class com.chromecore.camera.models.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# OpenGL ES shaders are loaded as strings
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep enum values for serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native libraries
-keep class org.opencv.** { *; }
