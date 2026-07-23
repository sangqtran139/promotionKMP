package com.ttcn.promotionsdk.app

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.FragmentPaymentDemoBinding
import com.ttcn.promotionsdk.promotionsdkui.base.PRMBaseFragment
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.PRMIntegrateManager
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.choosepromotion.PRMChoosePromotionFragment

class DemoPaymentIntegrateFragment : PRMBaseFragment<FragmentPaymentDemoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPaymentDemoBinding.inflate(inflater, container, false)

    // SDK Manager — đối tác khởi tạo 1 lần, truyền endowView vào
    private lateinit var promotionIntegrateManager: PRMIntegrateManager

    override fun setupUI() {
        promotionIntegrateManager = PRMIntegrateManager.create(binding.endowView)

        // ─── Wire endowView callbacks ─────────────────────────────────────────
        binding.endowView.apply {
            onOpenVoucherSelection = { openVoucherSelectionScreen() }
            onVoucherItemClick = { voucher -> showToast("Clicked: ${voucher.title}") }
            onError = { errorCode -> showToast(mapErrorMessage(errorCode)) }
        }

        // ─── Confirm thanh toán ───────────────────────────────────────────────
        binding.btnConfirmPayment.setOnClickListener {
            promotionIntegrateManager.confirmRedemption(
                onSuccess = { proceedPayment() },
                onError = { errorCode -> showToast(mapErrorMessage(errorCode)) },
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        promotionIntegrateManager.clear()
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun openVoucherSelectionScreen() {
        addFragment(PRMChoosePromotionFragment.forEndowView(binding.endowView))
    }

    // ─── Payment ──────────────────────────────────────────────────────────────

    private fun proceedPayment() {
        showToast("Den buoc thanh toan")
    }

    private fun mapErrorMessage(error: String): String = when (error) {
        "missing_customer_id" -> getString(R.string.prm_missing_customer_id)
        "INSUFFICIENT_BUDGET" -> getString(R.string.prm_budget_insufficient)
        else -> getString(R.string.prm_error_general)
    }
}