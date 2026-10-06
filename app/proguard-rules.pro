# Google Sign-In / Credential Manager / Play services
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-keep class androidx.credentials.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.api.client.**

# OkHttp / Kotlin
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
