package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.theme.PromotionListItemApplier
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.utils.extension.toVoucherDisplayDate
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherLogo

sealed class PromotionListItem {
    data class Header(val title: String) : PromotionListItem()
    data class Endow(
        val data: MyVoucherListItem
    ) : PromotionListItem()
}

class ChoosePromotionAdapter(
    private val onVoucherClick: (MyVoucherListItem, Int) -> Unit,
    private val onUseClick: (MyVoucherListItem, Int) -> Unit
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
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemTitleMyEndowBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }

            VIEW_TYPE_VOUCHER -> {
                val binding = PrmItemPromotionBinding.inflate(inflater, parent, false)
                VoucherViewHolder(binding, onVoucherClick, onUseClick)
            }

            else -> {
                val binding = PrmItemPromotionBinding.inflate(inflater, parent, false)
                VoucherViewHolder(binding, onVoucherClick, onUseClick)
            }
        }
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
        private val onVoucherClick: (MyVoucherListItem, Int) -> Unit,
        private val onUseClick: (MyVoucherListItem, Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PromotionListItem.Endow) {
            binding.apply {
                val voucher = item.data
                val ctx = binding.root.context

                // Hiển thị thông tin voucher
                imgVoucher.loadPromotionVoucherLogo(voucher.logo)
                txtVoucherName.text = voucher.merchantName
                tvContent.text = voucher.title.ifBlank { voucher.description }
                tvEndDate.text = ctx.getString(
                    R.string.prm_expiry_short_format,
                    voucher.expirationDate.toVoucherDisplayDate(),
                )

                val canUse = voucher.status == VoucherStatus.ACTIVE
                ctlTop.alpha = if (canUse) 1f else 0.6f
                txtExpired.isVisible = !canUse
                txtExpired.text = voucher.displayStatusLabel.ifBlank {
                    ctx.getString(R.string.prm_is_used)
                }
                lnDetail.isVisible = canUse
                txtExpired.isEnabled = !lnDetail.isVisible
                txtExpired.isClickable = !lnDetail.isVisible

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
                oldItem.data.voucherId == newItem.data.voucherId
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