# =============================================================================
# Consumer ProGuard/R8 rules — vds-promotion SDK
# Các rule này được NHÚNG vào AAR và tự động áp dụng vào app của đối tác
# khi họ bật minify (R8). Mục tiêu: tránh crash do obfuscate model/Retrofit.
# =============================================================================

# --- Public façade: API mà đối tác gọi trực tiếp -----------------------------
-keep class com.ttcn.promotionsdk.ui.entry.** { *; }
-keep interface com.ttcn.promotionsdk.ui.entry.** { *; }
-keep class com.ttcn.promotionsdk.core.config.** { *; }

# --- Domain models trả ra public (headless mode) -----------------------------
# Đối tác đọc field trực tiếp -> không được đổi tên field/class.
-keep class com.ttcn.promotionsdk.core.domain.model.** { *; }

# --- DTO serialize/deserialize bằng Gson -------------------------------------
# Giữ nguyên field name để Gson reflection map đúng JSON.
-keep class com.ttcn.promotionsdk.core.data.dto.** { *; }

# Giữ field được đánh @SerializedName ở mọi nơi
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# --- Retrofit ----------------------------------------------------------------
# Giữ interface API service và metadata generic/annotation cho Retrofit.
-keep,allowobfuscation interface com.ttcn.promotionsdk.core.data.remote.*ApiService { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepattributes Exceptions
# Retrofit phản chiếu kiểu trả về generic (Call<T>, suspend ...).
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- OkHttp / Okio (chỉ cảnh báo, không bắt buộc keep) -----------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# --- Gson generic type token -------------------------------------------------
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Room (scaffold) ---------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# --- Kotlin metadata & Parcelize ---------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
