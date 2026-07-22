package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.ServiceSelectorUiItem

internal data class PromotionDetailUiState(
    val isLoading: Boolean = false,
    val detail: VoucherDetail? = null,
    val status: VoucherStatus = VoucherStatus.UNKNOWN,
    val actionVisible: Boolean = true,
    val actionEnabled: Boolean = false,
    val actionLabel: String = "",
)

/**
 * Dữ liệu voucher cơ bản (từ màn list) để **seed** card + nút ngay khi mở Detail — trước khi fetch xong.
 * Đối ứng iOS `seedDisplay(from: promotion)`; giúp Android hiện card/nút thay vì shimmer trắng.
 */
internal data class PromotionDetailSeed(
    val merchantName: String,
    val title: String,
    val logo: String,
    val expirationDate: String,
    val status: String,
    val displayStatusLabel: String,
)

internal fun MyVoucherListItem.toDetailSeed() = PromotionDetailSeed(
    merchantName = merchantName,
    title = title,
    logo = logo,
    expirationDate = expirationDate,
    status = status.name,               // round-trip qua VoucherStatus.from ở store
    displayStatusLabel = displayStatusLabel,
)

internal sealed interface PromotionDetailAction {
    /** Seed nút "Dùng ngay" từ trạng thái cơ bản (trước khi fetch detail). */
    data class Seed(val status: String) : PromotionDetailAction
    data class LoadDetail(val voucherId: String) : PromotionDetailAction
    data object OpenServiceSelector : PromotionDetailAction
    data class ServiceSelected(val service: ServiceSelectorUiItem) : PromotionDetailAction
}

internal sealed interface PromotionDetailEffect {
    data class ShowError(val errorCode: String) : PromotionDetailEffect
    data class ShowServiceSelector(val services: List<ServiceSelectorUiItem>) : PromotionDetailEffect
}
