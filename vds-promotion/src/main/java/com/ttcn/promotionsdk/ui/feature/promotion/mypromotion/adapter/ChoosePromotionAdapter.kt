package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.PromotionItem
import com.ttcn.promotionsdk.ui.theme.PromotionListItemApplier
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherLogo

sealed class PromotionListItem {
    data class Header(val title: String) : PromotionListItem()
    data class Endow(
        val data: PromotionItem
    ) : PromotionListItem()
}

class ChoosePromotionAdapter(
    private val onVoucherClick: (PromotionItem, Int) -> Unit,
    private val onUseClick: (PromotionItem, Int) -> Unit
) : ListAdapter<PromotionListItem, RecyclerView.ViewHolder>(VoucherDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_VOUCHER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PromotionListItem.Header -> VIEW_TYPE_HEADER
            is PromotionListItem.Endow -> VIEW_TYPE_VOUCHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PrmItemPromotionBinding.inflate(inflater, parent, false)
        return VoucherViewHolder(binding, onVoucherClick, onUseClick)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PromotionListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is PromotionListItem.Endow -> (holder as VoucherViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemTitleMyEndowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PromotionListItem.Header) {
            binding.txtTitleEndow.text = item.title
        }
    }

    class VoucherViewHolder(
        private val binding: PrmItemPromotionBinding,
        private val onVoucherClick: (PromotionItem, Int) -> Unit,
        private val onUseClick: (PromotionItem, Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PromotionListItem.Endow) {
            binding.apply {
                val voucher = item.data
                val ctx = binding.root.context

                // Hiển thị thông tin voucher
                imgVoucher.loadPromotionVoucherLogo(voucher.urlLogo)
                txtVoucherName.text = voucher.name
                tvContent.text = ctx.getString(R.string.prm_discount_amount_format, voucher.discount)
                tvEndDate.text = ctx.getString(
                    R.string.prm_expiry_short_format,
                    ctx.getString(R.string.prm_demo_expiry_date),
                )

                ctlTop.alpha = if (voucher.isExpired) 0.6f else 1f
                txtExpired.isVisible = voucher.isExpired
                lnDetail.isVisible = !voucher.isExpired

                // Chỉ handle click ở root view
                root.setOnClickListener {
                    onVoucherClick(voucher, bindingAdapterPosition)
                }

                tvUse.setOnClickListener {
                    onUseClick(voucher, bindingAdapterPosition)
                }

                PromotionListItemApplier.apply(
                    this,
                    PromotionThemeRegistry.listItemToken(),
                )
            }
        }
    }
}

class VoucherDiffCallback : DiffUtil.ItemCallback<PromotionListItem>() {
    override fun areItemsTheSame(oldItem: PromotionListItem, newItem: PromotionListItem): Boolean {
        return when {
            oldItem is PromotionListItem.Header && newItem is PromotionListItem.Header -> {
                oldItem.title == newItem.title
            }

            oldItem is PromotionListItem.Endow && newItem is PromotionListItem.Endow -> {
                oldItem.data.id == newItem.data.id
            }

            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: PromotionListItem,
        newItem: PromotionListItem
    ): Boolean {
        return when (oldItem) {
            is PromotionListItem.Header if newItem is PromotionListItem.Header -> {
                oldItem == newItem
            }

            is PromotionListItem.Endow if newItem is PromotionListItem.Endow -> {
                oldItem.data == newItem.data
            }

            else -> false
        }
    }
}