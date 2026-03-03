# 1. Preserve Generic Signatures (The most important part for your error)
-keepattributes Signature, EnclosingMethod, InnerClasses
-keepattributes Annotation

# 2. Keep Retrofit and OkHttp internal structures
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# 3. Keep your Data Models (Update these paths to match your actual package)
# This prevents Gson from renaming variables like 'prediction' to 'a'
-keep class com.app.maldroid.data.model.** { *; }
-keep class com.app.maldroid.api.** { *; }

# 4. If you use Gson, keep its specific requirements
-keep class com.google.gson.** { *; }
-keep class androidx.lifecycle.LiveData { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

