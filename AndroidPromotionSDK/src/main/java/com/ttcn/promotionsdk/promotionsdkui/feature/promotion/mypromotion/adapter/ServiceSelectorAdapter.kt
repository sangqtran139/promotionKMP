package com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.databinding.ItemServiceSelectorBinding
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.ServiceSelectorUiItem
import com.ttcn.promotionsdk.promotionsdkui.utils.loadPromotionVoucherLogo

internal class ServiceSelectorAdapter(
    private val onItemClick: (ServiceSelectorUiItem) -> Unit,
) : ListAdapter<ServiceSelectorUiItem, ServiceSelectorAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceSelectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemServiceSelectorBinding,
        private val onItemClick: (ServiceSelectorUiItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ServiceSelectorUiItem) {
            binding.imgServiceIcon.loadPromotionVoucherLogo(item.iconUrl)
            binding.tvServiceName.text = item.serviceName.ifBlank { item.serviceCode }
            binding.root.contentDescription = item.serviceName.ifBlank { item.serviceCode }
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ServiceSelectorUiItem>() {
        override fun areItemsTheSame(oldItem: ServiceSelectorUiItem, newItem: ServiceSelectorUiItem) =
            oldItem.serviceCode == newItem.serviceCode

        override fun areContentsTheSame(oldItem: ServiceSelectorUiItem, newItem: ServiceSelectorUiItem) =
            oldItem == newItem
    }
}
