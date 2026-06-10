package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.model.VoucherDetail
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.databinding.FragmentDetailPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.adapter.PrmCustomFragmentPagerAdapter
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

    private var tabMediator: TabLayoutMediator? = null
    private var pagerBoundVoucherId: String? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDetailPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener { onBackFragment() }
        val voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty()
        if (voucherId.isBlank()) {
            showToast(getString(R.string.no_result))
            return
        }
        showDetailLoading()
        viewModel.handleAction(PromotionDetailAction.LoadDetail(voucherId))
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { state ->
            val detail = state.detail

            if (state.isLoading && detail == null) {
                showDetailLoading()
                return@collectFlow
            }

            if (detail != null) {
                bindDetailContent(state, detail)
                hideDetailLoading()
                return@collectFlow
            }

            hideDetailLoading(showContent = false)
        }
        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is PromotionDetailEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
            }
        }
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        tabMediator = null
        pagerBoundVoucherId = null
        super.onDestroyView()
    }

    private fun showDetailLoading() {
        binding.progressLoading.isVisible = true
        binding.contentContainer.isVisible = false
    }

    private fun hideDetailLoading(showContent: Boolean = true) {
        binding.progressLoading.isVisible = false
        binding.contentContainer.isVisible = showContent
    }

    private fun bindDetailContent(
        state: PromotionDetailUiState,
        detail: VoucherDetail,
    ) {
        binding.imgBanner.loadPromotionVoucherBanner(
            detail.banner.orEmpty(),
            preferCache = false,
        )
        binding.circleLogo.background = null
        binding.circleLogo.loadPromotionVoucherLogo(
            detail.logo.orEmpty(),
            preferCache = true,
        )
        binding.txtVoucherName.text = detail.merchantName.orEmpty()
        binding.tvContent.text = detail.title.orEmpty()
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

        bindDetailTabsIfNeeded(
            voucherId = detail.voucherId,
            descriptionHtml = resolveHtmlContent(
                detail.description,
                R.string.prm_empty_detail_info,
            ),
            guidelineHtml = resolveHtmlContent(
                detail.guideline,
                R.string.prm_empty_usage_guide,
            ),
        )
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

    private fun bindDetailTabsIfNeeded(
        voucherId: String,
        descriptionHtml: String,
        guidelineHtml: String,
    ) {
        if (pagerBoundVoucherId == voucherId && binding.viewPager.adapter != null) return
        pagerBoundVoucherId = voucherId
        tabMediator?.detach()

        val adapter = PrmCustomFragmentPagerAdapter(requireActivity())
        adapter.addFragment(
            PrmContentDetailEndowFragment.newInstance(descriptionHtml),
            getString(R.string.prm_tab_detail_info),
        )
        adapter.addFragment(
            PrmContentDetailEndowFragment.newInstance(guidelineHtml),
            getString(R.string.prm_tab_usage_guide),
        )
        binding.viewPager.adapter = adapter
        tabMediator = TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = adapter.getTitle(position)
        }.also { it.attach() }

        binding.tabs.tabMode = TabLayout.MODE_FIXED
        binding.tabs.tabGravity = TabLayout.GRAVITY_FILL
        binding.tabs.isTabIndicatorFullWidth = true
        applyTabUnderlineTheme()
    }

    private fun applyTabUnderlineTheme() {
        TabLayoutThemeApplier.apply(binding.tabs, PromotionThemeRegistry.tabUnderlineToken())
    }

    private fun resolveHtmlContent(html: String?, @StringRes emptyRes: Int): String {
        return html?.takeIf { it.isNotBlank() }
            ?: "<p>${getString(emptyRes)}</p>"
    }

    private fun mapErrorMessage(error: String): String {
        return when (error) {
            ErrorCodes.MISSING_CUSTOMER_ID -> getString(R.string.prm_missing_customer_id)
            "error_detail_unavailable" -> getString(R.string.no_result)
            else -> getString(R.string.prm_error_general)
        }
    }
}
