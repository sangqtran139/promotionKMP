package com.ttcn.promotionsdk.app

import android.content.Context

/**
 * Nhớ **số điện thoại** của lượt đăng nhập gần nhất để lần sau điền sẵn vào ô.
 *
 * Chỉ là tiện lợi của app demo: không có nó thì mỗi lần mở app phải gõ lại 11 chữ số, mà app demo
 * thì bị mở lại vài chục lần một ngày trong vòng lặp dev.
 *
 * **Chỉ lưu số điện thoại.** PIN và OTP không được ghi xuống đĩa — đó là thông tin xác thực, và
 * `SharedPreferences` là file XML chữ thường trong sandbox app. Số điện thoại thì người dùng cũng
 * nhìn thấy ngay trên màn hình, giữ lại không thêm rủi ro gì.
 *
 * Không phải bề mặt của SDK — SDK không biết app demo đăng nhập kiểu gì, nó chỉ nhận token qua
 * [DemoTokenSource].
 */
internal object DemoLoginPrefs {

    private const val FILE = "demo_login"
    private const val KEY_MSISDN = "msisdn"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Số đã dùng lần trước; chưa từng đăng nhập thì **rỗng** — không có số demo nào để điền hộ. */
    fun lastMsisdn(context: Context): String =
        prefs(context).getString(KEY_MSISDN, null)?.takeIf { it.isNotBlank() }.orEmpty()

    /** Ghi lại sau khi **xin OTP thành công** — số sai thì không đáng nhớ. */
    fun rememberMsisdn(context: Context, msisdn: String) {
        prefs(context).edit().putString(KEY_MSISDN, msisdn).apply()
    }
}
