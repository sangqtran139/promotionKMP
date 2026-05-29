package com.ttcn.promotionsdk.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ttcn.promotionsdk.app.databinding.FragmentMainLauncherBinding
import com.ttcn.promotionsdk.app.theme.ThemePreviewFragment
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.feature.promotion.PaymentIntegrateFragment

class MainLauncherFragment : PRMBaseFragment<FragmentMainLauncherBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMainLauncherBinding.inflate(inflater, container, false)

    override fun setupUI() {

        binding.btnCustomView.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.layoutRoot, ThemePreviewFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnOpenPaymentIntegrate.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.layoutRoot, PaymentIntegrateFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnOpenMyPromotion.setOnClickListener {
            PromotionSDK.openMyPromotion(requireActivity(), R.id.layoutRoot)
        }
    }
}
