package com.ttcn.promotionsdk.presentation.endow

/**
 * Sự kiện SDK báo ra host — **một lần mỗi lần đổi**, không phải state.
 *
 * Đối ứng 1-1 với `PromotionSDKCallback` của hai nền tảng (`onVoucherApplied` /
 * `onVoucherCountChanged` / `onAvailabilityChanged`). Native chỉ việc map sang callback tương ứng,
 * không tự quyết định lúc nào nên bắn.
 */
sealed interface EndowHostEvent {
    /** Widget vừa chuyển sang trạng thái ĐÃ ÁP. Mang id ưu đãi đầu tiên. */
    data class VoucherApplied(val voucherId: String) : EndowHostEvent

    /** Tổng số ưu đãi khả dụng đổi — host cập nhật badge/nhãn của mình. */
    data class VoucherCountChanged(val count: Int) : EndowHostEvent

    /** Widget bật/tắt theo feature flag — host thu gọn hoặc chừa chỗ cho nó. */
    data class AvailabilityChanged(val enabled: Boolean) : EndowHostEvent
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

    /** `-1` để lần đầu luôn khác `count` thật (kể cả khi count = 0). */
    private var lastCount: Int = -1

    /** `null` = chưa báo lần nào, nên lần áp cờ đầu tiên luôn bắn — kể cả khi cờ đang bật. */
    private var lastAvailability: Boolean? = null

    /**
     * Trả danh sách sự kiện phải phát cho [state] này, theo đúng thứ tự cần bắn.
     *
     * **Count trước, applied sau** — chốt theo thứ tự cũ của Android. iOS trước đây bắn ngược lại
     * (applied trong `switch`, count sau đó); host nào đang dựa vào việc nhận `onVoucherApplied`
     * trước `onVoucherCountChanged` sẽ thấy đổi. Chọn thứ tự này vì host thường cập nhật tổng tiền
     * theo count rồi mới xử lý voucher vừa áp.
     *
     * Gọi cả khi `hasLoadedInitial` còn false cũng an toàn: lúc đó `widgetState` là EMPTY và
     * `totalVoucherCount` là 0, không sinh sự kiện nào ngoài lần count đầu tiên.
     */
    fun onState(state: EndowState): List<EndowHostEvent> {
        val events = mutableListOf<EndowHostEvent>()

        if (state.totalVoucherCount != lastCount) {
            lastCount = state.totalVoucherCount
            events += EndowHostEvent.VoucherCountChanged(state.totalVoucherCount)
        }

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

    /**
     * Feature flag vừa được áp (fail-open: gọi ngay bằng cache, rồi gọi lại sau khi làm mới từ
     * server). Chỉ bắn khi **đổi**, để hai lời gọi đó không sinh callback trùng.
     */
    fun onAvailability(enabled: Boolean): List<EndowHostEvent> =
        if (lastAvailability == enabled) {
            emptyList()
        } else {
            lastAvailability = enabled
            listOf(EndowHostEvent.AvailabilityChanged(enabled))
        }
}
