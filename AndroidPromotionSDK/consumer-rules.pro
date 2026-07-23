# Giữ public API của SDK — host app gọi qua các class này.
-keep public class com.ttcn.promotionsdk.ui.entry.** { public *; }
-keep public class com.ttcn.promotionsdk.core.di.PromotionContainer { public *; }
-keep public class com.ttcn.promotionsdk.core.config.** { public *; }
-keep public class com.ttcn.promotionsdk.core.domain.model.** { *; }
-keep public class com.ttcn.promotionsdk.core.domain.exception.PromotionErrorCodes { *; }

# kotlinx.serialization sinh serializer qua companion object; R8 hay cắt nhầm.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.ttcn.promotionsdk.** {
    *** Companion;
}
-keepclasseswithmembers class com.ttcn.promotionsdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor + OkHttp
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
