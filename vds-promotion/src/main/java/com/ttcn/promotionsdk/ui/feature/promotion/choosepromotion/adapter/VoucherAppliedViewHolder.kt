package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.databinding.ItemListVoucherApplyBinding
import com.ttcn.promotionsdk.databinding.ItemListVoucherCountBinding

// VoucherAppliedViewHolder.kt
class VoucherAppliedViewHolder(
    private val binding: ItemListVoucherApplyBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(voucher: PromotionVoucherItem) {
        binding.txtName.text = voucher.discount
    }
}

// CountViewHolder.kt
class CountViewHolder(
    private val binding: ItemListVoucherCountBinding
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(count: Int) {
        binding.txtCount.text = "+$count"
    }
}