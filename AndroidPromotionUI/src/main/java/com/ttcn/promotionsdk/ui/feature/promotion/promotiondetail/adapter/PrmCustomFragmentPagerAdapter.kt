package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.adapter

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PrmCustomFragmentPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = arrayListOf<Fragment>()
    private val titles = arrayListOf<String>()
    private val fragmentIcons = arrayListOf<Drawable?>()

    fun addFragment(fragment: Fragment, title: String) {
        fragments.add(fragment)
        titles.add(title)
    }

    fun addFragmentIcon(fragment: Fragment, title: String, icon: Drawable?) {
        fragments.add(fragment)
        titles.add(title)
        fragmentIcons.add(icon)
    }

    fun getTitle(position: Int): String {
        return titles[position]
    }

    fun getFragmentIcon(position: Int): Drawable? {
        return fragmentIcons.getOrNull(position)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}