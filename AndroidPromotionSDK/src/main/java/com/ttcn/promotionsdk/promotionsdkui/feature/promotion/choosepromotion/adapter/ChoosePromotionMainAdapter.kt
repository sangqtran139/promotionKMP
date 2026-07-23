package com.ttcn.promotionsdk.promotionsdkui.feature.promotion.choosepromotion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding
import com.ttcn.promotionsdk.databinding.PrmItemSeeMoreBinding
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.promotionsdkui.theme.applier.PromotionListItemApplier
import com.ttcn.promotionsdk.promotionsdkui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.promotionsdkui.utils.extension.toVoucherDisplayDate

internal sealed class ChoosePromotionListItem {
    data class SectionHeader(val title: String) : ChoosePromotionListItem()
    data class VoucherItem(val data: MyVoucherListItem) : ChoosePromotionListItem()

    /**
     * Footer của section "Ưu đãi của tôi".
     *
     * @param isExpanded true  → đang expanded và đã hết trang → hiển thị "Thu gọn"
     *                   false → chưa expanded hoặc còn trang  → hiển thị "Xem thêm"
     */
    data class SeeMoreMyVoucher(val isExpanded: Boolean) : ChoosePromotionListItem()
}

internal class ChoosePromotionMainAdapter(
    private val onVoucherClick: (MyVoucherListItem) -> Unit,
    private val onDetailClick: (MyVoucherListItem) -> Unit,
    private val onSeeMoreMyVoucher: () -> Unit,
    private val onCollapseMyVoucher: () -> Unit,
) : ListAdapter<ChoosePromotionListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_VOUCHER = 1
        private const val TYPE_SEE_MORE = 2
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ChoosePromotionListItem.SectionHeader -> TYPE_HEADER
        is ChoosePromotionListItem.VoucherItem -> TYPE_VOUCHER
        is ChoosePromotionListItem.SeeMoreMyVoucher -> TYPE_SEE_MORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemTitleMyEndowBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            TYPE_SEE_MORE -> FooterViewHolder(
                PrmItemSeeMoreBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> VoucherViewHolder(ItemChoosePromotionBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChoosePromotionListItem.SectionHeader ->
                (holder as HeaderViewHolder).bind(item.title)

            is ChoosePromotionListItem.VoucherItem ->
                (holder as VoucherViewHolder).bind(item.data, onVoucherClick, onDetailClick)

            is ChoosePromotionListItem.SeeMoreMyVoucher ->
                (holder as FooterViewHolder).bind(
                    item.isExpanded,
                    onSeeMoreMyVoucher,
                    onCollapseMyVoucher
                )
        }
    }

    // ─── ViewHolders ──────────────────────────────────────────────────────────

    class HeaderViewHolder(private val binding: ItemTitleMyEndowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.txtTitleEndow.text = title
        }
    }

    class FooterViewHolder(private val binding: PrmItemSeeMoreBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            isExpanded: Boolean,
            onSeeMore: () -> Unit,
            onCollapse: () -> Unit,
        ) {
            binding.txtTitleEndow.text =
                binding.root.context.getString(
                    if (isExpanded) R.string.prm_collapse else R.string.prm_see_more
                )
            binding.imgArrow.rotation = if (isExpanded) 180f else 0f
            binding.root.setOnClickListener {
                if (isExpanded) onCollapse() else onSeeMore()
            }
        }
    }

    class VoucherViewHolder(private val binding: ItemChoosePromotionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            voucher: MyVoucherListItem,
            onVoucherClick: (MyVoucherListItem) -> Unit,
            onDetailClick: (MyVoucherListItem) -> Unit,
        ) {
            binding.apply {
                val ctx = root.context
                val isExpired = voucher.status == VoucherStatus.EXPIRED
                val isNotEnoughApply = voucher.status == VoucherStatus.REVOKED

                txtVoucherName.text = voucher.merchantName
                tvContent.text = voucher.title
                // API không trả HSD → ẩn hẳn dòng ngày (không hiện "HSD:" trống, không dùng date demo).
                val displayDate = voucher.expirationDate.toVoucherDisplayDate()
                tvEndDate.isVisible = displayDate.isNotBlank()
                if (displayDate.isNotBlank()) {
                    tvEndDate.text = ctx.getString(R.string.prm_expiry_short_format, displayDate)
                }

                cbUseVoucher.isChecked = voucher.isSelected
                cbUseVoucher.isClickable = false
                cbUseVoucher.isFocusable = false

                ctlTop.alpha = if (isExpired) 0.6f else 1f
                ctlNotEnoughApplyVoucher.isVisible = isNotEnoughApply
                imgCircleNotEnoughApplyVoucher.isVisible = isNotEnoughApply
                txtExpired.isVisible = isExpired
                lnDetail.isVisible = !isExpired
                cbUseVoucher.visibility =
                    if (isExpired || isNotEnoughApply) View.INVISIBLE else View.VISIBLE

                root.setOnClickListener {
                    if (isExpired || isNotEnoughApply) return@setOnClickListener
                    onVoucherClick(voucher)
                }
                lnDetail.setOnClickListener { onDetailClick(voucher) }

                PromotionListItemApplier.apply(this, PromotionThemeRegistry.listItemToken())
            }
        }
    }

    // ─── DiffCallback ─────────────────────────────────────────────────────────

    class DiffCallback : DiffUtil.ItemCallback<ChoosePromotionListItem>() {
        override fun areItemsTheSame(old: ChoosePromotionListItem, new: ChoosePromotionListItem) =
            when {
                old is ChoosePromotionListItem.SectionHeader && new is ChoosePromotionListItem.SectionHeader -> old.title == new.title
                old is ChoosePromotionListItem.VoucherItem && new is ChoosePromotionListItem.VoucherItem -> old.data.voucherId == new.data.voucherId
                old is ChoosePromotionListItem.SeeMoreMyVoucher && new is ChoosePromotionListItem.SeeMoreMyVoucher -> true
                else -> false
            }

        override fun areContentsTheSame(
            old: ChoosePromotionListItem,
            new: ChoosePromotionListItem
        ) =
            old == new
    }

}