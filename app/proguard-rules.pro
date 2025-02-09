# Keep your model classes
-keep class com.Hamp.HolyQuran.Surah { *; }
-keep class com.Hamp.HolyQuran.Verse { *; }
-keep class com.Hamp.HolyQuran.Translator { *; }
-keep class com.Hamp.HolyQuran.TranslationResponse { *; }

# Rules for Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface retrofit2.** { *; }

# Rules for Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }