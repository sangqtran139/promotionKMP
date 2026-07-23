package com.ttcn.promotionsdk.app

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.prm.R
import com.ttcn.prm.databinding.FragmentPaymentDemoBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.prm.ui.feature.promotion.PromotionIntegrateManager
import com.ttcn.prm.ui.feature.promotion.choosepromotion.ChoosePromotionFragment

class DemoPaymentIntegrateFragment : PRMBaseFragment<FragmentPaymentDemoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPaymentDemoBinding.inflate(inflater, container, false)

    // SDK Manager — đối tác khởi tạo 1 lần, truyền endowView vào
    private lateinit var promotionIntegrateManager: PromotionIntegrateManager

    override fun setupUI() {
        promotionIntegrateManager = PromotionIntegrateManager.create(binding.endowView)

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
        addFragment(ChoosePromotionFragment.forEndowView(binding.endowView))
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