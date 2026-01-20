# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements com.google.gson.TypeAdapterFactory
-keep class com.google.gson.** { *; }

# Music Models (Important for Gson and WebDAV)
-keep class com.example.folderplayer.ui.browser.SourceConfig { *; }
-keep class com.example.folderplayer.ui.browser.SourceType { *; }
-keep class com.example.folderplayer.data.source.MusicFile { *; }
-keep class com.example.folderplayer.utils.LyricLine { *; }
-keep class com.example.folderplayer.data.prefs.CachedMetadata { *; }
-keep class com.example.folderplayer.data.prefs.SourcePreferences$** { *; }
-keep class com.example.folderplayer.data.prefs.PlaybackPreferences$** { *; }

# Sardine / WebDAV / SimpleXML
-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-keep class org.simpleframework.xml.** { *; }
-dontwarn org.simpleframework.xml.**
-keepattributes Element, Root

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
