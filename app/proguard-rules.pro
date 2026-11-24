# ==============================================================================
# CycleGo India - Production R8 / ProGuard Configuration
# ==============================================================================

# Preserve line numbers for meaningful Crashlytics & AppCrashHandler stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve all Firebase Firestore Data Models (Required for deserialization)
-keep class com.sanika.bicyclebookingapp.model.** { *; }
-keepclassmembers class com.sanika.bicyclebookingapp.model.** {
    <fields>;
    <methods>;
}

# Preserve Firestore document annotations
-keepattributes *Annotation*
-keep @interface com.google.firebase.firestore.DocumentId
-keep @interface com.google.firebase.firestore.ServerTimestamp
-keep @interface com.google.firebase.firestore.Exclude

# Firebase App Check & Play Integrity
-keep class com.google.firebase.appcheck.** { *; }
-dontwarn com.google.firebase.appcheck.**

# Razorpay Payment Gateway SDK
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-dontwarn okio.**
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Google Play Services (Maps, Location, Tasks)
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.**

# ZXing Barcode & QR Scanner
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# Room Local Database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Glide Image Loader
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ViewBinding and SafeArgs
-keep class com.sanika.bicyclebookingapp.databinding.** { *; }
