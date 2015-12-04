# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jonathan.stanford/bin/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class com.jbirdvegas.mgerrit.database.** { *; }
-keep class com.jbirdvegas.mgerrit.search.** { *; }
-keep class com.jbirdvegas.mgerrit.message.** { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View { public <init>(android.content.Context); public <init>(android.content.Context, android.util.AttributeSet); public <init>(android.content.Context, android.util.AttributeSet, int); public void set*(...); }
-keepclassmembers class * extends android.app.Activity { public void *(android.view.View); }

-keepclassmembers class * extends com.sun.jna.** {
    <fields>;
    <methods>;
}

-keep class org.eclipse.jgit.lib.Repository
-keep class org.eclipse.jgit.transport.RemoteConfig { *** removeURI(...); }

-dontwarn android.support.v4.**
-dontwarn groovy.**
-dontwarn org.codehaus.groovy.**
-dontwarn org.eclipse.jgit.**
-dontwarn org.ajoberstarr.gradle.git.**
-dontwarn com.sun.jna.**
-dontwarn com.jcraft.jsch.jgss.**
-dontwarn com.jcraft.jsch.jzlib.**
-dontwarn com.jcraft.jsch.jcraft.**
-dontwarn org.gradle.api.**
-dontwarn org.gradle.util.**
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.lang.Throwable
-dontwarn org.slf4j**