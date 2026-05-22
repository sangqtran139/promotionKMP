package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.FragmentDetailPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.PromotionItem
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.TabLayoutThemeApplier
import com.ttcn.promotionsdk.ui.utils.extension.parcelable
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherBanner
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherLogo

class PromotionDetailFragment : PRMBaseFragment<FragmentDetailPromotionBinding>() {

    private val viewModel: PromotionDetailViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDetailPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener { onBackFragment() }
        arguments?.parcelable<PromotionItem>(KEY_VOUCHER)?.let { voucher ->
            binding.apply {
                imgBanner.loadPromotionVoucherBanner(voucher.urlBanner, preferCache = false)
                circleLogo.background = null
                circleLogo.loadPromotionVoucherLogo(voucher.urlLogo, preferCache = true)
                txtVoucherName.text = voucher.name
                tvContent.text = getString(R.string.prm_discount_amount_format, voucher.discount)
                tvExpired.text = getString(
                    R.string.prm_expiry_short_format,
                    getString(R.string.prm_demo_expiry_date),
                )
                tvUse.isVisible = !voucher.isExpired
            }
        }
        TabLayoutThemeApplier.apply(binding.tabs, PromotionThemeRegistry.tabUnderlineToken())
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { }
        collectFlow(viewModel.uiEffect) { }
    }

    companion object {
        private const val KEY_VOUCHER = "prm_promotion_detail_voucher"

        fun newInstance(voucher: PromotionItem): PromotionDetailFragment {
            return PromotionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_VOUCHER, voucher)
                }
            }
        }
    }
}