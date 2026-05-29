package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeApplier
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.databinding.ItemListPromotionCountBinding
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem

class ApplyPromotionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val vouchers = mutableListOf<MyVoucherListItem>()
    private val maxVisibleVouchers = 2
    private var badgeTokenOverride: DiscountBadgeToken? = null

    fun applyToken(token: DiscountBadgeToken?) {
        badgeTokenOverride = token
        if (vouchers.isEmpty()) return
        notifyItemRangeChanged(0, minOf(vouchers.size, maxVisibleVouchers))
    }

    companion object {
        private const val VIEW_TYPE_VOUCHER = 0
        private const val VIEW_TYPE_COUNT = 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<MyVoucherListItem>) {
        vouchers.clear()
        vouchers.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (vouchers.size > maxVisibleVouchers && position == maxVisibleVouchers) VIEW_TYPE_COUNT
        else VIEW_TYPE_VOUCHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_VOUCHER -> ApplyPromotionViewHolder(
                ItemListPromotionApplyBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_COUNT -> CountChoosePromotionViewHolder(
                ItemListPromotionCountBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ApplyPromotionViewHolder -> {
                val token = badgeTokenOverride ?: PromotionThemeRegistry.discountBadgeToken()
                holder.bind(vouchers[position], token)
            }
            is CountChoosePromotionViewHolder -> {
                holder.bind(vouchers.size - maxVisibleVouchers)
            }
        }
    }

    override fun getItemCount(): Int = when {
        vouchers.isEmpty() -> 0
        vouchers.size <= maxVisibleVouchers -> vouchers.size
        else -> maxVisibleVouchers + 1
    }

    private class ApplyPromotionViewHolder(
        private val binding: ItemListPromotionApplyBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(voucher: MyVoucherListItem, token: DiscountBadgeToken?) {
            binding.txtName.text = voucher.title
            DiscountBadgeApplier.apply(
                binding,
                token,
                available = voucher.status != VoucherStatus.EXPIRED
            )
        }
    }

    private class CountChoosePromotionViewHolder(
        private val binding: ItemListPromotionCountBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(count: Int) {
            binding.txtCount.text = binding.root.context.getString(
                R.string.prm_vouchers_more_suffix, count
            )
        }
    }
}