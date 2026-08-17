package com.ttcn.promotionsdk.presentation.serviceselector

import com.ttcn.promotionsdk.config.AvailableService
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.model.voucher.ApplicableProduct

/**
 * Dịch vụ khả dụng cho một voucher: **giao** giữa `applicableProducts.productId` và
 * `availableServices.productId`, loại trùng theo `productId` (giữ thứ tự host cung cấp).
 *
 * **Tầng UI-logic dùng chung** — cả hai nền tảng gọi hàm này; mỗi bên chỉ map [AvailableService]
 * sang model UI riêng (ServiceSelectorUiItem / ServiceSelectorItem) + dựng bottom sheet native.
 *
 * Bản thuần này không đọc config (để test không cần DI); nơi dùng thật gọi
 * [configuredServicesFor].
 */
fun servicesForApplicableProducts(
    applicableProducts: List<ApplicableProduct>,
    availableServices: List<AvailableService>,
): List<AvailableService> {
    val ids = applicableProducts.map { it.productId }.toSet()
    return availableServices
        .filter { it.productId in ids }
        .distinctBy { it.productId }
}

/**
 * [servicesForApplicableProducts] với `availableServices` lấy thẳng từ config đang chạy — điểm gọi
 * **duy nhất** cho cả hai nền tảng khi mở bottom sheet "Chọn dịch vụ".
 *
 * Đọc config **tại thời điểm mở sheet** chứ không nhận qua constructor: `updateSession(...)` dựng
 * lại `PromotionSDKConfig` mới, nên bản snapshot giữ từ lúc dựng màn sẽ là danh mục dịch vụ cũ.
 *
 * @throws IllegalStateException nếu SDK chưa `initialize()` — màn hình của SDK không tồn tại trước
 *   thời điểm đó, nên đây là lỗi lập trình chứ không phải trạng thái cần xử lý.
 */
fun configuredServicesFor(applicableProducts: List<ApplicableProduct>): List<AvailableService> =
    servicesForApplicableProducts(
        applicableProducts,
        PromotionContainer.requireConfig().availableServices,
    )
