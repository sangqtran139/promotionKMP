package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding

sealed class ChoosePromotionListItem {
    data class Header(val title: String) : ChoosePromotionListItem()
    data class ChoosePromotion(
        val data: PromotionItem
    ) : ChoosePromotionListItem()
}

class ListChoosePromotionAdapter(
    private val onVoucherClick: (PromotionItem, Int) -> Unit,
    private val onDetailClick: (PromotionItem, Int) -> Unit
) : ListAdapter<ChoosePromotionListItem, RecyclerView.ViewHolder>(ChoosePromotionDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_VOUCHER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChoosePromotionListItem.Header -> VIEW_TYPE_HEADER
            is ChoosePromotionListItem.ChoosePromotion -> VIEW_TYPE_VOUCHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemTitleMyEndowBinding.inflate(inflater, parent, false)
                HeaderListChoosePromotionViewHolder(binding)
            }

            else -> {
                val binding = ItemChoosePromotionBinding.inflate(inflater, parent, false)
                ItemPromotionViewHolder(binding, onVoucherClick, onDetailClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChoosePromotionListItem.Header -> (holder as HeaderListChoosePromotionViewHolder).bind(
                item
            )

            is ChoosePromotionListItem.ChoosePromotion -> (holder as ItemPromotionViewHolder).bind(
                item
            )
        }
    }

    private class HeaderListChoosePromotionViewHolder(
        private val binding: ItemTitleMyEndowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChoosePromotionListItem.Header) {
            binding.txtTitleEndow.text = item.title
        }
    }

    private class ItemPromotionViewHolder(
        private val binding: ItemChoosePromotionBinding,
        private val onVoucherClick: (PromotionItem, Int) -> Unit,
        private val onDetailClick: (PromotionItem, Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: ChoosePromotionListItem.ChoosePromotion) {
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

class ChoosePromotionDiffCallback : DiffUtil.ItemCallback<ChoosePromotionListItem>() {
    override fun areItemsTheSame(
        oldItem: ChoosePromotionListItem,
        newItem: ChoosePromotionListItem
    ): Boolean {
        return when {
            oldItem is ChoosePromotionListItem.Header && newItem is ChoosePromotionListItem.Header -> {
                oldItem.title == newItem.title
            }

            oldItem is ChoosePromotionListItem.ChoosePromotion && newItem is ChoosePromotionListItem.ChoosePromotion -> {
                oldItem.data.id == newItem.data.id
            }

            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: ChoosePromotionListItem,
        newItem: ChoosePromotionListItem
    ): Boolean {
        return when {
            oldItem is ChoosePromotionListItem.Header && newItem is ChoosePromotionListItem.Header -> {
                oldItem == newItem
            }

            oldItem is ChoosePromotionListItem.ChoosePromotion && newItem is ChoosePromotionListItem.ChoosePromotion -> {
                // So sánh toàn bộ data object, bao gồm cả isApplied
                oldItem.data == newItem.data
            }

            else -> false
        }
    }
}