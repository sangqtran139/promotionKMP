package com.ttcn.prm.ui.feature.choosepromotion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.R
import com.ttcn.prm.databinding.PrmItemChoosePromotionBinding
import com.ttcn.prm.databinding.PrmItemTitleMyEndowBinding
import com.ttcn.prm.databinding.PrmItemSectionDividerBinding
import com.ttcn.prm.databinding.PrmItemSeeMoreBinding
import com.ttcn.prm.ui.feature.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.theme.applier.PromotionListItemApplier
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.utils.extension.toHighlightedSpannable
import com.ttcn.prm.ui.utils.extension.toVoucherDisplayDate

internal sealed class ChoosePromotionListItem {
    data class SectionHeader(val title: String) : ChoosePromotionListItem()

    /** [highlightKeyword]: tô đỏ đoạn khớp từ khoá đang tìm — đối ứng `highlightKeyword` bên iOS. */
    data class VoucherItem(
        val data: MyVoucherListItem,
        val highlightKeyword: String = "",
    ) : ChoosePromotionListItem()

    /**
     * Footer của section "Ưu đãi của tôi".
     *
     * @param isExpanded true  → đang expanded và đã hết trang → hiển thị "Thu gọn"
     *                   false → chưa expanded hoặc còn trang  → hiển thị "Xem thêm"
     */
    data class SeeMoreMyVoucher(val isExpanded: Boolean) : ChoosePromotionListItem()

    /** Vạch ngăn giữa hai nhóm — chỉ chèn khi cả hai nhóm cùng có dữ liệu. */
    data object SectionDivider : ChoosePromotionListItem()
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
        private const val TYPE_DIVIDER = 3
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ChoosePromotionListItem.SectionHeader -> TYPE_HEADER
        is ChoosePromotionListItem.VoucherItem -> TYPE_VOUCHER
        is ChoosePromotionListItem.SeeMoreMyVoucher -> TYPE_SEE_MORE
        is ChoosePromotionListItem.SectionDivider -> TYPE_DIVIDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                PrmItemTitleMyEndowBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            TYPE_DIVIDER -> DividerViewHolder(
                PrmItemSectionDividerBinding.inflate(
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

            else -> VoucherViewHolder(PrmItemChoosePromotionBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChoosePromotionListItem.SectionHeader ->
                (holder as HeaderViewHolder).bind(item.title)

            is ChoosePromotionListItem.VoucherItem ->
                (holder as VoucherViewHolder).bind(item, onVoucherClick, onDetailClick)

            is ChoosePromotionListItem.SeeMoreMyVoucher ->
                (holder as FooterViewHolder).bind(
                    item.isExpanded,
                    onSeeMoreMyVoucher,
                    onCollapseMyVoucher
                )

            ChoosePromotionListItem.SectionDivider -> Unit
        }
    }

    // ─── ViewHolders ──────────────────────────────────────────────────────────

    class HeaderViewHolder(private val binding: PrmItemTitleMyEndowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.txtTitleEndow.text = title
        }
    }

    class DividerViewHolder(binding: PrmItemSectionDividerBinding) :
        RecyclerView.ViewHolder(binding.root)

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

    class VoucherViewHolder(private val binding: PrmItemChoosePromotionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: ChoosePromotionListItem.VoucherItem,
            onVoucherClick: (MyVoucherListItem) -> Unit,
            onDetailClick: (MyVoucherListItem) -> Unit,
        ) {
            binding.apply {
                val ctx = root.context
                val voucher = item.data
                // Quyết định "còn dùng được" lấy THẲNG từ store (`ChooseOffer.isUsable` → isEnabled) —
                // không tự suy lại từ status ở đây (giữ 2 nền tảng đồng nhất rule; xem MyPromotionAdapter).
                val canUse = voucher.isEnabled

                val highlightColor = ContextCompat.getColor(ctx, R.color.prm_color_EE0033)
                txtVoucherName.text = voucher.merchantName.toHighlightedSpannable(
                    keyword = item.highlightKeyword,
                    highlightColor = highlightColor,
                )
                tvContent.text = voucher.title.toHighlightedSpannable(
                    keyword = item.highlightKeyword,
                    highlightColor = highlightColor,
                )
                // Dòng ngày: store quyết định "sắp hết hạn" (expiringInDays, theo expireWarningDate của
                // server) → "HSD còn X ngày" tô cam; ngược lại HSD thường (màu mặc định). API không trả
                // HSD (null/rỗng) → "HSD: Không hết hạn"; có HSD mà parse hỏng → ẩn hẳn dòng.
                // Màu phải set mọi nhánh vì ViewHolder bị tái sử dụng.
                val displayDate = voucher.expirationDate.toVoucherDisplayDate()
                val expiringInDays = voucher.expiringInDays
                when {
                    expiringInDays != null -> {
                        tvEndDate.isVisible = true
                        tvEndDate.text = ctx.getString(R.string.prm_expiry_remaining_days, expiringInDays)
                        tvEndDate.setTextColor(ContextCompat.getColor(ctx, R.color.prm_tokenCarrotOrange100))
                    }
                    displayDate.isNotBlank() -> {
                        tvEndDate.isVisible = true
                        tvEndDate.text = ctx.getString(R.string.prm_expiry_short_format, displayDate)
                        tvEndDate.setTextColor(ContextCompat.getColor(ctx, R.color.prm_tokenDark60))
                    }
                    voucher.expirationDate.isBlank() -> {
                        tvEndDate.isVisible = true
                        tvEndDate.text = ctx.getString(R.string.prm_expiry_never)
                        tvEndDate.setTextColor(ContextCompat.getColor(ctx, R.color.prm_tokenDark60))
                    }
                    else -> tvEndDate.isVisible = false
                }

                cbUseVoucher.isChecked = voucher.isSelected
                cbUseVoucher.isClickable = false
                cbUseVoucher.isFocusable = false

                // Không đủ điều kiện → làm mờ card + dải cảnh báo + nhãn lý do, ẩn "Chi tiết" và checkbox.
                // Đối ứng iOS: `isDisabled` (blur overlay) + `isEligible` (warningView) + `stateText`
                // (lý do lấy từ `unmatchedRules`, dự phòng "Không đủ điều kiện").
                ctlTop.alpha = if (canUse) 1f else 0.6f
                ctlNotEnoughApplyVoucher.isVisible = !canUse
                imgCircleNotEnoughApplyVoucher.isVisible = !canUse
                txtExpired.isVisible = !canUse
                txtExpired.text = voucher.displayStatusLabel
                    .ifBlank { ctx.getString(R.string.prm_status_ineligible) }
                lnDetail.isVisible = canUse
                cbUseVoucher.visibility = if (canUse) View.VISIBLE else View.INVISIBLE

                root.setOnClickListener {
                    if (!canUse) return@setOnClickListener
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
                old is ChoosePromotionListItem.SectionDivider && new is ChoosePromotionListItem.SectionDivider -> true
                else -> false
            }

        override fun areContentsTheSame(
            old: ChoosePromotionListItem,
            new: ChoosePromotionListItem
        ) =
            old == new
    }

}