package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.promotionsdk.databinding.PrmFragmentChooseEndowBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ChooseEndowAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.EndowListItem
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.PromotionVoucherItem
import com.ttcn.promotionsdk.ui.utils.extension.VerticalSpaceItemDecoration
import com.ttcn.promotionsdk.ui.utils.extension.parcelableArrayList
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn chọn voucher - Nhận list voucher từ parent
 * Không tự tạo data
 */
class ChooseEndowFragment : PRMBaseFragment<PrmFragmentChooseEndowBinding>() {

    companion object {
        private const val KEY_ALL_VOUCHERS = "all_vouchers"
        private const val KEY_CURRENT_APPLIED = "current_applied"

        fun newInstance(
            allVouchers: List<PromotionVoucherItem>,
            currentAppliedVouchers: List<PromotionVoucherItem>,
            onApplyVoucher: (List<PromotionVoucherItem>) -> Unit
        ): ChooseEndowFragment {
            return ChooseEndowFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(KEY_ALL_VOUCHERS, ArrayList(allVouchers))
                    putParcelableArrayList(KEY_CURRENT_APPLIED, ArrayList(currentAppliedVouchers))
                }
                this.onApplyVoucher = onApplyVoucher
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        PrmFragmentChooseEndowBinding.inflate(inflater, container, false)

    private var onApplyVoucher: ((List<PromotionVoucherItem>) -> Unit)? = null

    /**
     * false  -> chỉ chọn 1 voucher
     * true   -> chọn nhiều voucher
     */
    private val isMultiSelection = true

    /**
     * List voucher được truyền từ parent (SINGLE SOURCE OF TRUTH)
     */
    private var allVouchers: List<PromotionVoucherItem> = emptyList()

    /**
     * Voucher đã apply trước đó
     */
    private var previousAppliedVouchers: List<PromotionVoucherItem> = emptyList()

    /**
     * Voucher đang chọn hiện tại
     */
    private val currentSelectedVouchers = mutableListOf<PromotionVoucherItem>()

    /**
     * List voucher sau khi filter từ search, dùng để hiển thị lên UI
     */
    private var filteredVouchers: List<PromotionVoucherItem> = emptyList()

    private lateinit var voucherAdapter: ChooseEndowAdapter

    override fun setupUI() {
        // Lấy data từ arguments
        allVouchers =
            arguments?.parcelableArrayList<PromotionVoucherItem>(KEY_ALL_VOUCHERS) ?: arrayListOf()
        previousAppliedVouchers =
            arguments?.parcelableArrayList<PromotionVoucherItem>(KEY_CURRENT_APPLIED)
                ?: arrayListOf()
        filteredVouchers = allVouchers

        // Khởi tạo selection từ previous applied
        currentSelectedVouchers.clear()
        currentSelectedVouchers.addAll(previousAppliedVouchers)

        setupRecyclerView()
        setupButtons()
        setupSearch()
    }

    private fun setupRecyclerView() {
        voucherAdapter = ChooseEndowAdapter(
            onVoucherClick = { voucher, position ->
                handleVoucherSelection(voucher)
            },
            onDetailClick = { voucher, position ->
                showToast("Click detail at position $position - ${voucher.name}")
            })

        binding.rcvVoucher.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = voucherAdapter
            addItemDecoration(
                VerticalSpaceItemDecoration(
                    0,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
                )
            )
        }

        loadVouchers()
    }

    private fun loadVouchers(vouchers: List<PromotionVoucherItem> = filteredVouchers) {

        val items = mutableListOf<EndowListItem>()

        val myVouchers = vouchers.take(15)

        val otherVouchers = vouchers.drop(15)

        // Section 1
        if (myVouchers.isNotEmpty()) {

            items.add(
                EndowListItem.Header("Ưu đãi của tôi")
            )

            myVouchers.forEach { voucher ->

                val isSelected =
                    currentSelectedVouchers.any {
                        it.id == voucher.id
                    }

                items.add(
                    EndowListItem.Endow(
                        voucher.copy(
                            isApplied = isSelected
                        )
                    )
                )
            }
        }

        // Section 2
        if (otherVouchers.isNotEmpty()) {

            items.add(
                EndowListItem.Header("Ưu đãi khác")
            )

            otherVouchers.forEach { voucher ->

                val isSelected =
                    currentSelectedVouchers.any {
                        it.id == voucher.id
                    }

                items.add(
                    EndowListItem.Endow(
                        voucher.copy(
                            isApplied = isSelected
                        )
                    )
                )
            }
        }

        voucherAdapter.submitList(items)
    }

