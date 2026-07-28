package com.ttcn.prm.ui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.databinding.ItemServiceSelectorBinding
import com.ttcn.prm.ui.feature.promotion.mypromotion.ServiceSelectorUiItem
import com.ttcn.prm.ui.utils.loadPromotionVoucherLogo

internal class ServiceSelectorAdapter(
    private val visibleItemCount: Int = 3,
    private val onItemClick: (ServiceSelectorUiItem) -> Unit,
) : ListAdapter<ServiceSelectorUiItem, ServiceSelectorAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceSelectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        // Chia bề rộng để **đúng `visibleItemCount` item lọt một màn**, item thứ 4 trở đi nằm ngoài
        // → vuốt ngang mới tới (TLNV MOB_002 item #6).
        //
        // Phải set bằng code: item layout khai `match_parent`, mà trong `LinearLayoutManager` ngang
        // thì `match_parent` = trọn bề ngang RecyclerView → mỗi lần chỉ thấy 1 dịch vụ, và list
        // "không cuộn được" vì mỗi item đã chiếm hết màn.
        //
        // Đo tại đây vì `parent` (RecyclerView) đã đo xong trước khi tạo view holder. Cách cũ
        // (`doOnLayout` + `notifyDataSetChanged`) chạy ngay trong lượt layout → dễ dính
        // "Cannot call this method while RecyclerView is computing a layout or scrolling".
        val usable = parent.measuredWidth - parent.paddingStart - parent.paddingEnd
        if (usable > 0 && visibleItemCount > 0) {
            binding.root.updateLayoutParams { width = usable / visibleItemCount }
        }
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
