package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import android.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.databinding.FragmentMyPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.searchmypromotion.SearchMyPromotionFragment

class MyPromotionFragment : PRMBaseFragment<FragmentMyPromotionBinding>() {

    private val viewModel: MyPromotionViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.btnBack.setOnClickListener { onBackFragment() }
        binding.imgSearch.setOnClickListener { openSearchMyPromotion() }
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { /* sync list / loading from state */ }
        collectFlow(viewModel.uiEffect) { /* one-shot: toast, navigation */ }
    }

    private fun openSearchMyPromotion() {
        val fm = requireActivity().supportFragmentManager
        if (fm.findFragmentByTag(TAG_SEARCH_MY_PROMOTION) != null) return
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.content, SearchMyPromotionFragment(), TAG_SEARCH_MY_PROMOTION)
            .addToBackStack(TAG_SEARCH_MY_PROMOTION)
            .commit()
    }

    private companion object {
        private const val TAG_SEARCH_MY_PROMOTION = "prm_search_my_promotion"
    }
}