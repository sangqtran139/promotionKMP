package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherListItem
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding
import com.ttcn.promotionsdk.databinding.PrmItemSeeMoreBinding
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.theme.PromotionListItemApplier
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry

sealed class ChoosePromotionListItem {
    data class SectionHeader(val title: String) : ChoosePromotionListItem()
    data class VoucherItem(val data: MyVoucherListItem) : ChoosePromotionListItem()
    data object SeeMoreMyVoucher : ChoosePromotionListItem()
}

class ChoosePromotionMainAdapter(
    private val onVoucherClick: (MyVoucherListItem) -> Unit,
    private val onDetailClick: (MyVoucherListItem) -> Unit,
    private val onSeeMoreMyVoucher: () -> Unit,
) : ListAdapter<ChoosePromotionListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_VOUCHER = 1
        private const val TYPE_SEE_MORE_MY = 2
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ChoosePromotionListItem.SectionHeader -> TYPE_HEADER
        is ChoosePromotionListItem.VoucherItem -> TYPE_VOUCHER
        is ChoosePromotionListItem.SeeMoreMyVoucher -> TYPE_SEE_MORE_MY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemTitleMyEndowBinding.inflate(inflater, parent, false))
            TYPE_SEE_MORE_MY -> FooterViewHolder(PrmItemSeeMoreBinding.inflate(inflater, parent, false))
            else -> VoucherViewHolder(ItemChoosePromotionBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChoosePromotionListItem.SectionHeader -> (holder as HeaderViewHolder).bind(item.title)
            is ChoosePromotionListItem.VoucherItem -> (holder as VoucherViewHolder).bind(item.data, onVoucherClick, onDetailClick)
            is ChoosePromotionListItem.SeeMoreMyVoucher -> (holder as FooterViewHolder).bind(onSeeMoreMyVoucher)
        }
    }

    class HeaderViewHolder(private val binding: ItemTitleMyEndowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.txtTitleEndow.text = title
        }
    }

    class FooterViewHolder(private val binding: PrmItemSeeMoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(onClick: () -> Unit) {
            binding.root.setOnClickListener { onClick() }
        }
    }

    class VoucherViewHolder(private val binding: ItemChoosePromotionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            voucher: MyVoucherListItem,
            onVoucherClick: (MyVoucherListItem) -> Unit,
            onDetailClick: (MyVoucherListItem) -> Unit,
        ) {
            binding.apply {
                val ctx = root.context
                val isExpired = voucher.status == VoucherStatus.EXPIRED

                txtVoucherName.text = voucher.merchantName
                tvContent.text = voucher.title
                tvEndDate.text = ctx.getString(
                    R.string.prm_expiry_short_format,
                    voucher.expirationDate.ifEmpty { ctx.getString(R.string.prm_demo_expiry_date) }
                )

                cbUseVoucher.isChecked = voucher.isSelected
                cbUseVoucher.isClickable = false
                cbUseVoucher.isFocusable = false

                ctlTop.alpha = if (isExpired) 0.6f else 1f
                ctlNotEnoughApplyVoucher.isVisible = false
                imgCircleNotEnoughApplyVoucher.isVisible = false
                txtExpired.isVisible = isExpired
                lnDetail.isVisible = !isExpired
                cbUseVoucher.visibility = if (isExpired) View.INVISIBLE else View.VISIBLE

                root.setOnClickListener {
                    if (isExpired) return@setOnClickListener
                    onVoucherClick(voucher)
                }
                lnDetail.setOnClickListener { onDetailClick(voucher) }

                PromotionListItemApplier.apply(this, PromotionThemeRegistry.listItemToken())
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChoosePromotionListItem>() {
        override fun areItemsTheSame(old: ChoosePromotionListItem, new: ChoosePromotionListItem): Boolean {
            return when {
                old is ChoosePromotionListItem.SectionHeader && new is ChoosePromotionListItem.SectionHeader -> old.title == new.title
                old is ChoosePromotionListItem.VoucherItem && new is ChoosePromotionListItem.VoucherItem -> old.data.voucherId == new.data.voucherId
                old is ChoosePromotionListItem.SeeMoreMyVoucher && new is ChoosePromotionListItem.SeeMoreMyVoucher -> true
                else -> false
            }
        }

        override fun areContentsTheSame(old: ChoosePromotionListItem, new: ChoosePromotionListItem) = old == new
    }

    fun updateVoucherSelection(
        voucherId: String,
        isMultiSelection: Boolean = true,
    ) {
        val updatedList = currentList.map { item ->
            if (item is ChoosePromotionListItem.VoucherItem) {

                val selected = when {
                    isMultiSelection && item.data.voucherId == voucherId -> {
                        !item.data.isSelected
                    }

                    !isMultiSelection -> {
                        item.data.voucherId == voucherId &&
                                !item.data.isSelected
                    }

                    else -> {
                        item.data.isSelected
                    }
                }

                item.copy(
                    data = item.data.copy(
                        isSelected = selected
                    )
                )
            } else {
                item
            }
        }

        submitList(updatedList)
    }
}