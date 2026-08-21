package com.ttcn.promotionsdk.presentation.endow

/**
 * Sự kiện SDK báo ra host — **một lần mỗi lần đổi**, không phải state.
 *
 * Đối ứng `PromotionSDKCallback.onVoucherApplied` của hai nền tảng. Native chỉ việc map sang
 * callback tương ứng, không tự quyết định lúc nào nên bắn.
 *
 * `VoucherCountChanged` và `AvailabilityChanged` đã bị bỏ cùng với callback tương ứng — host không
 * cần biết hai thứ đó.
 */
sealed interface EndowHostEvent {
    /**
     * Widget vừa có ưu đãi ở vị trí đầu danh sách (APPLIED hoặc UNAVAILABLE — id vẫn bắn dù
     * [EndowAppliedDiscount.valid] `false`, host tự biết còn hợp lệ hay không qua widget/API riêng),
     * hoặc đang ở một trong hai trạng thái đó mà đổi sang voucher khác. Mang id ưu đãi đầu tiên.
     */
    data class VoucherApplied(val voucherId: String) : EndowHostEvent

}

/**
 * Quyết định **khi nào** phát [EndowHostEvent] — rule dùng chung, thay cho hai bản chép tay.
 *
 * Trước đây mỗi nền tảng tự giữ máy trạng thái riêng và đã lệch nhau: Android theo dõi bằng ba biến
 * (`lastNotifiedState: EndowWidgetState`, `lastNotifiedCount`, `lastNotifiedAvailability`) trong
 * `PRMEndowView`, iOS chỉ hai (`lastNotifiedCount`, `lastNotifiedApplied: Bool`) rải trong
 * `PromotionSDKImpl.render`. Cùng một hợp đồng public với host mà hai cách tính khác nhau, và không
 * bên nào có test.
 *
 * **Có trạng thái, nên mỗi widget một instance.** Android tạo theo `PRMEndowView`; iOS giữ một cái
 * trong `PromotionSDKImpl` (ở đó chỉ có một widget sống tại một thời điểm). Chủ sở hữu vẫn là native
 * vì vòng đời widget hai bên khác nhau — chỉ **rule** là chung.
 *
 * Không phát sự kiện nào khi giá trị không đổi: `render`/`collect` chạy lại mỗi lần state đổi, bắn
 * theo mỗi lần render là host nhận hàng chục callback trùng cho một thao tác.
 */
class EndowHostNotifier {

    /**
     * Id ưu đãi đã báo cho host gần nhất, `null` nếu đang không có ưu đãi nào áp (chưa áp / vừa huỷ).
     * So theo **id** chứ không theo transition trạng thái: "Chọn lại" khi widget đang APPLIED (host tự
     * gọi `PromotionSDK.openChoosePromotion` từ nút riêng, không qua "Hủy" trên widget) đổi thẳng
     * `appliedDiscounts` sang voucher khác mà `widgetState` không hề rời APPLIED — theo dõi bằng
     * transition (bản cũ) thì bỏ lọt lần đổi này, host kẹt callback với voucher cũ.
     */
    private var lastNotifiedVoucherId: String? = null

    /**
     * Trả danh sách sự kiện phải phát cho [state] này, theo đúng thứ tự cần bắn.
     *
     * Bắn ở cả APPLIED lẫn UNAVAILABLE: host cần biết id voucher đã áp dù server trả `valid = false`
     * (vd hết ngân sách/hết hạn giữa chừng) — muốn biết còn hợp lệ hay không thì tự đọc
     * `EndowWidgetState`/gọi `validateDiscounts`, callback này chỉ có nhiệm vụ báo **id**.
     *
     * Gọi cả khi `hasLoadedInitial` còn false cũng an toàn: lúc đó `widgetState` là EMPTY nên không
     * sinh sự kiện nào.
     */
    fun onState(state: EndowState): List<EndowHostEvent> {
        val events = mutableListOf<EndowHostEvent>()

        if (state.widgetState == EndowWidgetState.APPLIED || state.widgetState == EndowWidgetState.UNAVAILABLE) {
            val voucherId = state.appliedDiscounts.firstOrNull()?.objectId
            // Không có id thì KHÔNG bắn — nhưng cũng không đụng lastNotifiedVoucherId, nếu không lần
            // render kế (danh sách đã có id) sẽ bắn lại và host nhận hai lần cho một lần áp.
            if (voucherId != null && voucherId != lastNotifiedVoucherId) {
                events += EndowHostEvent.VoucherApplied(voucherId)
                lastNotifiedVoucherId = voucherId
            }
        } else {
            lastNotifiedVoucherId = null
        }

        return events
    }

}
