package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.databinding.ItemLoadingNotifyPrmBinding
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.theme.applier.PromotionListItemApplier
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.utils.extension.toHighlightedSpannable
import com.ttcn.promotionsdk.ui.utils.extension.toVoucherDisplayDate
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherLogo

sealed class MyPromotionListItem {
    data class Header(val title: String) : MyPromotionListItem()

    data class Endow(
        val data: MyVoucherListItem,
        val rowKey: String,
        val highlightKeyword: String = "",
    ) : MyPromotionListItem()

    data object Loading : MyPromotionListItem()
}

class MyPromotionAdapter(
    private val onVoucherClick: (MyVoucherListItem, Int) -> Unit,
    private val onUseClick: (MyVoucherListItem, Int) -> Unit
) : ListAdapter<MyPromotionListItem, RecyclerView.ViewHolder>(MyPromotionDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_VOUCHER = 1
        private const val VIEW_TYPE_LOADING = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MyPromotionListItem.Header -> VIEW_TYPE_HEADER
            is MyPromotionListItem.Endow -> VIEW_TYPE_VOUCHER
            is MyPromotionListItem.Loading -> VIEW_TYPE_LOADING
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

            VIEW_TYPE_LOADING -> {
                val binding = ItemLoadingNotifyPrmBinding.inflate(inflater, parent, false)
                LoadingViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MyPromotionListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MyPromotionListItem.Endow -> (holder as VoucherViewHolder).bind(item)
            is MyPromotionListItem.Loading -> Unit
        }
    }

    class LoadingViewHolder(
        binding: ItemLoadingNotifyPrmBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    class HeaderViewHolder(
        private val binding: ItemTitleMyEndowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MyPromotionListItem.Header) {
            binding.txtTitleEndow.text = item.title
        }
    }

    class VoucherViewHolder(
        private val binding: PrmItemPromotionBinding,
        private val onVoucherClick: (MyVoucherListItem, Int) -> Unit,
        private val onUseClick: (MyVoucherListItem, Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MyPromotionListItem.Endow) {
            binding.apply {
                val voucher = item.data
                val ctx = binding.root.context
                val canUse = voucher.status == VoucherStatus.ACTIVE

                imgVoucher.loadPromotionVoucherLogo(voucher.logo)

                val highlightColor = ContextCompat.getColor(ctx, R.color.color_EE0033)
                val highlightKeyword = item.highlightKeyword
                txtVoucherName.text = voucher.merchantName.toHighlightedSpannable(
                    keyword = highlightKeyword,
                    highlightColor = highlightColor,
                )
                tvContent.text = voucher.title
                    .ifBlank { voucher.description }
                    .toHighlightedSpannable(
                        keyword = highlightKeyword,
                        highlightColor = highlightColor,
                    )
                tvEndDate.text = ctx.getString(
                    R.string.prm_expiry_short_format,
                    voucher.expirationDate.toVoucherDisplayDate(),
                )

                ctlTop.alpha = if (canUse) 1f else 0.6f
                txtExpired.isVisible = !canUse
                txtExpired.text = voucher.displayStatusLabel.ifBlank {
                    ctx.getString(R.string.prm_is_used)
                }
                lnDetail.isVisible = canUse
                txtExpired.isEnabled = !lnDetail.isVisible
                txtExpired.isClickable = !lnDetail.isVisible

                root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onVoucherClick(voucher, position)
                    }
                }

                tvUse.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onUseClick(voucher, position)
                    }
                }

                PromotionListItemApplier.apply(
                    this,
                    PromotionThemeRegistry.listItemToken(),
                )
            }
        }
    }
}

fun buildPromotionListItems(
    vouchers: List<MyVoucherListItem>,
    isLoadingMore: Boolean,
    headerTitle: String? = null,
    keyword: String = "",
): List<MyPromotionListItem> {
    return buildList {
        if (!headerTitle.isNullOrBlank()) {
            add(MyPromotionListItem.Header(headerTitle))
        }
        addAll(
            vouchers.mapIndexed { index, voucher ->
                MyPromotionListItem.Endow(
                    data = voucher,
                    rowKey = voucher.buildStableRowKey(index),
                    highlightKeyword = keyword,
                )
            },
        )
        if (isLoadingMore) {
            add(MyPromotionListItem.Loading)
        }
    }
}

private fun MyVoucherListItem.buildStableRowKey(index: Int): String {
    return listOf(
        voucherId,
        campaignId,
        merchantName,
        title,
        expirationDate,
        status.name,
        displayStatusLabel,
        index.toString(),
    ).joinToString(separator = "_")
}

class MyPromotionDiffCallback : DiffUtil.ItemCallback<MyPromotionListItem>() {
    override fun areItemsTheSame(oldItem: MyPromotionListItem, newItem: MyPromotionListItem): Boolean {
        return when {
            oldItem is MyPromotionListItem.Header && newItem is MyPromotionListItem.Header -> {
                oldItem.title == newItem.title
            }

            oldItem is MyPromotionListItem.Endow && newItem is MyPromotionListItem.Endow -> {
                oldItem.rowKey == newItem.rowKey
            }

            oldItem is MyPromotionListItem.Loading && newItem is MyPromotionListItem.Loading -> {
                true
            }

            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: MyPromotionListItem,
        newItem: MyPromotionListItem,
    ): Boolean {
        return oldItem == newItem
    }
}
