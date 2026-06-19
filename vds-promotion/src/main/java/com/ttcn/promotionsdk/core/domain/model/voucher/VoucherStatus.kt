package com.ttcn.promotionsdk.core.domain.model.voucher

enum class VoucherStatus {
    ACTIVE,
    RESERVED,
    REDEEMED,
    EXPIRED,
    REVOKED,
    SUSPENDED,
    UNKNOWN;

    companion object {
        fun from(raw: String?): VoucherStatus {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
