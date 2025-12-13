# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve project model classes
-keep class com.uoa.safedriveafrica.core.model.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod

# Keep data classes
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keepclassmembers class * extends dagger.hilt.android.lifecycle.HiltViewModel {
    <init>(...);
}

# Keep Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Retrofit & OkHttp (if used)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.**

# PdfBox (tom-roush/PdfBox-Android)
# Keep PdfBox classes
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }

# Suppress warnings for optional PdfBox dependencies
# These are optional codecs that aren't required for basic PDF operations
-dontwarn com.gemalto.jp2.**
-dontwarn javax.imageio.**
-dontwarn java.awt.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.bouncycastle.**

# Keep PdfBox filter classes (used for image processing)
-keep class com.tom_roush.pdfbox.filter.** { *; }

# Keep PdfBox resources
-keepclassmembers class com.tom_roush.pdfbox.** {
    public <init>(...);
}

# Prevent obfuscation of PdfBox reflection-based code
-keepattributes *Annotation*,Signature,Exception

# Keep specific decoder classes that might be dynamically loaded
-keep class com.gemalto.jp2.JP2Decoder { *; }
