package com.ttcn.prm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.sign

/**
 * Bọc quanh 1 view con cuộn ngang được (ở đây là `WebView` chứa ảnh rộng hơn màn hình) khi view đó
 * nằm trong `ViewPager2` (`PromotionDetailFragment.viewPager`, 2 tab "Thông tin chi tiết"/"Hướng dẫn
 * sử dụng"). Không có lớp này, `RecyclerView` nội bộ của `ViewPager2` intercept mọi vuốt ngang ngay
 * từ đầu (nó cũng là view cuộn ngang, ăn gesture trước `WebView`) → vuốt để xem hết ảnh bị nuốt giữa
 * chừng thành chuyển tab, dù ảnh chưa cuộn hết. Chỉ xảy ra ở Android; iOS không dùng cơ chế intercept
 * kiểu này nên không dính.
 *
 * Cách chặn: tại `ACTION_DOWN`, khoá `ViewPager2` không cho intercept
 * (`requestDisallowInterceptTouchEvent(true)`) — lời gọi này tự bubble lên hết chuỗi ancestor nên
 * host này không cần là con trực tiếp của `ViewPager2`. Từ `ACTION_MOVE`, nếu gesture là vuốt NGANG
 * và `WebView` con vẫn còn `canScrollHorizontally(direction)` theo đúng hướng vuốt → tiếp tục khoá,
 * để `WebView` tự cuộn. Khi ảnh đã cuộn hết cỡ (hết đường cuộn theo hướng đó) → mở khoá
 * (`requestDisallowInterceptTouchEvent(false)`), `ViewPager2` mới được nhận lại gesture để chuyển
 * tab — đúng yêu cầu "kéo hết ảnh mới sang tab kế". Chuẩn NestedScrollableHost của Google cho
 * ViewPager2 (RecyclerView ngang lồng trong ViewPager2 ngang), áp dụng lại cho `WebView`.
 */
internal class PRMNestedScrollableWebViewHost @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet? = null, defStyleInt: Int = 0
) : FrameLayout(context, attributeSet, defStyleInt) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialX = 0f
    private var initialY = 0f

    private val child get() = getChildAt(0)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        handleInterceptTouchEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }

    private fun handleInterceptTouchEvent(ev: MotionEvent) {
        // Con không cuộn ngang được theo hướng nào cả (vd ảnh vừa khít màn hình) -> khỏi can thiệp,
        // để ViewPager2 xử lý vuốt như bình thường.
        if (!canChildScrollHorizontally(-1) && !canChildScrollHorizontally(1)) return

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - initialX
                val dy = ev.y - initialY
                if (abs(dx) < touchSlop && abs(dy) < touchSlop) return

                if (abs(dx) > abs(dy)) {
                    // Vuốt ngang, cùng trục với ViewPager2: chỉ nhường quyền intercept cho ViewPager2
                    // khi con hết đường cuộn tiếp theo đúng hướng đang vuốt.
                    val direction = -dx.sign.toInt()
                    val childCanScroll = child?.canScrollHorizontally(direction) ?: false
                    parent?.requestDisallowInterceptTouchEvent(childCanScroll)
                } else {
                    // Vuốt dọc: không phải gesture chuyển tab, để nguyên trạng thái khoá hiện tại
                    // (đang giữ từ ACTION_DOWN) tránh ViewPager2 giật lấy giữa chừng.
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
    }

    private fun canChildScrollHorizontally(direction: Int): Boolean =
        child?.canScrollHorizontally(direction) ?: false
}