    private fun handleVoucherSelection(voucher: PromotionVoucherItem) {
        if (isMultiSelection) {
            handleMultiSelection(voucher)
        } else {
            handleSingleSelection(voucher)
        }

        // Reload lại list với trạng thái mới
        loadVouchers()
        updateApplyButtonState()
    }

    /**
     * Chỉ chọn 1 voucher
     */
    private fun handleSingleSelection(voucher: PromotionVoucherItem) {
        val isCurrentlySelected = currentSelectedVouchers.any { it.id == voucher.id }

        if (isCurrentlySelected) {
            // Click lại item đang chọn -> bỏ chọn
            currentSelectedVouchers.clear()
        } else {
            // Chọn item mới, bỏ chọn item cũ
            currentSelectedVouchers.clear()
            currentSelectedVouchers.add(voucher)
        }
    }

    /**
     * Cho phép chọn nhiều voucher
     */
    private fun handleMultiSelection(voucher: PromotionVoucherItem) {
        val isCurrentlySelected = currentSelectedVouchers.any { it.id == voucher.id }

        if (isCurrentlySelected) {
            // Bỏ chọn
            currentSelectedVouchers.removeAll { it.id == voucher.id }
        } else {
            // Thêm vào danh sách chọn
            currentSelectedVouchers.add(voucher)
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            onBackFragment()
        }

        binding.btnApply.setOnClickListener {
//            if (currentSelectedVouchers.isEmpty()) {
//                return@setOnClickListener
//            }

            // Trả về vouchers đã chọn với trạng thái isApplied = true
            val appliedVouchers = currentSelectedVouchers.map {
                it.copy(isApplied = true)
            }

            onApplyVoucher?.invoke(appliedVouchers)
        }

        updateApplyButtonState()
    }

    @SuppressLint("SetTextI18n")
    private fun updateApplyButtonState() {
        val hasSelectedVoucher = currentSelectedVouchers.isNotEmpty()

//        binding.btnApply.isEnabled = hasSelectedVoucher

        // Single select không hiển thị layout giảm giá
        if (!isMultiSelection) {
            binding.layoutReducePrice.isVisible = false
            return
        }

        binding.layoutReducePrice.isVisible = hasSelectedVoucher

        if (!hasSelectedVoucher) return

        val totalDiscount = currentSelectedVouchers.sumOf {
            it.discount.toLongOrNull() ?: 0L
        }
        binding.txtNumberChooseEndow.text =
            "Đã chọn ${currentSelectedVouchers.size} voucher"
        binding.txtReducedPrice.text =
            "-${formatMoney(totalDiscount)}đ"
    }

    private fun formatMoney(amount: Long): String {
        return NumberFormat
            .getNumberInstance(Locale("vi", "VN"))
            .format(amount)
    }

    private fun setupSearch() {
        binding.edtVoucher.apply {
            onTextChangeListener = { keyword ->
                if (keyword.isEmpty()) {
                    performFilter(keyword)
                }
            }

            setOnSearchActionListener {
                performFilter(
                    getInputField().text?.toString().orEmpty()
                )
            }

            setOnDoneKeyboardListener {
                performFilter(
                    getInputField().text?.toString().orEmpty()
                )
            }
        }
    }

    private fun performFilter(keyword: String) {

        val query = keyword.trim()

        filteredVouchers =
            if (query.isEmpty()) {
                allVouchers
            } else {

                allVouchers.filter { voucher ->

                    voucher.name.contains(
                        query,
                        ignoreCase = true
                    )
                }
            }

        loadVouchers()
    }
}