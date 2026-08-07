# Giữ public API của SDK — host app gọi qua các class này. `com.ttcn.prm.entry.**` là bề mặt
# DUY NHẤT: mọi thứ ngoài package đó đều `internal`, R8 rút gọn thoải mái.
-keep public class com.ttcn.prm.entry.** { public *; }
-keep public class com.ttcn.promotionsdk.di.PromotionContainer { public *; }
-keep public class com.ttcn.promotionsdk.config.** { public *; }
-keep public class com.ttcn.promotionsdk.domain.model.** { *; }
-keep public class com.ttcn.promotionsdk.domain.exception.PromotionErrorCodes { *; }

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
