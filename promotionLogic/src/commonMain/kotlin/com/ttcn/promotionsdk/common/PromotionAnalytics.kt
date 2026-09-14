package com.ttcn.promotionsdk.common

import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.host.PromotionEvent
import com.ttcn.promotionsdk.host.PromotionTracker

/**
 * Quy ước đặt tên event + **tham số dùng chung giữa các feature**.
 *
 * Ở đây **cố ý không có tên event nào**. Tên event thuộc về feature bắn nó, nên nó nằm cạnh store
 * của feature đó (`ChoosePromotionEvents`, `PromotionDetailEvents`). Gom hết tên vào một file ở
 * `common/` thì tầng nền phải biết danh sách feature — chiều phụ thuộc ngược, và là chỗ đầu tiên vỡ
 * khi mỗi feature tách thành một module Gradle riêng.
 *
 * Cái *thật sự* dùng chung chỉ có hai thứ, và cả hai đều ở đây:
 *
 * 1. [PREFIX] — để event của SDK không đụng event của host.
 * 2. Tên tham số lặp lại ở nhiều feature — `voucher_id` viết hai kiểu ở hai màn là hai cột trong
 *    dashboard.
 *
 * Tên event là **hợp đồng với BI**, không phải chi tiết nội bộ: thêm thì tự do, đổi/xoá thì ghi vào
 * `CHANGELOG.md`. Danh mục đầy đủ: `docs/common/HostCapabilities.md` §4.1.
 */
internal object PromotionEvents {

    /** Quy ước: `prm_<màn>_<hành_động>`, snake_case. */
    const val PREFIX: String = "prm_"

    const val PARAM_VOUCHER_ID: String = "voucher_id"
    const val PARAM_MY_COUNT: String = "my_count"
    const val PARAM_OTHER_COUNT: String = "other_count"
    const val PARAM_HAS_KEYWORD: String = "has_keyword"
    const val PARAM_REJECTED_COUNT: String = "rejected_count"
    const val PARAM_SELECTED: String = "selected"
    const val PARAM_STATUS: String = "status"
}

/**
 * Cửa duy nhất mà lõi bắn event ra [PromotionTracker] của host.
 *
 * Tồn tại vì hai chuyện mà từng call-site không nên phải tự lo:
 *
 * 1. **SDK chưa khởi tạo.** Tracker nằm trong đồ thị DI, mà store có thể được dựng/chạy khi đồ thị
 *    chưa có (unit test ở `commonTest` dựng store trực tiếp, không `initialize`). Bỏ qua **im lặng**
 *    ở nhánh này là đúng: chưa có host nào để nhận thì không có gì để cảnh báo.
 * 2. **Host ném.** Tracking là phụ trợ; một exception từ `track()` của host **không** được kéo đổ
 *    màn hình đang hiển thị. Nhánh này thì **có** log — nuốt lỗi của host mà im hoàn toàn là cách
 *    hỏng tệ nhất, đúng lý do [promotionWarn] tồn tại.
 *
 * Nó **không biết feature nào tồn tại** — chỉ nhận một chuỗi và một map. Nhờ vậy feature thêm sau
 * (kể cả ở module khác) dùng được ngay mà không sửa gì ở đây.
 */
internal object PromotionAnalytics {

    fun track(name: String, params: Map<String, String> = emptyMap()) {
        track(PromotionEvent(name, params))
    }

    fun track(event: PromotionEvent) {
        if (!PromotionContainer.isInitialized()) return
        runCatching { get<PromotionTracker>().track(event) }
            .onFailure { promotionWarn("Tracker của host ném ở event '${event.name}': ${it.message}") }
    }
}
