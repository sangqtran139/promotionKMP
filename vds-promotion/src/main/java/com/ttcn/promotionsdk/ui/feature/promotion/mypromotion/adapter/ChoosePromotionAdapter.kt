package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.databinding.ItemLoadingNotifyPrmBinding
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
        val data: MyVoucherListItem,
        val rowKey: String,
    ) : PromotionListItem()

    data object Loading : PromotionListItem()
}

class ChoosePromotionAdapter(
    private val onVoucherClick: (MyVoucherListItem, Int) -> Unit,
    private val onUseClick: (MyVoucherListItem, Int) -> Unit
) : ListAdapter<PromotionListItem, RecyclerView.ViewHolder>(VoucherDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_VOUCHER = 1
        private const val VIEW_TYPE_LOADING = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PromotionListItem.Header -> VIEW_TYPE_HEADER
            is PromotionListItem.Endow -> VIEW_TYPE_VOUCHER
            is PromotionListItem.Loading -> VIEW_TYPE_LOADING
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
            is PromotionListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is PromotionListItem.Endow -> (holder as VoucherViewHolder).bind(item)
            is PromotionListItem.Loading -> Unit
        }
    }

    class LoadingViewHolder(
        binding: ItemLoadingNotifyPrmBinding,
    ) : RecyclerView.ViewHolder(binding.root)

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
): List<PromotionListItem> {
    return buildList {
        if (!headerTitle.isNullOrBlank()) {
            add(PromotionListItem.Header(headerTitle))
        }
        addAll(
            vouchers.mapIndexed { index, voucher ->
                PromotionListItem.Endow(
                    data = voucher,
                    rowKey = voucher.buildStableRowKey(index),
                )
            },
        )
        if (isLoadingMore) {
            add(PromotionListItem.Loading)
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

class VoucherDiffCallback : DiffUtil.ItemCallback<PromotionListItem>() {
    override fun areItemsTheSame(oldItem: PromotionListItem, newItem: PromotionListItem): Boolean {
        return when {
            oldItem is PromotionListItem.Header && newItem is PromotionListItem.Header -> {
                oldItem.title == newItem.title
            }

            oldItem is PromotionListItem.Endow && newItem is PromotionListItem.Endow -> {
                oldItem.rowKey == newItem.rowKey
            }

            oldItem is PromotionListItem.Loading && newItem is PromotionListItem.Loading -> {
                true
            }

            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: PromotionListItem,
        newItem: PromotionListItem,
    ): Boolean {
        return oldItem == newItem
    }
}
