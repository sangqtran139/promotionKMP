package com.ttcn.promotionsdk.promotionsdkui.feature.promotion.choosepromotion.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.entry.PRMAppliedDiscount
import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding
import com.ttcn.promotionsdk.databinding.ItemListPromotionCountBinding
import com.ttcn.promotionsdk.promotionsdkui.theme.applier.DiscountBadgeApplier
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMDiscountBadgeToken
import com.ttcn.promotionsdk.promotionsdkui.theme.PromotionThemeRegistry

/**
 * Adapter cho [rcvEndow] trong PRMEndowView.
 *
 * Luôn render từ [PRMAppliedDiscount] — dữ liệu trực tiếp từ discountDetails
 * trong response của validateStackableDiscounts.
 *
 * - [PRMAppliedDiscount.valid] = true  → hiển thị bình thường
 * - [PRMAppliedDiscount.valid] = false → hiển thị mờ/disabled
 */
internal class ApplyPromotionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<PRMAppliedDiscount>()
    private val maxVisibleVouchers = 2
    private var badgeTokenOverride: PRMDiscountBadgeToken? = null

    fun applyToken(token: PRMDiscountBadgeToken?) {
        badgeTokenOverride = token
        if (items.isEmpty()) return
        notifyItemRangeChanged(0, minOf(items.size, maxVisibleVouchers))
    }

    companion object {
        private const val VIEW_TYPE_VOUCHER = 0
        private const val VIEW_TYPE_COUNT   = 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PRMAppliedDiscount>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items.size > maxVisibleVouchers && position == maxVisibleVouchers)
            VIEW_TYPE_COUNT
        else
            VIEW_TYPE_VOUCHER

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
                holder.bind(items[position], token)
            }
            is CountChoosePromotionViewHolder -> {
                holder.bind(items.size - maxVisibleVouchers)
            }
        }
    }

    override fun getItemCount(): Int = when {
        items.isEmpty()                  -> 0
        items.size <= maxVisibleVouchers -> items.size
        else                             -> maxVisibleVouchers + 1
    }

    // ─── ViewHolders ──────────────────────────────────────────────────────────

    private class ApplyPromotionViewHolder(
        private val binding: ItemListPromotionApplyBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PRMAppliedDiscount, token: PRMDiscountBadgeToken?) {
            binding.txtName.text = item.calculatedDiscount  // objectId làm label fallback

            // valid=false → hiển thị mờ
            binding.root.alpha = if (item.valid) 1f else 0.4f

            DiscountBadgeApplier.apply(
                binding,
                token,
                available = item.valid,
            )
        }
    }

    private class CountChoosePromotionViewHolder(
        private val binding: ItemListPromotionCountBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(count: Int) {
            binding.txtCount.text = binding.root.context.getString(
                R.string.prm_vouchers_more_suffix, count
            )
        }
    }
}