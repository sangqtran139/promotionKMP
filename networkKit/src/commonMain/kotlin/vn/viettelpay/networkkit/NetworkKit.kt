package vn.viettelpay.networkkit

/**
 * UC0: chưa có logic networking — chỉ là điểm neo để commonTest có gì đó thật để assert, xác nhận
 * toolchain KMP (commonMain/androidMain/iosMain, publish mavenLocal) chạy thông trước khi UC1+ đắp
 * dần HttpClient factory, header, token provider... Xem docs/common/SharedNetworkKit.md.
 */
internal object NetworkKit {
    const val VERSION: String = "0.1.0"
}
