package com.ttcn.prm.ui.base

import android.view.KeyEvent
import android.view.Window
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.ttcn.prm.ui.utils.PRMLog

/**
 * Chặn back **hệ thống** hộ một màn SDK. Tách khỏi [PRMBaseFragment] để base chỉ còn 3 dòng gọi vào
 * đây, thay vì trộn lẫn hai cơ chế của Android với chuyện điều hướng.
 *
 * Phải có **hai** đường, vì host xử lý back theo hai kiểu khác nhau:
 *
 * | Đường | Bắt được back khi | Gắn / gỡ |
 * |---|---|---|
 * | `OnBackPressedCallback` | host để back chảy qua `onBackPressedDispatcher` — gồm cả predictive back (Android 13+), thứ **không** sinh `KeyEvent` | [registerDispatcherCallback], tự gỡ theo `viewLifecycleOwner` |
 * | `Window.Callback` | host tự override `onBackPressed()` mà không gọi `super` → back không bao giờ tới dispatcher, chỉ còn ở `KeyEvent` (đã gặp thật khi tích hợp) | [wrapWindow] ở `onResume`, [unwrapWindow] ở `onPause` |
 *
 * Hai đường không giẫm chân nhau: đường nào bắt trước thì nuốt event, đường kia không thấy gì.
 *
 * @param handleBack `true` = SDK đã xử lý xong, **nuốt** event. `false` = SDK không có gì để làm,
 * event đi tiếp tới host/NavController **như chưa hề bị chặn**. Đừng đổi thành luôn `true`: wrapper
 * gắn trên window của Activity nên nó sẽ nuốt sạch phím back của host.
 */
internal class PrmSystemBackInterceptor(private val handleBack: () -> Boolean) {

    /** Wrapper mà **chính interceptor này** đã gắn — để [unwrapWindow] gỡ đúng cái của mình. */
    private var installedWindowCallback: BackKeyWindowCallback? = null

    /** Gọi ở `onViewCreated`. Callback sống theo `viewLifecycleOwner` nên không cần gỡ tay. */
    fun registerDispatcherCallback(fragment: Fragment) {
        val screen = fragment::class.java.simpleName
        val dispatcher = fragment.requireActivity().onBackPressedDispatcher
        dispatcher.addCallback(fragment.viewLifecycleOwner) {
            if (handleBack()) {
                PRMLog.d(TAG, "[$screen] back qua dispatcher -> SDK đóng 1 màn")
                return@addCallback
            }
            // SDK hết màn để đóng -> nhường host. Phải tự tắt TRƯỚC khi gọi lại dispatcher, nếu không
            // nó chọn đúng callback này (vẫn đang enabled) -> đệ quy vô hạn. Bật lại ngay sau đó:
            // màn SDK vẫn còn sống thì vẫn phải chặn lần back kế tiếp.
            PRMLog.d(TAG, "[$screen] back qua dispatcher -> SDK không còn màn, nhường host")
            isEnabled = false
            dispatcher.onBackPressed()
            if (fragment.isAdded) isEnabled = true
        }
    }

    /**
     * Gọi ở `onResume`.
     *
     * ⚠️ Nhiều màn SDK cùng resumed (host `add()` mà không `hide()`): màn thứ hai thấy
     * `window.callback` đã là [BackKeyWindowCallback] nên bỏ qua, không wrap chồng. Phím back lúc đó
     * chạy [handleBack] của màn SDK **đầu tiên** trong stack, bất kể màn nào đang hiện trên cùng —
     * nên [handleBack] chỉ được thao tác qua `FragmentManager` dùng chung, không đụng state riêng
     * của một fragment.
     */
    fun wrapWindow(window: Window) {
        val current = window.callback
        if (current is BackKeyWindowCallback) return
        val wrapper = BackKeyWindowCallback(current, handleBack)
        installedWindowCallback = wrapper
        window.callback = wrapper
    }

    /**
     * Gọi ở `onPause`. Chỉ khôi phục khi `window.callback` **vẫn đúng là instance mình đã gắn**
     * (so bằng `===`): thư viện khác có thể đã thay nó sau lúc mình wrap, đụng vào là hỏng của họ.
     */
    fun unwrapWindow(window: Window?) {
        val wrapper = installedWindowCallback
        if (wrapper != null && window != null && window.callback === wrapper) {
            window.callback = wrapper.delegate
        }
        installedWindowCallback = null
    }

    /**
     * Chỉ override `dispatchKeyEvent` — điểm đầu tiên mọi `KeyEvent` của Window đi qua, nên back bị
     * bắt trước cả `Activity.dispatchKeyEvent()` / `onBackPressed()` của host. Phần còn lại delegate
     * nguyên vẹn.
     */
    private class BackKeyWindowCallback(
        val delegate: Window.Callback,
        private val handleBack: () -> Boolean,
    ) : Window.Callback by delegate {

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            val isBackKeyUp =
                event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
            if (isBackKeyUp && handleBack()) return true
            return delegate.dispatchKeyEvent(event)
        }
    }

    private companion object {
        /** Lọc log lúc tích hợp host thật: `adb logcat -s PRMSystemBack`. */
        const val TAG = "PRMSystemBack"
    }
}
