package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.databinding.FragmentDetailPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment

class PromotionDetailFragment : PRMBaseFragment<FragmentDetailPromotionBinding>() {

    private val viewModel: PromotionDetailViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDetailPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener { onBackFragment() }
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { }
        collectFlow(viewModel.uiEffect) { }
    }
}