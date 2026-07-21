package com.ttcn.promotionsdk.core.data.dto.voucher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Khuôn `voucher` chung của ví voucher — dùng cho cả API 3.5.9 (Get Customer Voucher Detail §6.2)
 * và 3.5.10 (Search Customer Vouchers §6.4, `content[].voucher`).
 *
 * Breaking từ Detail v1.3 / Search v1.7: bỏ lớp `campaign`, mỗi item phẳng gồm object `voucher` này
 * + các field ngang hàng ([CustomerVoucherDetail] / [VoucherListItem]).
 */
@Serializable
data class VoucherInfoDto(
    /** ID voucher (= campaign_id sinh ra voucher). Dùng cho API 3.5.9 (Get Customer Voucher Detail). */
    @SerialName("id") val id: String,
    @SerialName("brand") val brand: VoucherBrandDto? = null,
    /** Ảnh banner của ưu đãi. */
    @SerialName("image") val image: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("remainingQty") val remainingQty: Int? = null,
    @SerialName("usedQty") val usedQty: Int? = null,
    /** Mã số loại giảm giá (enum). VD 2 = giảm theo %. */
    @SerialName("discountType") val discountType: Int? = null,
    @SerialName("discountValue") val discountValue: Double? = null,
    @SerialName("maxDiscount") val maxDiscount: Double? = null,
    @SerialName("minOrder") val minOrder: Double? = null,
    /** Nội dung/điều khoản ngắn của ưu đãi. */
    @SerialName("content") val content: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    /** Ngày kết thúc hiệu lực voucher, format `dd-MM-yyyy`. */
    @SerialName("endDate") val endDate: String? = null,
    /** Ngày kết thúc chiến dịch, format `dd-MM-yyyy`. */
    @SerialName("campaignEndDate") val campaignEndDate: String? = null,
    /** Số ngày hiệu lực của mã (bản chuỗi của `expiredTimeNumber`). */
    @SerialName("expiredTime") val expiredTime: String? = null,
    @SerialName("unlimitedQty") val unlimitedQty: Boolean? = null,
)

@Serializable
data class VoucherBrandDto(
    @SerialName("name") val name: String? = null,
    /** Mảng URL logo (nhiều kích cỡ/biến thể). */
    @SerialName("logo") val logo: List<String> = emptyList(),
)

/**
 * `metadata` map phẳng key-value của voucher (Detail §6.4 / Search §6.5). Chỉ khai các key **điều
 * khiển hiển thị** đã biết; key custom theo chương trình được `ignoreUnknownKeys` bỏ qua.
 */
@Serializable
data class VoucherMetadataDto(
    /** `"true"` / `"false"` — enable/disable nút "Sử dụng". */
    @SerialName("usable") val usable: String? = null,
    @SerialName("displayMode") val displayMode: String? = null,
    /** Lý do vô hiệu khi `usable="false"` (EXPIRED / REDEEMED / SERVICE_NOT_APPLICABLE). */
    @SerialName("disabledReason") val disabledReason: String? = null,
    @SerialName("usageGuideUrl") val usageGuideUrl: String? = null,
    @SerialName("eligibilityScore") val eligibilityScore: String? = null,
    @SerialName("matchedRules") val matchedRules: String? = null,
)

/** Một mã (coupon code) đã cấp cho khách — chỉ có ở API Detail (§6.3), list không kèm. */
@Serializable
data class VoucherCodeDto(
    @SerialName("phone") val phone: String? = null,
    @SerialName("codex") val codex: String? = null,
    /** Hạn dùng của mã, format `dd/MM/yyyy`. */
    @SerialName("expiredAt") val expiredAt: String? = null,
    /** Thời điểm khách nhận mã, format `dd/MM/yyyy HH:mm:ss`. */
    @SerialName("pickedUpAt") val pickedUpAt: String? = null,
)
