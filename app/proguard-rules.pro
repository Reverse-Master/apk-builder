# Keep apksig + Bouncy Castle reflective entries
-keep class com.android.apksig.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.android.apksig.**
