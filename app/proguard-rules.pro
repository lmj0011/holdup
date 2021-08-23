# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ref: https://stackoverflow.com/questions/9651703/using-proguard-with-android-without-obfuscation
#-dontobfuscate
#-optimizationpasses 5
#-dontusemixedcaseclassnames
#-dontskipnonpubliclibraryclasses
#-dontpreverify
#-verbose
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes Signature,SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ref: https://stackoverflow.com/a/46333633/2445763
-keep class org.kodein.type.TypeReference
-keepclasseswithmembernames class * { @org.kodein.type.TypeReference <methods>; }
-keepclasseswithmembernames class * { @org.kodein.type.TypeReference <fields>; }
