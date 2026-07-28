package com.ttcn.prm.ui.feature.promotion.mypromotion.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.databinding.ItemTagMyPromotionBinding
import com.ttcn.prm.ui.feature.promotion.mypromotion.TabItem
import com.ttcn.prm.ui.theme.PromotionThemeDefaults
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.theme.applier.TabChipThemeApplier

internal class MyPromotionTabAdapter(
    private val onTabSelected: (TabItem) -> Unit = {},
) : RecyclerView.Adapter<MyPromotionTabAdapter.TabViewHolder>() {
    private var tabs: List<TabItem> = emptyList()

    /** Hiện "(số lượng)" cạnh tên tab hay không — xem [submitTabs]. */
    private var showCount: Boolean = false

    var selectedPosition: Int = RecyclerView.NO_POSITION
        private set

    override fun getItemCount(): Int = tabs.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemTagMyPromotionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(tabs[position], position == selectedPosition)
    }

    inner class TabViewHolder(
        private val binding: ItemTagMyPromotionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION || pos == selectedPosition) return@setOnClickListener
                val old = selectedPosition
                selectedPosition = pos
                if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
                notifyItemChanged(pos)
                onTabSelected(tabs[pos])
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(tab: TabItem, selected: Boolean) {
            // Chưa load xong danh sách → chỉ tên tab (TLNV MOB_001 control #3).
            val countText = tab.count.takeIf { showCount && it >= 0 }?.let { " ($it)" }.orEmpty()
            binding.tvTag.text = tab.label + countText
            val ctx = binding.root.context
            val token = PromotionThemeRegistry.tabChipToken()
                ?: PromotionThemeDefaults.tabChip(ctx)
            TabChipThemeApplier.applyChip(binding.tvTag, selected, token)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    /**
     * @param showCount `false` khi **chưa load xong danh sách** → chỉ hiện tên tab, không hiện
     * "(số lượng)" (TLNV MOB_001 control #3). Đối ứng iOS `renderTabs(..., showCount:)`.
     */
    fun submitTabs(items: List<TabItem>, selectedCode: String?, showCount: Boolean = true) {
        val newSelectedPosition = if (items.isEmpty()) {
            RecyclerView.NO_POSITION
        } else {
            items.indexOfFirst { it.code == selectedCode }.takeIf { it >= 0 } ?: 0
        }
        val tabsChanged = tabs != items || showCount != this.showCount
        val oldSelectedPosition = selectedPosition

        tabs = items
        this.showCount = showCount
        selectedPosition = newSelectedPosition

        when {
            tabsChanged -> notifyDataSetChanged()
            oldSelectedPosition != newSelectedPosition -> {
                if (oldSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(oldSelectedPosition)
                }
                if (newSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(newSelectedPosition)
                }
            }
        }
    }
}
