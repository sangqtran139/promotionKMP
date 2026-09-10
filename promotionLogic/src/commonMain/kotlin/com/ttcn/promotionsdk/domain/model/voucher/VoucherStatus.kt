package com.ttcn.promotionsdk.domain.model.voucher

/**
 * Mã trạng thái thô của voucher do server trả. Dùng chung Android & iOS.
 */
public enum class VoucherStatus {
    // Dùng được
    ACTIVE,
    AVAILABLE,
    USABLE,

    /** Ưu đãi công khai khách chưa nhận — vẫn cho chọn và dùng. */
    AVAILABLE_TO_CLAIM,

    // Đã dùng
    REDEEMED,
    USED,

    // Hết hạn
    EXPIRED,

    // Không đủ điều kiện / đang bị giữ
    RESERVED,
    REVOKED,
    SUSPENDED,
    INELIGIBLE,
    NOT_ELIGIBLE,

    /** Server trả mã lạ. Xử lý **fail-closed**: xem như không dùng được. */
    UNKNOWN;

    public fun displayState(): VoucherDisplayState = when (this) {
        ACTIVE, AVAILABLE, USABLE, AVAILABLE_TO_CLAIM -> VoucherDisplayState.USABLE
        REDEEMED, USED -> VoucherDisplayState.USED
        EXPIRED -> VoucherDisplayState.EXPIRED
        RESERVED, REVOKED, SUSPENDED, INELIGIBLE, NOT_ELIGIBLE -> VoucherDisplayState.INELIGIBLE
        // Không suy đoán từ dữ liệu khác: mã lạ thì không cho dùng, tránh áp nhầm ưu đãi vào đơn.
        UNKNOWN -> VoucherDisplayState.INELIGIBLE
    }

    public companion object {
        public fun from(raw: String?): VoucherStatus =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

/**
 * Trạng thái hiển thị, dùng chung cho `AndroidPromotionSDK` và `iosPromotionUI`.
 * Thay cho `PromotionDisplayState` (Swift) và phép so sánh `status == ACTIVE` (Android).
 */
public enum class VoucherDisplayState {
    /** Còn dùng được → hiện nút "Dùng ngay". */
    USABLE,
    USED,
    EXPIRED,

    /** Hiển thị mờ, không cho chọn. */
    INELIGIBLE;

    public val isUsable: Boolean get() = this == USABLE
}

/**
 * Trạng thái hiển thị của một voucher. **Không** kiểm tra hạn dùng ở client
 * (`expirationDate < now`) — hoàn toàn dựa vào `status` của server.
 */
public fun VoucherItem.displayState(): VoucherDisplayState = VoucherStatus.from(status).displayState()

public fun VoucherDetail.displayState(): VoucherDisplayState = VoucherStatus.from(status).displayState()
