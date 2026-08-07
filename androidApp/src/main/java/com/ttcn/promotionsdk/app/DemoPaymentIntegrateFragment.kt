package com.ttcn.promotionsdk.app

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.app.base.AppBaseFragment
import com.ttcn.promotionsdk.app.databinding.FragmentPaymentDemoBinding
import com.ttcn.prm.entry.PromotionSDK

/**
 * Màn thanh toán mẫu — tất cả những gì màn này chạm vào SDK đều nằm ở `com.ttcn.prm.entry`:
 * widget `PRMEndowView` (đặt trong layout của host) và `PromotionSDK.createChoosePromotionFragment`.
 * Không có lớp nội bộ nào của SDK ở đây.
 */
class DemoPaymentIntegrateFragment : AppBaseFragment<FragmentPaymentDemoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPaymentDemoBinding.inflate(inflater, container, false)

    override fun setupUI() {
        // ─── Wire endowView callbacks ─────────────────────────────────────────
        binding.endowView.apply {
            onOpenVoucherSelection = { openVoucherSelectionScreen() }
            onError = { errorCode -> showToast(mapErrorMessage(errorCode)) }
        }

        // ─── Confirm thanh toán ───────────────────────────────────────────────
        // Gọi thẳng trên widget: không còn class manager riêng, cũng không còn `clear()` phải nhớ.
        binding.btnConfirmPayment.setOnClickListener {
            binding.endowView.confirmRedemption(
                onSuccess = { proceedPayment() },
                onError = { errorCode -> showToast(mapErrorMessage(errorCode)) },
            )
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    /** SDK trả về [androidx.fragment.app.Fragment] trần — màn "Chọn ưu đãi" là UI nội bộ của SDK. */
    private fun openVoucherSelectionScreen() {
        addFragment(PromotionSDK.createChoosePromotionFragment(binding.endowView))
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
