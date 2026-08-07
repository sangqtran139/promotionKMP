package com.ttcn.promotionsdk.presentation.serviceselector

import com.ttcn.promotionsdk.config.AvailableService
import com.ttcn.promotionsdk.domain.model.voucher.ApplicableProduct

/**
 * Dịch vụ khả dụng cho một voucher: **giao** giữa `applicableProducts.productId` và
 * `availableServices.serviceCode`, loại trùng theo `serviceCode` (giữ thứ tự host cung cấp).
 *
 * **Tầng UI-logic dùng chung** — cả hai nền tảng gọi hàm này; mỗi bên chỉ map [AvailableService]
 * sang model UI riêng (ServiceSelectorUiItem / ServiceSelectorItem) + dựng bottom sheet native.
 *
 * Không đọc config bên trong (để test thuần & không phụ thuộc DI): caller truyền [availableServices]
 * (Android từ `config`, iOS từ `PromotionContainer.requireConfig()`).
 */
fun servicesForApplicableProducts(
    applicableProducts: List<ApplicableProduct>,
    availableServices: List<AvailableService>,
): List<AvailableService> {
    val ids = applicableProducts.map { it.productId }.toSet()
    return availableServices
        .filter { it.serviceCode in ids }
        .distinctBy { it.serviceCode }
}
