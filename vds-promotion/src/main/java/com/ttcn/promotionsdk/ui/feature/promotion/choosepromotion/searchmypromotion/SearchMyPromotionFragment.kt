package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.searchmypromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.databinding.FragmentSearchMyPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment

class SearchMyPromotionFragment : PRMBaseFragment<FragmentSearchMyPromotionBinding>() {

    private val viewModel: SearchMyPromotionViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener { onBackFragment() }
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { }
        collectFlow(viewModel.uiEffect) {}
    }
}