# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.vexlora.echoband.service.ChatRequest { *; }
-keep class com.vexlora.echoband.service.ChatResponse { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
