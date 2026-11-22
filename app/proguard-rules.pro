# youtubedl-android (refleksi + JSON mapping via Jackson)
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Model aplikasi
-keep class com.mp34dl.model.** { *; }

# Dependensi opsional yang tidak ada di Android
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn java.beans.**
-dontwarn javax.lang.model.**
-dontwarn org.w3c.dom.bootstrap.**
