package com.ttcn.promotionsdk.app

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.app.databinding.FragmentMainLauncherBinding
import com.ttcn.promotionsdk.app.headless.DemoHeadlessFragment
import com.ttcn.promotionsdk.app.theme.ThemePreviewFragment
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.entry.PromotionSDK

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
                .replace(R.id.layoutRoot, DemoPaymentIntegrateFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnHeadlessDemo.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.layoutRoot, DemoHeadlessFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        binding.btnOpenMyPromotion.setOnClickListener {
            Log.d(TAG, "Opening MyPromotion via PromotionSDK.openMyPromotion")
            PromotionSDK.openMyPromotion(requireActivity(), R.id.layoutRoot)
        }
    }

    private companion object {
        const val TAG = "PromotionLauncher"
    }
}
