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
    /** Widget vừa chuyển sang trạng thái ĐÃ ÁP. Mang id ưu đãi đầu tiên. */
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

    /** `null` = chưa render lần nào, nên lần đầu vào APPLIED luôn được tính là chuyển trạng thái. */
    private var lastWidgetState: EndowWidgetState? = null

    /**
     * Trả danh sách sự kiện phải phát cho [state] này, theo đúng thứ tự cần bắn.
     *
     * Gọi cả khi `hasLoadedInitial` còn false cũng an toàn: lúc đó `widgetState` là EMPTY nên không
     * sinh sự kiện nào.
     */
    fun onState(state: EndowState): List<EndowHostEvent> {
        val events = mutableListOf<EndowHostEvent>()

        val current = state.widgetState
        if (current == EndowWidgetState.APPLIED && lastWidgetState != EndowWidgetState.APPLIED) {
            // Không có id thì KHÔNG bắn — nhưng vẫn ghi nhận đã sang APPLIED ở dưới, nếu không lần
            // render kế (danh sách đã có id) sẽ bắn lại và host nhận hai lần cho một lần áp.
            state.appliedDiscounts.firstOrNull()?.objectId?.let {
                events += EndowHostEvent.VoucherApplied(it)
            }
        }
        lastWidgetState = current

        return events
    }

}
