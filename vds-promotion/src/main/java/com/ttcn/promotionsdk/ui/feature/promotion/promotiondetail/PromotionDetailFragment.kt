package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.databinding.FragmentDetailPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.TabLayoutThemeApplier
import com.ttcn.promotionsdk.ui.utils.extension.toVoucherDisplayDate
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherBanner
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherLogo

class PromotionDetailFragment : PRMBaseFragment<FragmentDetailPromotionBinding>() {

    private val viewModelFactory by inject<PromotionViewModelFactory>()

    private val viewModel: PromotionDetailViewModel by viewModels {
        viewModelFactory
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDetailPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener { onBackFragment() }
        val voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty()
        if (voucherId.isBlank()) {
            showToast(getString(R.string.no_result))
            return
        }
        viewModel.handleAction(PromotionDetailAction.LoadDetail(voucherId))
        TabLayoutThemeApplier.apply(binding.tabs, PromotionThemeRegistry.tabUnderlineToken())
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { state ->
            val detail = state.detail
            if (detail != null) {
                binding.imgBanner.loadPromotionVoucherBanner(
                    detail.banner.orEmpty(),
                    preferCache = false
                )
                binding.circleLogo.background = null
                binding.circleLogo.loadPromotionVoucherLogo(
                    detail.logo.orEmpty(),
                    preferCache = true
                )
                binding.txtVoucherName.text = detail.merchantName.orEmpty()
                binding.tvExpired.text = getString(
                    R.string.prm_expiry_short_format,
                    detail.expirationDate.orEmpty().toVoucherDisplayDate(),
                )
                binding.tvUse.isVisible = state.actionVisible
                binding.tvUse.isEnabled = state.actionEnabled
                binding.tvUse.text = state.actionLabel.ifBlank {
                    if (state.status == VoucherStatus.ACTIVE) getString(R.string.use_now)
                    else detail.displayStatusLabel.orEmpty()
                }
                binding.tvContent.text = resolveDetailContent(detail)
            }
        }
        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is PromotionDetailEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
            }
        }
    }

    companion object {
        private const val KEY_VOUCHER_ID = "prm_promotion_detail_voucher_id"

        fun newInstance(voucherId: String): PromotionDetailFragment {
            return PromotionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_VOUCHER_ID, voucherId)
                }
            }
        }
    }

    private fun resolveDetailContent(detail: CustomerVoucherDetail): CharSequence {
        val content = detail.title?.takeIf { it.isNotBlank() }
            ?: detail.description.orEmpty()
        return content.toHtmlText()
    }

    private fun String.toHtmlText(): CharSequence {
        val spanned: Spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)
        return spanned.toString().ifBlank { this }
    }

    private fun mapErrorMessage(error: String): String {
        return when (error) {
            "missing_customer_id" -> getString(R.string.prm_missing_customer_id)
            "error_detail_unavailable" -> getString(R.string.no_result)
            else -> getString(R.string.prm_error_general)
        }
    }
}