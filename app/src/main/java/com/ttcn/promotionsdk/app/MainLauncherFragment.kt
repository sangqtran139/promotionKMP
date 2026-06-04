package com.ttcn.promotionsdk.app

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.app.databinding.FragmentMainLauncherBinding
import com.ttcn.promotionsdk.app.mock.promotion.PromotionMockApi
import com.ttcn.promotionsdk.app.theme.ThemePreferenceManager
import com.ttcn.promotionsdk.app.theme.ThemePreviewFragment
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.PromotionSDKOptions
import com.ttcn.promotionsdk.ui.feature.promotion.PaymentIntegrateFragment
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme

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
            if (ENABLE_PROMOTION_MOCK) {
                PromotionMockApi.ensureStarted()
                val mockBaseUrl = PromotionMockApi.baseUrl
                Log.d(TAG, "Mock started, baseUrl=$mockBaseUrl")
                if (PromotionContainer.isInitialized()) {
                    Log.d(TAG, "PromotionContainer initialized, calling PromotionSDK.release()")
                    PromotionSDK.release()
                }
                val savedTheme = ThemePreferenceManager(requireContext()).load()
                PromotionSDK.init(
                    requireContext(),
                    PromotionSDKOptions(
                        config = PromotionSDKConfig(
                            apiKey = "mock-api-key",
                            baseUrl = mockBaseUrl,
                            requestContextProvider = object : PromotionRequestContextProvider {
                                override fun getCustomerId(): String = "CUS-001"
                                override fun getService(): String? = null
                                override fun getAccessToken(): String = "mock-access-token"
                                override fun getLanguage(): String = "vi-VN"
                            },
                        ),
                        theme = savedTheme?.let { PromotionSDKTheme(config = it) }
                            ?: PromotionSDK.getTheme(),
                    ),
                )
                Log.d(TAG, "PromotionSDK.init done with mock baseUrl=$mockBaseUrl")
            }
            Log.d(TAG, "Opening MyPromotion via PromotionSDK.openMyPromotion")
            PromotionSDK.openMyPromotion(requireActivity(), R.id.layoutRoot)
        }
    }

    private companion object {
        const val TAG = "PromotionMockApi"
        const val ENABLE_PROMOTION_MOCK = true
    }
}
