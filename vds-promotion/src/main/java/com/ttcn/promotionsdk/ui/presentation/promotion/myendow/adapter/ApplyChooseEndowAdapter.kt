package com.ttcn.promotionsdk.ui.presentation.promotion.myendow.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.databinding.ItemListVoucherApplyBinding
import com.ttcn.promotionsdk.databinding.ItemListVoucherCountBinding

class ApplyChooseEndowAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val vouchers = mutableListOf<PromotionVoucherItem>()
    private val maxVisibleVouchers = 2

    companion object {
        private const val VIEW_TYPE_VOUCHER = 0
        private const val VIEW_TYPE_COUNT = 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PromotionVoucherItem>) {
        vouchers.clear()
        vouchers.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (vouchers.size > maxVisibleVouchers && position == maxVisibleVouchers) {
            VIEW_TYPE_COUNT
        } else {
            VIEW_TYPE_VOUCHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_VOUCHER -> {
                val binding = ItemListVoucherApplyBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                VoucherAppliedViewHolder(binding)
            }
            VIEW_TYPE_COUNT -> {
                val binding = ItemListVoucherCountBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                CountViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VoucherAppliedViewHolder -> {
                holder.bind(vouchers[position])
            }
            is CountViewHolder -> {
                val remainingCount = vouchers.size - maxVisibleVouchers
                holder.bind(remainingCount)
            }
        }
    }

    override fun getItemCount(): Int {
        return when {
            vouchers.isEmpty() -> 0
            vouchers.size <= maxVisibleVouchers -> vouchers.size
            else -> maxVisibleVouchers + 1
        }
    }
}