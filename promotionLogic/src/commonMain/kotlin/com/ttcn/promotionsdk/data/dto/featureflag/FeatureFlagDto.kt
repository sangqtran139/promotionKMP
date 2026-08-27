package com.ttcn.promotionsdk.data.dto.featureflag

import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlags
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeatureFlagItemResponse(
    @SerialName("flagName") val flagName: String,
    @SerialName("enabled") val enabled: Boolean,
)

/**
 * Body gọi feature flag. **Không còn `userId` / `sessionId`** — SDK không nhận định danh khách từ
 * host nữa; server tự lấy từ JWT `sub` như mọi API promotion khác.
 *
 * `encodeDefaults = true` ở [PromotionHttpClient] nên body luôn là `{"properties":{}}`, không phải `{}`.
 */
@Serializable
internal data class FeatureFlagRequest(
    @SerialName("properties") val properties: Map<String, String> = emptyMap(),
)

/**
 * Response → cờ tính năng. **Cờ server không trả ⇒ BẬT.**
 *
 * Kill-switch phải là hành động **chủ động** của server: chỉ `"enabled": false` mới tắt. Server
 * quên khai một cờ, hoặc thêm cờ mới mà BFF chưa cấu hình, thì tính năng vẫn chạy.
 *
 * > Trước đây mặc định `false`, với lý do "fail-closed ở tầng DTO, gate lo fail-open". Lý do đó
 * > **không đúng trong thực tế**: `PromotionFeatureGate.isEnabled` chỉ mở khi có *exception* (chưa
 * > initialize, DI chưa sẵn sàng) — cờ `false` do parse ra thì nó cho qua nguyên. Hệ quả đã gặp
 * > thật: BFF trả `ENABLE_ALL` + `VOUCHER_LIST` nhưng thiếu `VOUCHER_DETAIL` → màn danh sách mở
 * > được, bấm vào chi tiết thì bị chặn. Nặng hơn: giá trị sai đó được ghi vào cache, nên những lần
 * > refresh hỏng sau đó vẫn đọc ra `false` — tính năng chết cho tới khi server trả lại cờ.
 */
internal fun List<FeatureFlagItemResponse>.toPromotionFeatureFlags(): PromotionFeatureFlags {
    val map = associate { it.flagName to it.enabled }
    return PromotionFeatureFlags(
        enableAll = map[PromotionFeatureFlag.ENABLE_ALL] ?: true,
        voucherApply = map[PromotionFeatureFlag.VOUCHER_APPLY] ?: true,
        voucherRedeem = map[PromotionFeatureFlag.VOUCHER_REDEEM] ?: true,
        voucherSelection = map[PromotionFeatureFlag.VOUCHER_SELECTION] ?: true,
        voucherDetail = map[PromotionFeatureFlag.VOUCHER_DETAIL] ?: true,
        voucherList = map[PromotionFeatureFlag.VOUCHER_LIST] ?: true,
    )
}
