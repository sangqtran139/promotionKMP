package com.ttcn.promotionsdk.ui.utils.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.PrmViewEndowBinding
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ApplyPromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.EndowViewState
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
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
    private var lastAppliedToken: DiscountBadgeToken? = null

    private var onUseVoucherClickListener: (() -> Unit)? = null
    private var onChangeVoucherClickListener: (() -> Unit)? = null
    private var onVoucherItemClickListener: ((MyVoucherListItem) -> Unit)? = null

    init {
        setupRecyclerView()
        setupClickListeners()
        applyToken(PromotionThemeRegistry.discountBadgeToken())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    fun setVoucherCount(count: Int) {
        currentState = if (count > 0) EndowViewState.NOT_APPLIED else EndowViewState.EMPTY
        showNotAppliedState(count)
    }

    fun setAppliedVouchers(appliedVouchers: List<MyVoucherListItem>) {
        if (appliedVouchers.isEmpty()) {
            currentState = EndowViewState.NOT_APPLIED
            showNotAppliedState(count = 0)
        } else {
            currentState = EndowViewState.APPLIED
            showAppliedState(appliedVouchers)
        }
    }

    fun applyToken(token: DiscountBadgeToken?) {
        lastAppliedToken = token
        applyTokenInternal(token)
    }

    fun setTitle(title: String) {
        binding.txtTitleEndow.text = title
    }

    fun getCurrentState(): EndowViewState = currentState

    fun setOnUseVoucherClickListener(listener: () -> Unit) {
        onUseVoucherClickListener = listener
    }

    fun setOnChangeVoucherClickListener(listener: () -> Unit) {
        onChangeVoucherClickListener = listener
    }

    fun setOnVoucherItemClickListener(listener: (MyVoucherListItem) -> Unit) {
        onVoucherItemClickListener = listener
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        binding.rcvEndow.apply {
            adapter = applyPromotionAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupClickListeners() {
        binding.txtStatusEndow.setOnClickListener {
            when (currentState) {
                EndowViewState.NOT_APPLIED -> onUseVoucherClickListener?.invoke()
                EndowViewState.APPLIED -> onChangeVoucherClickListener?.invoke()
                EndowViewState.EMPTY -> Unit
            }
        }
    }

    private fun showNotAppliedState(count: Int) {
        binding.apply {
            txtNumberEndow.text = when (count) {
                0 -> context.getString(R.string.prm_no_endow)
                1 -> context.getString(R.string.prm_one_endow)
                else -> context.getString(R.string.prm_multiple_endow, count)
            }
            txtNumberEndow.visibility = VISIBLE
            rcvEndow.visibility = GONE
            txtStatusEndow.visibility = if (count > 0) VISIBLE else GONE
            txtStatusEndow.text = context.getString(R.string.prm_use_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    private fun showAppliedState(appliedVouchers: List<MyVoucherListItem>) {
        binding.apply {
            applyPromotionAdapter.submitList(appliedVouchers)
            txtNumberEndow.visibility = GONE
            rcvEndow.visibility = VISIBLE
            txtStatusEndow.visibility = VISIBLE
            txtStatusEndow.text = context.getString(R.string.prm_change_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    private fun applyTokenInternal(token: DiscountBadgeToken?) {
        binding.root.background = null
        token?.actionTextColor?.let { binding.txtStatusEndow.applyTextColorIfSet(it) }
        applyPromotionAdapter.applyToken(token)
    }
}