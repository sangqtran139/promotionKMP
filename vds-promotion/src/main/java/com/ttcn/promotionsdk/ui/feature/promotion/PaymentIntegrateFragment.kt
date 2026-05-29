package com.ttcn.promotionsdk.ui.feature.promotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.databinding.FragmentPaymentDemoBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionFragment

class PaymentIntegrateFragment : PRMBaseFragment<FragmentPaymentDemoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPaymentDemoBinding.inflate(inflater, container, false)

    private val viewModelFactory by inject<PromotionViewModelFactory>()
    private val viewModel: PaymentIntegrateViewModel by viewModels { viewModelFactory }

    override fun setupUI() {
        setupEndowView()
    }

    override fun observeData() {
        super.observeData()

        collectFlow(viewModel.uiState) { state ->
            if (state.appliedVouchers.isEmpty()) {
                binding.endowView.setVoucherCount(state.totalVoucherCount)
            } else {
                binding.endowView.setAppliedVouchers(state.appliedVouchers)
            }
        }

        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is PaymentIntegrateEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
            }
        }

        viewModel.handleAction(PaymentIntegrateAction.LoadInitialIfNeeded)
    }

    private fun setupEndowView() {
        binding.endowView.apply {
            setOnUseVoucherClickListener { openVoucherSelectionScreen() }
            setOnChangeVoucherClickListener { openVoucherSelectionScreen() }
            setOnVoucherItemClickListener { voucher -> showToast("Clicked: ${voucher.title}") }
        }
    }

    private fun openVoucherSelectionScreen() {
        val state = viewModel.uiState.value
        val fragment = ChoosePromotionFragment().apply {
            initialMyVouchers = state.myVouchers
            initialOtherVouchers = state.otherVouchers
            selectedVouchers = state.appliedVouchers
            onApplyVoucher = { selected ->
                viewModel.handleAction(PaymentIntegrateAction.ApplyVouchers(selected))
            }
        }
        addFragment(fragment)
    }

    private fun mapErrorMessage(error: String): String {
        return when (error) {
            "missing_customer_id" -> getString(com.ttcn.promotionsdk.R.string.prm_missing_customer_id)
            else -> getString(com.ttcn.promotionsdk.R.string.prm_error_general)
        }
    }
}