package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.databinding.FragmentDetailPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.PromotionItem
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
            binding.imgBanner.loadPromotionVoucherBanner(voucher.urlBanner, preferCache = false)
            binding.circleLogo.background = null
            binding.circleLogo.loadPromotionVoucherLogo(voucher.urlLogo, preferCache = true)
            binding.txtVoucherName.text = voucher.name
            binding.tvContent.text = "Giảm giá ${voucher.discount}đ"
            binding.tvExpired.text = "HSD 15/05/2025"
        }
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