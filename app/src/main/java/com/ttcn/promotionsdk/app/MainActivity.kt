package com.ttcn.promotionsdk.app

import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import com.ttcn.promotionsdk.app.databinding.LayoutMainBinding
import com.ttcn.promotionsdk.ui.presentation.promotion.myendow.PaymentIntegrateFragment
import com.vds.vdsinappmessage.base.PRMBaseActivity

class MainActivity : PRMBaseActivity<ViewModel, LayoutMainBinding>() {

    override val layoutId: Int
        get() = R.layout.layout_main

    override fun init() {
        enableEdgeToEdge()
        addFragment(
            PaymentIntegrateFragment.newInstance(),
            R.id.layoutRoot
        )
    }
}
