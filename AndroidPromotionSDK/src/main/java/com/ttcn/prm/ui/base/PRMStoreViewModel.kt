package com.ttcn.prm.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.promotionsdk.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.presentation.base.PRMEffect
import com.ttcn.promotionsdk.presentation.base.PRMStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach

/**
 * Lớp bọc **duy nhất** quanh store dùng chung ([PRMStore] ở `promotionLogic`).
 *
 * Nó tồn tại vì hai thứ store không tự có, chứ không phải để thêm một tầng kiến trúc:
 *  1. **Sống qua xoay màn** — store là object thường; đặt trong Fragment là xoay màn mất state và
 *     gọi lại API. `viewModelScope` cũng từ đây mà ra.
 *  2. **Lỗi thành sự kiện một lần** — [errors] (xem KDoc bên [PRMStore.errors]).
 *
 * Nghiệp vụ **không** đi qua đây: màn phát [dispatch] thẳng `Intent` của store và đọc thẳng
 * [state]. Trước kia mỗi màn có một `Action` + `UiState` sao chép gần như 1-1 `Intent`/`State` của
 * store, kèm `bindStore`/`render`/`handleError` chép y hệt ở bốn file — bỏ hết.
 *
 * **`abstract` + subclass ba dòng, KHÔNG phải một class generic dùng chung.** `by viewModels()` lấy
 * *tên class* làm khoá, mà `PRMStoreViewModel<A, B>` và `PRMStoreViewModel<C, D>` erase về **cùng**
 * một class → dùng chung một class là hai màn đè khoá nhau, trả nhầm instance và nổ
 * `ClassCastException` lúc chạy. `promotionViewModelFactory()` cũng đánh khoá initializer theo
 * `T::class` nên sẽ hỏng y vậy. Mỗi màn một class con giữ cả hai thứ đó đúng, và cho phần thuần
 * Android (mở bottom sheet "Chọn dịch vụ"…) một chỗ tử tế để ở.
 */
internal abstract class PRMStoreViewModel<S : Any, I : Any>(
    storeFactory: (CoroutineScope) -> PRMStore<S, I>,
) : ViewModel() {

    protected val store: PRMStore<S, I> = storeFactory(viewModelScope)

    val state: StateFlow<S> get() = store.state

    /**
     * Sự kiện **một lần** (lỗi…). View map `errorCode` → chuỗi hiển thị.
     *
     * `TOKEN_EXPIRED` (HTTP 401 — xem [PromotionErrorCodes.TOKEN_EXPIRED]) bắn thêm
     * `onExpireToken()` ra host tại đây, một lần cho cả bốn màn kế thừa lớp này, thay vì mỗi
     * Fragment tự bắt. Effect vẫn chảy tiếp xuống view như cũ (không nuốt), UI vẫn tự quyết có hiển
     * thị lỗi hay không.
     */
    val effects: Flow<PRMEffect> get() = store.effects
        .onEach { effect ->
            if (effect is PRMEffect.ShowError && effect.errorCode == PromotionErrorCodes.TOKEN_EXPIRED) {
                PromotionSDK.getCallback()?.onExpireToken()
            }
        }

    fun dispatch(intent: I) = store.dispatch(intent)
}
