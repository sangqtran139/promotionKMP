package com.ttcn.promotionsdk.ui.feature.promotion

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.databinding.FragmentPaymentDemoBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionFragment

class PaymentIntegrateFragment : PRMBaseFragment<FragmentPaymentDemoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPaymentDemoBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.endowView.apply {
            onOpenVoucherSelection = { openVoucherSelectionScreen() }
            onVoucherItemClick = { voucher -> showToast("Clicked: ${voucher.title}") }
            onError = { errorCode -> showToast(mapErrorMessage(errorCode)) }
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun openVoucherSelectionScreen() {
        val endowView = binding.endowView
        val fragment = ChoosePromotionFragment().apply {
            initialMyVouchers = endowView.myVouchers
            initialOtherVouchers = endowView.otherVouchers
            selectedVouchers = endowView.appliedVouchers
            onApplyVoucher = { selected -> endowView.setAppliedVouchers(selected) }
        }
        addFragment(fragment)
    }

    private fun mapErrorMessage(error: String): String = when (error) {
        "missing_customer_id" -> getString(com.ttcn.promotionsdk.R.string.prm_missing_customer_id)
        else -> getString(com.ttcn.promotionsdk.R.string.prm_error_general)
    }
}