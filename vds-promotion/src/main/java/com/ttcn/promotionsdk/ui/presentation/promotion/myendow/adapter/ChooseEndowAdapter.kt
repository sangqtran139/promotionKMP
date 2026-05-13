package com.ttcn.promotionsdk.ui.presentation.promotion.myendow.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.databinding.ItemMyEndowBinding
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding

sealed class EndowListItem {
    data class Header(val title: String) : EndowListItem()
    data class Endow(
        val data: PromotionVoucherItem
    ) : EndowListItem()
}

class ChooseEndowAdapter(
    private val onVoucherClick: (PromotionVoucherItem, Int) -> Unit,
    private val onDetailClick: (PromotionVoucherItem, Int) -> Unit
) : ListAdapter<EndowListItem, RecyclerView.ViewHolder>(VoucherDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_VOUCHER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EndowListItem.Header -> VIEW_TYPE_HEADER
            is EndowListItem.Endow -> VIEW_TYPE_VOUCHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemTitleMyEndowBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }

            else -> {
                val binding = ItemMyEndowBinding.inflate(inflater, parent, false)
                VoucherViewHolder(binding, onVoucherClick, onDetailClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is EndowListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is EndowListItem.Endow -> (holder as VoucherViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemTitleMyEndowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EndowListItem.Header) {
            binding.txtTitleEndow.text = item.title
        }
    }

    class VoucherViewHolder(
        private val binding: ItemMyEndowBinding,
        private val onVoucherClick: (PromotionVoucherItem, Int) -> Unit,
        private val onDetailClick: (PromotionVoucherItem, Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: EndowListItem.Endow) {
            binding.apply {
                val voucher = item.data

                // Hiển thị thông tin voucher
                txtVoucherName.text = voucher.name
                tvContent.text = "Giảm giá ${voucher.discount}đ"
                tvEndDate.text = "HSD 15/05/2025"

                // Sử dụng isApplied từ PromotionVoucherItem
                cbUseVoucher.isChecked = voucher.isApplied

                // Disable checkbox click để tránh conflict với root click
                cbUseVoucher.isClickable = false
                cbUseVoucher.isFocusable = false

                ctlTop.alpha = if (voucher.isExpired) 0.6f else 1f
                ctlNotEnoughApplyVoucher.isVisible = voucher.isNotEnoughApplied
                imgCircleNotEnoughApplyVoucher.isVisible = voucher.isNotEnoughApplied
                txtExpired.isVisible = voucher.isExpired
                lnDetail.isVisible = !voucher.isExpired
                cbUseVoucher.visibility = if (voucher.isExpired) View.INVISIBLE else View.VISIBLE

                // Chỉ handle click ở root view
                root.setOnClickListener {
                    if (voucher.isExpired) {
                        return@setOnClickListener
                    }
                    onVoucherClick(voucher, bindingAdapterPosition)
                }

                lnDetail.setOnClickListener {
                    onDetailClick(voucher, bindingAdapterPosition)
                }
            }
        }
    }
}

class VoucherDiffCallback : DiffUtil.ItemCallback<EndowListItem>() {
    override fun areItemsTheSame(oldItem: EndowListItem, newItem: EndowListItem): Boolean {
        return when {
            oldItem is EndowListItem.Header && newItem is EndowListItem.Header -> {
                oldItem.title == newItem.title
            }

            oldItem is EndowListItem.Endow && newItem is EndowListItem.Endow -> {
                oldItem.data.id == newItem.data.id
            }

            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: EndowListItem, newItem: EndowListItem): Boolean {
        return when {
            oldItem is EndowListItem.Header && newItem is EndowListItem.Header -> {
                oldItem == newItem
            }

            oldItem is EndowListItem.Endow && newItem is EndowListItem.Endow -> {
                // So sánh toàn bộ data object, bao gồm cả isApplied
                oldItem.data == newItem.data
            }

            else -> false
        }
    }
}