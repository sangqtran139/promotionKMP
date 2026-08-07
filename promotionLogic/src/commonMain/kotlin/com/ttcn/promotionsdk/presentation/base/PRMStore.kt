package com.ttcn.promotionsdk.presentation.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * Bộ ba **State / Intent / Effect** của MVI, phần dùng chung Android & iOS.
 *
 * Năm store (`MyPromotion`, `ChoosePromotion`, `SearchMyPromotion`, `PromotionDetail`, `Endow`) vốn
 * đã cùng khuôn — interface này chỉ **ghi thành lời** cái khuôn đó để tầng native bọc chúng bằng
 * **một** lớp chung thay vì mỗi màn một bản sao.
 *
 * ```
 * View  --dispatch(intent)-->  Store  --state-->   View   (nguồn sự thật, phát lại được)
 *                                   --effects-->  View   (sự kiện MỘT LẦN: lỗi…)
 * ```
 *
 * **Đóng ở bốn thành viên.** Đừng kéo `refresh()` / `loadMore()` lên đây dù phần lớn store đều có:
 * mọi hành vi đi qua [dispatch] với `Intent` sealed riêng của từng store, nên interface không có lý
 * do phình. Nới nó ra là quay lại đúng thứ base class cũ đã mắc.
 */
interface PRMStore<S : Any, I : Any> {

    /** Nguồn sự thật của màn. Phát lại được — collect lúc nào cũng có giá trị hiện tại. */
    val state: StateFlow<S>

    /** Đưa một **Intent** (ý định của user) vào store; store xử lý rồi cập nhật [state]. */
    fun dispatch(intent: I)

    /** Mã lỗi đang chờ báo trong [state]; `null` = không có. */
    fun errorOf(state: S): String?

    /** Intent xoá lỗi sau khi đã báo — cả năm store đều có `ConsumeError`. */
    val consumeErrorIntent: I

    /**
     * Sự kiện **một lần**, đối lập với [state] là thứ phát lại được.
     *
     * `errorCode` nằm trong [state] là *trạng thái*; hiển thị nó thẳng thì mỗi lần state phát lại
     * (xoay màn, collect lại) là toast bắn lần nữa. Ở đây mỗi mã đi ra **một** lần rồi tự [dispatch]
     * [consumeErrorIntent] để xoá khỏi state.
     *
     * Chỉ tiêu thụ khi có người collect: màn đang `STOPPED` thì lỗi **nằm lại** trong state và được
     * báo lúc màn sống lại, thay vì bắn vào hư không.
     *
     * [PRMEffect] là `sealed` nên màn **không** tự thêm nhánh riêng được — cố ý: effect ở đây là
     * phần dùng chung mọi màn. Màn nào cần một one-shot của riêng nó thì tự phơi một `Flow` trong
     * ViewModel của mình, đừng nới sealed này ra.
     */
    val effects: Flow<PRMEffect>
        get() = state
            .mapNotNull { errorOf(it) }
            .onEach { dispatch(consumeErrorIntent) }
            .map { PRMEffect.ShowError(it) }
}

/** Sự kiện một lần dùng chung mọi màn — xem [PRMStore.effects]. */
sealed interface PRMEffect {
    /** Mã lỗi thô; tầng native map sang chuỗi hiển thị (SDK không giữ chuỗi tiếng Việt ở lõi). */
    data class ShowError(val errorCode: String) : PRMEffect
}
