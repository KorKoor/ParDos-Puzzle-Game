# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---- ParDos: reglas para la versión de lanzamiento (R8) ----
# Conserva números de línea para poder leer los errores que reporte Play Console / Crashlytics
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Modelos que Firebase/Firestore lee y escribe por reflexión
-keep class com.korkoor.pardos.domain.model.** { *; }
-keepclassmembers class com.korkoor.pardos.domain.model.** { *; }

# Enumeraciones del módulo compartido: se guardan por nombre/id en SharedPreferences
-keepclassmembers enum com.korkoor.pardos.** { *; }

# Play Billing y AdMob traen sus propias reglas; solo evitamos avisos de clases opcionales
-dontwarn com.google.android.gms.**
-dontwarn com.android.billingclient.**
