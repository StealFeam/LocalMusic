# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\InstallSoftware\android_sdk/tools/proguard/proguard-android.txt
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
-dontpreverify
-keepattributes *Annotation*,InnerClasses       #保持注解
-keepattributes Signature
# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

-optimizations !code/simplification/cast,!field/*,!class/merging/*
# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
public static java.lang.String TABLENAME;
}
-keep class **$Properties
# If you do not use SQLCipher:
-dontwarn org.greenrobot.greendao.database.**
# If you do not use Rx:
-dontwarn rx.**

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keepclasseswithmembers public class * implements kotlin.coroutines.CoroutineContext

-keep public interface kotlin.coroutines.CoroutineContext{*;}
-keep public interface kotlin.coroutines.CoroutineContext$Key{*;}
-keep public interface kotlin.jvm.functions.**
-keep public class kotlin.coroutines.**{*;}
-keep public class kotlinx.coroutines.**{*;}
-keep public interface kotlinx.coroutines.**{*;}

-keep public class * extends android.app.Activity {
   public boolean *(android.view.Menu);
   public boolean onCreateOptionsMenu(android.view.Menu);
}

-keepclasseswithmembernames public class com.zy.ppmusic.mvp.view.MediaActivity{
    public boolean onCreateOptionsMenu(android.view.Menu);
}

-keepclassmembernames public class android.support.v7.view.menu.MenuBuilder{*;}

# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
