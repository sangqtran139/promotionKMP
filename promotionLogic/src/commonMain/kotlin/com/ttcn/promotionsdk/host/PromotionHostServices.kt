package com.ttcn.promotionsdk.host

import com.ttcn.promotionsdk.data.local.PromotionPreferences

/**
 * **Gói năng lực do host cấp** — một chỗ duy nhất cho mọi thứ SDK không tự làm được vì nó nằm ở tầng
 * native của app: tracking, kho dữ liệu, và những cổng sẽ thêm về sau.
 *
 * Đây là *cái gói*, không phải một tính năng. Lý do nó tồn tại: nếu mỗi năng lực là một tham số
 * riêng của `PromotionSDKConfig`/`PromotionSDKOptions`/`initialize` thì thêm năng lực thứ ba là sửa
 * **sáu** chữ ký public trên hai nền tảng, và lần nào cũng có nguy cơ một bên quên. Với gói này,
 * thêm năng lực = thêm **một field ở đây**; chữ ký public không đổi.
 *
 * Mọi field đều **tuỳ chọn**. Không cấu hình gì thì SDK chạy y nguyên bằng hiện thực mặc định của
 * mình — host bật đúng thứ mình cần, không phải hiện thực cả gói.
 *
 * ```kotlin
 * PromotionSDK.initialize(
 *     context, tokenSource = …, baseUrl = BASE_URL,
 *     hostServices = PromotionHostServices(tracker = AppPromotionTracker),
 * )
 * ```
 *
 * **Không thuộc gói này:** `PromotionTokenSource` / `PromotionRequestContextProvider`. Chúng cũng là
 * cổng do host cấp, nhưng đứng ở `PromotionSessionConfig` vì token là **thông tin phiên** — gắn với
 * lần đăng nhập, đổi theo user. Gói này là **năng lực hạ tầng**, gắn với vòng đời app.
 *
 * Xem `docs/common/HostCapabilities.md`.
 */
public data class PromotionHostServices(
    /** Nơi nhận event của SDK. `null` → SDK không bắn đi đâu. Xem [PromotionTracker]. */
    val tracker: PromotionTracker? = null,
    /**
     * Kho khoá–giá trị của host thay cho kho mặc định của SDK (`SharedPreferences`/`NSUserDefaults`).
     *
     * Dành cho app đã có sẵn một kho — điển hình là **DB của bản SDK native cũ** — và muốn SDK đọc
     * ghi vào đúng chỗ đó thay vì mở thêm một kho thứ hai. `null` → SDK dùng kho mặc định của mình.
     *
     * **Hợp đồng:** xem [PromotionPreferences]. Đặc biệt lưu ý nó **đồng bộ** và có thể bị gọi từ
     * thread nền, nên hiện thực phải nhanh; và SDK **không** lưu token hay dữ liệu nhạy cảm vào đây.
     */
    val storage: PromotionPreferences? = null,
)
