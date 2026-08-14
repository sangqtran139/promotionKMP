package com.ttcn.prm.ui.feature.choosepromotion.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.R
import com.ttcn.prm.ui.feature.endowview.AppliedDiscount
import com.ttcn.prm.databinding.PrmItemListPromotionApplyBinding
import com.ttcn.prm.databinding.PrmItemListPromotionCountBinding
import com.ttcn.prm.ui.theme.applier.DiscountBadgeApplier
import com.ttcn.prm.ui.theme.token.DiscountBadgeToken
import com.ttcn.prm.ui.theme.PromotionThemeRegistry

/**
 * Adapter cho [rcvEndow] trong PRMEndowView.
 *
 * Luôn render từ [AppliedDiscount] — dữ liệu trực tiếp từ discountDetails
 * trong response của validateStackableDiscounts.
 *
 * - [AppliedDiscount.valid] = true  → nền/chữ xanh mint (bình thường)
 * - [AppliedDiscount.valid] = false → nền/chữ xám, đồng bộ nút "+N" — xem [DiscountBadgeApplier]
 */
internal class ApplyPromotionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<AppliedDiscount>()
    private val maxVisibleVouchers = 2
    private var badgeTokenOverride: DiscountBadgeToken? = null

    fun applyToken(token: DiscountBadgeToken?) {
        badgeTokenOverride = token
        if (items.isEmpty()) return
        notifyItemRangeChanged(0, minOf(items.size, maxVisibleVouchers))
    }

    companion object {
        private const val VIEW_TYPE_VOUCHER = 0
        private const val VIEW_TYPE_COUNT   = 1
    }

    /**
     * Danh sách y hệt thì thôi: `PRMEndowView.renderState` nay chạy theo **[EndowState] dùng chung**,
     * tức nó thức dậy cả với những field widget không vẽ (`isValidating`, `isLoading`) — trước đây
     * `PRMEndowUiState` không có mấy field đó nên nuốt luôn. Không chặn ở đây thì mỗi vòng validate
     * lại một lần `notifyDataSetChanged` rebind toàn bộ hàng với đúng dữ liệu cũ.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<AppliedDiscount>) {
        if (items == list) return
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items.size > maxVisibleVouchers && position == maxVisibleVouchers)
            VIEW_TYPE_COUNT
        else
            VIEW_TYPE_VOUCHER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_VOUCHER -> ApplyPromotionViewHolder(
                PrmItemListPromotionApplyBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_COUNT -> CountChoosePromotionViewHolder(
                PrmItemListPromotionCountBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ApplyPromotionViewHolder -> {
                val token = badgeTokenOverride ?: PromotionThemeRegistry.discountBadgeToken()
                holder.bind(items[position], token)
            }
            is CountChoosePromotionViewHolder -> {
                holder.bind(items.size - maxVisibleVouchers)
            }
        }
    }

    override fun getItemCount(): Int = when {
        items.isEmpty()                  -> 0
        items.size <= maxVisibleVouchers -> items.size
        else                             -> maxVisibleVouchers + 1
    }

    // ─── ViewHolders ──────────────────────────────────────────────────────────

    private class ApplyPromotionViewHolder(
        private val binding: PrmItemListPromotionApplyBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppliedDiscount, token: DiscountBadgeToken?) {
            // Text ưu tiên lấy từ `tags[0]` (nhãn server gửi sẵn). Server cũ / host tự dựng
            // `AppliedDiscount` qua `setDiscountDetails` không kèm tags → fallback format số tiền
            // giảm như trước, tránh voucher hiện chữ trống. Đối ứng `PromotionSDKImpl.formatDiscount` bên iOS.
            binding.txtName.text = item.tags.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: formatDiscountAmount(binding.root.context, item.calculatedDiscount)

            // Nền/chữ đổi màu (xanh mint khi valid, xám khi không) — xem `DiscountBadgeApplier`.
            DiscountBadgeApplier.apply(
                binding,
                token,
                available = item.valid,
            )
        }
    }

    private class CountChoosePromotionViewHolder(
        private val binding: PrmItemListPromotionCountBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(count: Int) {
            binding.txtCount.text = binding.root.context.getString(
                R.string.prm_vouchers_more_suffix, count
            )
        }
    }
}

/**
 * `"75000"` → `"Giảm 75.000đ"`. Chuỗi không rút được số → trả nguyên bản (không bịa "0đ").
 * Đối ứng `PromotionSDKImpl.formatDiscount` bên iOS.
 */
private fun formatDiscountAmount(context: Context, raw: String): String {
    val digits = raw.filter { it.isDigit() }
    val value = digits.toLongOrNull() ?: return raw
    val grouped = value.toString().reversed().chunked(3).joinToString(".").reversed()
    return context.getString(R.string.prm_discount_amount_format, grouped)
}
