package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeApplier
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.databinding.ItemListPromotionCountBinding

class ApplyPromotionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val vouchers = mutableListOf<PromotionItem>()
    private val maxVisibleVouchers = 2
    private var badgeTokenOverride: DiscountBadgeToken? = null

    fun applyToken(token: DiscountBadgeToken?) {
        badgeTokenOverride = token
        if (vouchers.isEmpty()) return
        val voucherVisible = minOf(vouchers.size, maxVisibleVouchers)
        if (voucherVisible > 0) {
            notifyItemRangeChanged(0, voucherVisible)
        }
    }

    companion object {
        private const val VIEW_TYPE_VOUCHER = 0
        private const val VIEW_TYPE_COUNT = 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PromotionItem>) {
        vouchers.clear()
        vouchers.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (vouchers.size > maxVisibleVouchers && position == maxVisibleVouchers) {
            VIEW_TYPE_COUNT
        } else {
            VIEW_TYPE_VOUCHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_VOUCHER -> {
                val binding = ItemListPromotionApplyBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ApplyPromotionViewHolder(binding)
            }

            VIEW_TYPE_COUNT -> {
                val binding = ItemListPromotionCountBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                CountChoosePromotionViewHolder(binding)
            }

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
                val remainingCount = vouchers.size - maxVisibleVouchers
                holder.bind(remainingCount)
            }
        }
    }

    override fun getItemCount(): Int {
        return when {
            vouchers.isEmpty() -> 0
            vouchers.size <= maxVisibleVouchers -> vouchers.size
            else -> maxVisibleVouchers + 1
        }
    }

    private class ApplyPromotionViewHolder(
        private val binding: ItemListPromotionApplyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(voucher: PromotionItem, token: DiscountBadgeToken?) {
            binding.txtName.text = voucher.discount
            DiscountBadgeApplier.apply(binding, token, available = !voucher.isExpired)
        }
    }

    // CountViewHolder.kt
    private class CountChoosePromotionViewHolder(
        private val binding: ItemListPromotionCountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(count: Int) {
            val ctx = binding.root.context
            binding.txtCount.text = ctx.getString(R.string.prm_vouchers_more_suffix, count)
        }
    }
}