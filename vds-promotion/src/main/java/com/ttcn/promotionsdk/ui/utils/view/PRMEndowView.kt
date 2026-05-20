// PRMEndowView.kt
package com.ttcn.promotionsdk.ui.utils.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.PrmViewEndowBinding
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ApplyPromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.EndowViewState
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.PromotionItem
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.utils.applyTextColorIfSet

class PRMEndowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: PrmViewEndowBinding =
        PrmViewEndowBinding.inflate(LayoutInflater.from(context), this, true)
    private val applyPromotionAdapter = ApplyPromotionAdapter()

    private var currentState: EndowViewState = EndowViewState.NOT_APPLIED
    private var allVouchers: List<PromotionItem> = emptyList()
    private var lastAppliedToken: DiscountBadgeToken? = null

    // Callbacks
    private var onUseVoucherClickListener: (() -> Unit)? = null
    private var onChangeVoucherClickListener: (() -> Unit)? = null
    private var onVoucherItemClickListener: ((PromotionItem) -> Unit)? = null

    init {
        setupRecyclerView()
        setupClickListeners()
        applyToken(PromotionThemeRegistry.discountBadgeToken())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    fun applyToken(token: DiscountBadgeToken?) {
        lastAppliedToken = token
        applyTokenInternal(token)
    }

    private fun applyTokenInternal(token: DiscountBadgeToken?) {
        // Text/background badge tokens apply to voucher chips only (DiscountBadgeApplier).
        binding.root.background = null
        token?.actionTextColor?.let { binding.txtStatusEndow.applyTextColorIfSet(it) }
        applyPromotionAdapter.applyToken(token)
    }

    private fun setupRecyclerView() {
        binding.rcvEndow.apply {
            adapter = applyPromotionAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

//        voucherAdapter.setOnItemClickListener { voucher ->
//            onVoucherItemClickListener?.invoke(voucher)
//        }
    }

    private fun setupClickListeners() {
        binding.txtStatusEndow.setOnClickListener {
            when (currentState) {
                EndowViewState.NOT_APPLIED -> {
                    // Bấm "Sử dụng" -> mở màn chọn voucher
                    onUseVoucherClickListener?.invoke()
                }

                EndowViewState.APPLIED -> {
                    // Bấm "Chọn lại" -> mở màn chọn voucher khác
                    onChangeVoucherClickListener?.invoke()
                }

                EndowViewState.EMPTY -> {
                    // Không làm gì
                }
            }
        }
    }

    /**
     * Set danh sách tất cả vouchers
     * Tự động phát hiện trạng thái dựa vào isApplied
     */
    fun setVouchers(vouchers: List<PromotionItem>) {
        allVouchers = vouchers

        currentState = when {
            vouchers.isEmpty() -> EndowViewState.EMPTY
            vouchers.any { it.isApplied } -> EndowViewState.APPLIED
            else -> EndowViewState.NOT_APPLIED
        }

        updateUI()
    }

    /**
     * Set trạng thái thủ công
     */
    fun setState(state: EndowViewState, voucherCount: Int = 0) {
        currentState = state
        allVouchers = emptyList()

        if (state == EndowViewState.NOT_APPLIED && voucherCount > 0) {
            binding.txtNumberEndow.text = context.getString(
                R.string.prm_multiple_endow, voucherCount
            )
        }

        updateUI()
    }

    private fun updateUI() {
        when (currentState) {
            EndowViewState.EMPTY -> {
                showNotAppliedState()
            }

            EndowViewState.NOT_APPLIED -> {
                showNotAppliedState()
            }

            EndowViewState.APPLIED -> {
                showAppliedState()
            }
        }
    }

    /**
     * Trạng thái: Không có voucher nào
     */
    private fun showEmptyState() {
        binding.apply {
            txtNumberEndow.text = context.getString(R.string.prm_no_endow)
            txtNumberEndow.visibility = View.VISIBLE
            rcvEndow.visibility = View.GONE
            txtStatusEndow.visibility = View.GONE
        }
    }

    /**
     * Trạng thái: Có voucher nhưng chưa apply
     * Hiển thị: "Bạn có X mã ưu đãi" + button "Sử dụng"
     */
    private fun showNotAppliedState() {
        binding.apply {
            val count = allVouchers.size
            txtNumberEndow.text = when (count) {
                0 -> context.getString(R.string.prm_no_endow)
                1 -> context.getString(R.string.prm_one_endow)
                else -> context.getString(R.string.prm_multiple_endow, count)
            }

            txtNumberEndow.visibility = VISIBLE
            rcvEndow.visibility = GONE
            txtStatusEndow.visibility = VISIBLE
            txtStatusEndow.text = context.getString(R.string.prm_use_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    /**
     * Trạng thái: Đã apply voucher
     * Hiển thị: RecyclerView với vouchers đã apply + button "Chọn lại"
     */
    private fun showAppliedState() {
        binding.apply {
            val appliedVouchers = allVouchers.filter { it.isApplied }

            applyPromotionAdapter.submitList(appliedVouchers)

            txtNumberEndow.visibility = View.GONE
            rcvEndow.visibility = View.VISIBLE
            txtStatusEndow.visibility = View.VISIBLE
            txtStatusEndow.text = context.getString(R.string.prm_change_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    /**
     * Set callback khi bấm "Sử dụng" (trạng thái NOT_APPLIED)
     */
    fun setOnUseVoucherClickListener(listener: () -> Unit) {
        onUseVoucherClickListener = listener
    }

    /**
     * Set callback khi bấm "Chọn lại" (trạng thái APPLIED)
     */
    fun setOnChangeVoucherClickListener(listener: () -> Unit) {
        onChangeVoucherClickListener = listener
    }

    /**
     * Set callback khi click vào voucher item trong RecyclerView
     */
    fun setOnVoucherItemClickListener(listener: (PromotionItem) -> Unit) {
        onVoucherItemClickListener = listener
    }

    /**
     * Set title tùy chỉnh
     */
    fun setTitle(title: String) {
        binding.txtTitleEndow.text = title
    }

    /**
     * Get trạng thái hiện tại
     */
    fun getCurrentState(): EndowViewState = currentState

    /**
     * Get danh sách vouchers đã apply
     */
    fun getAppliedVouchers(): List<PromotionItem> {
        return allVouchers.filter { it.isApplied }
    }
}