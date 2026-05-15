package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import android.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.promotionsdk.databinding.FragmentMyPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.allAvailableVouchers
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.searchmypromotion.SearchMyPromotionFragment
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.ChoosePromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.MyPromotionTabAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.PromotionListItem
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailFragment

class MyPromotionFragment : PRMBaseFragment<FragmentMyPromotionBinding>() {

    private val viewModel: MyPromotionViewModel by viewModels()

    private val tabAdapter = MyPromotionTabAdapter(
        titles = listOf("Tất cả", "Sắp hết hạn"),
        onTabSelected = { },
    )

    private val homeListAdapter = ChoosePromotionAdapter(
        onVoucherClick = { voucher, _ ->
            addFragment(PromotionDetailFragment.newInstance(voucher))
        },
        onUseClick = { _, _ ->
        },
    )

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.btnBack.setOnClickListener { onBackFragment() }

        binding.imgSearch.setOnClickListener { openSearchMyPromotion() }

        binding.rvTabs.adapter = tabAdapter

        binding.homeList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeListAdapter
        }

        homeListAdapter.submitList(
            allAvailableVouchers.map { PromotionListItem.Endow(it) })
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { }
        collectFlow(viewModel.uiEffect) { }
    }

    private fun openSearchMyPromotion() {
        val fm = requireActivity().supportFragmentManager
        if (fm.findFragmentByTag(TAG_SEARCH_MY_PROMOTION) != null) return
        fm.beginTransaction().setReorderingAllowed(true)
            .add(R.id.content, SearchMyPromotionFragment(), TAG_SEARCH_MY_PROMOTION)
            .addToBackStack(TAG_SEARCH_MY_PROMOTION).commit()
    }

    private companion object {
        private const val TAG_SEARCH_MY_PROMOTION = "prm_search_my_promotion"
    }
}