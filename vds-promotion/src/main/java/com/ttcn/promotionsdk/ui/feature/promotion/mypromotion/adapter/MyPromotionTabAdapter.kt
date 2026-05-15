package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.ItemTagMyPromotionBinding

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
            if (selected) {
                binding.tvTag.setBackgroundResource(R.drawable.prm_background_4e_corner)
                binding.tvTag.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            } else {
                binding.tvTag.setBackgroundResource(R.drawable.prm_bg_my_promotion_tab_unselected)
                binding.tvTag.setTextColor(ContextCompat.getColor(ctx, R.color.color_7A7A7A))
            }
        }
    }
}
