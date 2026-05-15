package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding
import com.ttcn.promotionsdk.databinding.ItemListPromotionCountBinding

class ApplyPromotionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val vouchers = mutableListOf<PromotionItem>()
    private val maxVisibleVouchers = 2

    companion object {
        private const val VIEW_TYPE_VOUCHER = 0
        private const val VIEW_TYPE_COUNT = 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PromotionItem>) {
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
                val binding = ItemListPromotionApplyBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ApplyPromotionViewHolder(binding)
            }

            VIEW_TYPE_COUNT -> {
                val binding = ItemListPromotionCountBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                CountChoosePromotionViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ApplyPromotionViewHolder -> {
                holder.bind(vouchers[position])
            }

            is CountChoosePromotionViewHolder -> {
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

    private class ApplyPromotionViewHolder(
        private val binding: ItemListPromotionApplyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(voucher: PromotionItem) {
            binding.txtName.text = voucher.discount
        }
    }

    // CountViewHolder.kt
    private class CountChoosePromotionViewHolder(
        private val binding: ItemListPromotionCountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(count: Int) {
            binding.txtCount.text = "+$count"
        }
    }
}