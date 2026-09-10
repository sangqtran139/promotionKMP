package com.ttcn.promotionsdk.app

import com.ttcn.prm.entry.api.PromotionSDKError
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.app.base.AppBaseFragment
import com.ttcn.promotionsdk.app.databinding.FragmentPaymentDemoBinding

/**
 * Màn thanh toán mẫu — tất cả những gì màn này chạm vào SDK đều nằm ở `com.ttcn.prm.entry`:
 * widget `PRMOfferWidget` (đặt trong layout của host). Widget tự điều hướng sang màn "Chọn ưu đãi"
 * khi user bấm — host không cần wiring gì cho việc đó. Không có lớp nội bộ nào của SDK ở đây.
 */
class DemoPaymentIntegrateFragment : AppBaseFragment<FragmentPaymentDemoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPaymentDemoBinding.inflate(inflater, container, false)

    override fun setupUI() {
        // ─── Wire offerWidget callbacks ─────────────────────────────────────────
        binding.offerWidget.onError = { error -> handleSdkError(error) }

        // ─── Confirm thanh toán ───────────────────────────────────────────────
        // Gọi thẳng trên widget: không còn class manager riêng, cũng không còn `clear()` phải nhớ.
        binding.btnConfirmPayment.setOnClickListener {
            binding.offerWidget.confirmRedemption(
                onSuccess = { proceedPayment() },
                onError = { error -> handleSdkError(error) },
            )
        }
    }

    /**
     * Mẫu xử lý lỗi từ widget: **đọc qua `PromotionSDKError.from(code)`**, không so chuỗi.
     *
     * `onError` trả thẳng `PromotionSDKError` (kiểu công khai), host `when` là xong — không phải so
     * chuỗi mã lỗi.
     *
     * Riêng `FeatureDisabled`: **SDK đã tự hiện popup**, host chỉ cần dừng luồng — hiện thêm thông
     * báo của mình là user đọc hai lần cho cùng một chuyện.
     */
    private fun handleSdkError(error: PromotionSDKError) {
        when (error) {
            is PromotionSDKError.FeatureDisabled -> Unit
            else -> showToast(error.message)
        }
    }

    // ─── Payment ──────────────────────────────────────────────────────────────

    private fun proceedPayment() {
        showToast(getString(R.string.demo_payment_done))
    }

    /** SDK chỉ trả **mã lỗi**; câu hiển thị là chuỗi của host. */
    private fun mapErrorMessage(error: String): String = when (error) {
        "missing_customer_id" -> getString(R.string.demo_error_missing_customer)
        "INSUFFICIENT_BUDGET" -> getString(R.string.demo_error_budget_insufficient)
        else -> getString(R.string.demo_error_general)
    }
}
