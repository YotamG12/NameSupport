# Add project specific ProGuard rules here.

# Room — keep entities and DAOs so generated code can find them at runtime
-keep class com.namesupport.data.db.** { *; }

# WorkManager — keep worker class names used in WorkRequest
-keep class com.namesupport.worker.** { *; }

# BroadcastReceivers — exported and referenced by name in PendingIntents
-keep class com.namesupport.receiver.** { *; }

# DataStore — keep preferences keys
-keepclassmembers class * {
    @androidx.datastore.preferences.core.Preferences$Key *;
}

# Gemini AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
