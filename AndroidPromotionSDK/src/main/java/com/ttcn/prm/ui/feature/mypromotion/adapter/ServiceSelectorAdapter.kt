package com.ttcn.prm.ui.feature.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.databinding.PrmItemServiceSelectorBinding
import com.ttcn.prm.ui.feature.mypromotion.ServiceSelectorUiItem
import com.ttcn.prm.ui.utils.loadPromotionVoucherLogo

internal class ServiceSelectorAdapter(
    private val onItemClick: (ServiceSelectorUiItem) -> Unit,
) : ListAdapter<ServiceSelectorUiItem, ServiceSelectorAdapter.ViewHolder>(DiffCallback()) {

    /**
     * Không chia bề rộng bằng code: sheet dùng `GridLayoutManager`, nó tự cấp cho mỗi ô đúng
     * 1/`spanCount` bề ngang nên `match_parent` ở item layout là vừa khít.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PrmItemServiceSelectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: PrmItemServiceSelectorBinding,
        private val onItemClick: (ServiceSelectorUiItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ServiceSelectorUiItem) {
            binding.imgServiceIcon.loadPromotionVoucherLogo(item.iconUrl)
            binding.tvServiceName.text = item.productName.ifBlank { item.productId }
            binding.root.contentDescription = item.productName.ifBlank { item.productId }
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ServiceSelectorUiItem>() {
        override fun areItemsTheSame(oldItem: ServiceSelectorUiItem, newItem: ServiceSelectorUiItem) =
            oldItem.productId == newItem.productId

        override fun areContentsTheSame(oldItem: ServiceSelectorUiItem, newItem: ServiceSelectorUiItem) =
            oldItem == newItem
    }
}
