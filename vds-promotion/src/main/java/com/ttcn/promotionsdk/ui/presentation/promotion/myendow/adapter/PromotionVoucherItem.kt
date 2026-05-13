package com.ttcn.promotionsdk.ui.presentation.promotion.myendow.adapter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PromotionVoucherItem(
    val id: String,
    val name: String,
    val discount: String,
    val isExpired: Boolean = false,
    var isNotEnoughApplied: Boolean = false,
    var isApplied: Boolean = false,
) : Parcelable

// Enum cho trạng thái view
enum class EndowViewState {
    EMPTY,              // Chưa có voucher nào
    NOT_APPLIED,        // Có voucher nhưng chưa apply
    APPLIED             // Đã apply voucher
}