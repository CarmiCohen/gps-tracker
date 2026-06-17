# --- Socket.io & Engine.io Rules ---
# These libraries use reflection and dynamic class loading.
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn io.socket.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- JSON/Serialization ---
-keep class org.json.** { *; }
-keep @kotlinx.serialization.Serializable class com.gps19.app.** { *; }

# --- Room Persistence ---
-keep class com.gps19.app.LogEntity { *; }
-keep class com.gps19.app.TrailEntity { *; }
-keep class com.gps19.app.HistoryEntity { *; }
-keep class com.gps19.app.ViolationEntity { *; }
-keep class com.gps19.app.PendingStatusEntity { *; }
-keepclassmembers class com.gps19.app.*Dao { *; }

# --- Protobuf / DataStore ---
-keep class com.gps19.app.AppSettings { *; }
-keep class com.gps19.app.AlertSettings { *; }
-keep class com.google.protobuf.** { *; }

# --- Models & Telemetry (Reflection/JSON) ---
# Ensuring telemetry and UI state models are not obfuscated to prevent serialization failures.
-keep class com.gps19.app.TrackerStatus { *; }
-keep class com.gps19.app.LocationUpdate { *; }
-keep class com.gps19.app.LogEntry { *; }
-keep class com.gps19.app.ConnectionPoint { *; }
-keep class com.gps19.app.DashboardState { *; }
-keep class com.gps19.app.LocationState { *; }
-keep class com.gps19.app.MainUiState { *; }
-keep class com.gps19.app.IntegrityState { *; }
-keep class com.gps19.app.StatsState { *; }
-keep class com.gps19.app.BatteryState { *; }
-keep class com.gps19.app.ConnectivityState { *; }

# --- General Android ---
-keepattributes Signature, *Annotation*, EnclosingMethod
