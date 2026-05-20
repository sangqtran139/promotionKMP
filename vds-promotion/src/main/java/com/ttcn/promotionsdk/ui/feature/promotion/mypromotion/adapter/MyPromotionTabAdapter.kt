package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.databinding.ItemTagMyPromotionBinding
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDefaults
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.TabChipThemeApplier

class MyPromotionTabAdapter(
    private val titles: List<String>,
    private val onTabSelected: (Int) -> Unit = {},
) : RecyclerView.Adapter<MyPromotionTabAdapter.TabViewHolder>() {

    var selectedPosition: Int = 0
        private set

    override fun getItemCount(): Int = titles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemTagMyPromotionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(titles[position], position == selectedPosition)
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
                notifyItemChanged(old)
                notifyItemChanged(pos)
                onTabSelected(pos)
            }
        }

        fun bind(title: String, selected: Boolean) {
            binding.tvTag.text = title
            val ctx = binding.root.context
            val token = PromotionThemeRegistry.tabChipToken()
                ?: PromotionThemeDefaults.tabChip(ctx)
            TabChipThemeApplier.applyChip(binding.tvTag, selected, token)
        }
    }
}
