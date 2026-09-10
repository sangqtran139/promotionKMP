package com.ttcn.prm.ui.feature.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.R
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.domain.model.voucher.displayState
import com.ttcn.prm.databinding.PrmItemLoadingNotifyPrmBinding
import com.ttcn.prm.databinding.PrmItemTitleMyOfferBinding
import com.ttcn.prm.databinding.PrmItemPromotionBinding
import com.ttcn.prm.ui.feature.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.theme.applier.PromotionListItemApplier
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.utils.extension.toHighlightedSpannable
import com.ttcn.prm.ui.utils.extension.toVoucherDisplayDate
import com.ttcn.prm.ui.utils.loadPromotionVoucherLogo

internal sealed class MyPromotionListItem {
    data class Header(val title: String) : MyPromotionListItem()

    data class Offer(
        val data: MyVoucherListItem,
        val rowKey: String,
        val highlightKeyword: String = "",
    ) : MyPromotionListItem()

    data object Loading : MyPromotionListItem()
}

internal class MyPromotionAdapter(
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
            is MyPromotionListItem.Offer -> VIEW_TYPE_VOUCHER
            is MyPromotionListItem.Loading -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = PrmItemTitleMyOfferBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }

            VIEW_TYPE_VOUCHER -> {
                val binding = PrmItemPromotionBinding.inflate(inflater, parent, false)
                VoucherViewHolder(binding, onVoucherClick, onUseClick)
            }

            VIEW_TYPE_LOADING -> {
                val binding = PrmItemLoadingNotifyPrmBinding.inflate(inflater, parent, false)
                LoadingViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MyPromotionListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MyPromotionListItem.Offer -> (holder as VoucherViewHolder).bind(item)
            is MyPromotionListItem.Loading -> Unit
        }
    }

    class LoadingViewHolder(
        binding: PrmItemLoadingNotifyPrmBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    class HeaderViewHolder(
        private val binding: PrmItemTitleMyOfferBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MyPromotionListItem.Header) {
            binding.txtTitleOffer.text = item.title
        }
    }

    class VoucherViewHolder(
        private val binding: PrmItemPromotionBinding,
        private val onVoucherClick: (MyVoucherListItem, Int) -> Unit,
        private val onUseClick: (MyVoucherListItem, Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MyPromotionListItem.Offer) {
            binding.apply {
                val voucher = item.data
                val ctx = binding.root.context
                // Quyết định "còn dùng được" lấy THẲNG từ store (promotionLogic) — không tự suy lại
                // từ status ở đây (giữ 2 nền tảng đồng nhất rule; xem MyPromotionVoucher.isEnabled).
                val canUse = voucher.isEnabled

                imgVoucher.loadPromotionVoucherLogo(voucher.logo)

                val highlightColor = ContextCompat.getColor(ctx, R.color.prm_color_EE0033)
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
                // Dòng ngày: store quyết định "sắp hết hạn" (expiringInDays, theo expireWarningDate của
                // server) → hiện "HSD còn X ngày" tô cam; ngược lại hiện HSD thường (màu mặc định).
                // API không trả HSD (null/rỗng) → "HSD: Không hết hạn"; có HSD mà parse hỏng → ẩn hẳn
                // (không dám khẳng định vô hạn). Màu phải set mọi nhánh vì ViewHolder bị tái sử dụng.
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

                ctlTop.alpha = if (canUse) 1f else 0.6f
                txtExpired.isVisible = !canUse
                // Nhãn trạng thái (đối ứng iOS `MyPromotionCellViewModel`):
                // - HẾT HẠN / ĐÃ DÙNG: LUÔN dùng chuỗi của SDK. `displayStatusLabel` là
                //   `metadata.disabledReason` thô của server — mã enum ("EXPIRED", "REDEEMED"),
                //   không phải chuỗi hiển thị, ưu tiên nó thì tag lòi chữ tiếng Anh ra UI.
                // - Còn lại: giữ nhãn server (lý do không đủ điều kiện, đã là câu đọc được).
                txtExpired.text = when (voucher.status.displayState()) {
                    VoucherDisplayState.USED -> ctx.getString(R.string.prm_status_used)
                    VoucherDisplayState.EXPIRED -> ctx.getString(R.string.prm_status_expired)
                    else -> voucher.displayStatusLabel.ifBlank {
                        ctx.getString(R.string.prm_status_ineligible)
                    }
                }
                lnDetail.isVisible = canUse
                // NHÃN NÚT lấy từ server (vd AVAILABLE_TO_CLAIM → "Nhận"), dự phòng "Sử dụng" —
                // đối ứng iOS và đối ứng luôn màn Chi tiết (cũng dùng displayStatusLabel).
                if (canUse) {
                    tvUse.text = voucher.displayStatusLabel.ifBlank { ctx.getString(R.string.prm_use) }
                }
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

internal fun buildPromotionListItems(
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
                MyPromotionListItem.Offer(
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

internal class MyPromotionDiffCallback : DiffUtil.ItemCallback<MyPromotionListItem>() {
    override fun areItemsTheSame(oldItem: MyPromotionListItem, newItem: MyPromotionListItem): Boolean {
        return when {
            oldItem is MyPromotionListItem.Header && newItem is MyPromotionListItem.Header -> {
                oldItem.title == newItem.title
            }

            oldItem is MyPromotionListItem.Offer && newItem is MyPromotionListItem.Offer -> {
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
