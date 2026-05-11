package com.ttcn.promotionsdk.app

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.ttcn.promotionsdk.databinding.ItemMyEndowBinding
import com.ttcn.promotionsdk.databinding.ItemTitleMyEndowBinding

class PromotionItem() : AbstractBindingItem<ItemMyEndowBinding>() {

    override val type: Int
        get() = R.id.item_promotion_id

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ItemMyEndowBinding {
        return ItemMyEndowBinding.inflate(inflater, parent, false)
    }

    override fun bindView(
        binding: ItemMyEndowBinding,
        payloads: List<Any>
    ) {
        binding.tvStore.text = "Viettel"
        binding.tvContent.text =  "a;sjfjhilufhjlisd"
        binding.tvEndDate.text = "HSD 15/05/20000"
    }
}

class TextItem(
    val titleEndow: String = ""
) : AbstractBindingItem<ItemTitleMyEndowBinding>() {

    override val type: Int
        get() = R.id.item_title_id

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ItemTitleMyEndowBinding {
        return ItemTitleMyEndowBinding.inflate(inflater, parent, false)
    }

    override fun bindView(
        binding: ItemTitleMyEndowBinding,
        payloads: List<Any>
    ) {
        binding.txtTitleEndow.text = titleEndow
    }
}