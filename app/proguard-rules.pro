# =====================================================================
# Reglas R8/ProGuard - Gestion Integral de Supermercados
# =====================================================================

# --- Reglas generales ---
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn org.jetbrains.annotations.**

# --- Kotlin / Corrutinas ---
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# --- Room: entidades y DAOs generados ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn com.google.errorprone.annotations.**

# --- Modelos de dominio (serializados en backups JSON) ---
-keep class com.gis.supermercados.data.local.entity.** { *; }
-keep class com.gis.supermercados.domain.model.** { *; }
-keepclassmembers class com.gis.supermercados.core.reporting.model.** { *; }

# --- Motores de exportacion (usados por reflexion de nombres de archivo) ---
-keep class com.gis.supermercados.core.reporting.pdf.** { *; }
-keep class com.gis.supermercados.core.reporting.xlsx.** { *; }

# --- Enums (Room los persiste por nombre) ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class com.gis.supermercados.core.backup.AutoBackupWorker { *; }

# --- Navegacion Compose (argumentos por reflexion) ---
-keepnames class * implements java.io.Serializable
