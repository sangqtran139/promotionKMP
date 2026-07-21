package com.ttcn.promotionsdk.core.data.dto.voucher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload `data` của API chi tiết voucher (`GET .../customer-vouchers/{voucherId}`), v1.3.
 * Map sang domain `VoucherDetail` qua [VoucherMapper].
 *
 * Breaking v1.3: bỏ lớp lồng `campaign`. `data` là 1 item phẳng gồm object [VoucherInfoDto] +
 * các field ngang hàng `codes[]` / `quantity` / `value` / `amount` / dates / `metadata`.
 * Trạng thái dùng được chuyển sang `metadata.usable` + `disabledReason`.
 */
@Serializable
data class CustomerVoucherDetail(
    @SerialName("voucher") val voucher: VoucherInfoDto,
    @SerialName("codes") val codes: List<VoucherCodeDto> = emptyList(),
    @SerialName("quantity") val quantity: Int? = null,
    @SerialName("value") val value: Double? = null,
    @SerialName("amount") val amount: Double? = null,
    /** Thời điểm bắt đầu hiệu lực, format `yyyy-MM-dd'T'HH:mm:ss`. */
    @SerialName("startDate") val startDate: String? = null,
    /** Thời điểm kết thúc hiệu lực, format `yyyy-MM-dd'T'HH:mm:ss`. */
    @SerialName("endDate") val endDate: String? = null,
    @SerialName("expiredTimeNumber") val expiredTimeNumber: Int? = null,
    @SerialName("priority") val priority: Int? = null,
    /** `1` = voucher thuộc sở hữu của chính khách. API detail luôn `1`. */
    @SerialName("isYourself") val isYourself: Int? = null,
    @SerialName("metadata") val metadata: VoucherMetadataDto? = null,
)
