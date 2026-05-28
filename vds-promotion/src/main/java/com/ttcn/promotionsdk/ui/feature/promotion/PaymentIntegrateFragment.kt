package com.ttcn.promotionsdk.ui.feature.promotion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.databinding.FragmentPaymentDemoBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.PromotionItem
import com.ttcn.promotionsdk.ui.utils.extension.parcelableArrayList

/**
 * Màn chính - Quản lý list voucher và apply
 * List voucher gốc nằm ở đây
 */
class PaymentIntegrateFragment : PRMBaseFragment<FragmentPaymentDemoBinding>() {

    companion object {
        private const val KEY_APPLIED_VOUCHERS = "applied_vouchers"

        fun newInstance(appliedVouchers: List<PromotionItem>? = null): PaymentIntegrateFragment {
            return PaymentIntegrateFragment().apply {
                arguments = Bundle().apply {
                    appliedVouchers?.let {
                        putParcelableArrayList(KEY_APPLIED_VOUCHERS, ArrayList(it))
                    }
                }
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPaymentDemoBinding.inflate(inflater, container, false)

    /**
     * SINGLE SOURCE OF TRUTH - List voucher gốc chỉ có ở đây
     */
    private val allAvailableVouchers =
        com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.allAvailableVouchers

    /**
     * Danh sách voucher đã được apply
     */
    private var currentAppliedVouchers: List<PromotionItem> = emptyList()

    override fun setupUI() {
        currentAppliedVouchers =
            arguments?.parcelableArrayList<PromotionItem>(KEY_APPLIED_VOUCHERS)
                ?: arrayListOf()
        setupEndowView()
    }

    private fun setupEndowView() {
        binding.endowView.apply {

            // Cập nhật trạng thái isApplied cho vouchers
            updateVouchersState()

            // Khi bấm "Sử dụng" (lần đầu)
            setOnUseVoucherClickListener {
                openVoucherSelectionScreen()
            }

            // Khi bấm "Chọn lại" (đã có voucher)
            setOnChangeVoucherClickListener {
                openVoucherSelectionScreen()
            }

            // Click vào voucher item trong RecyclerView
            setOnVoucherItemClickListener { voucher ->
                showToast("Clicked: ${voucher.name}")
            }
        }
    }

    /**
     * Cập nhật trạng thái và hiển thị vouchers
     */
    private fun updateVouchersState() {
        val mergedVouchers = allAvailableVouchers.map { voucher ->
            val isApplied = currentAppliedVouchers.any { it.id == voucher.id }
            voucher.copy(isApplied = isApplied)
        }
        binding.endowView.setVouchers(mergedVouchers)
    }

    /**
     * Mở màn chọn voucher
     * Truyền toàn bộ list voucher + list đã apply
     */
    private fun openVoucherSelectionScreen() {
        addFragment(
            ChoosePromotionFragment()
        )
    }

    /**
     * Xử lý khi user apply voucher từ MyEndowFragment
     */
    private fun handleVoucherApplied(selectedVouchers: List<PromotionItem>) {
        // Cập nhật vouchers đã apply
        currentAppliedVouchers = selectedVouchers

        // Cập nhật UI
        updateVouchersState()

        // Back về màn hiện tại
        onBackFragment()
    }
}