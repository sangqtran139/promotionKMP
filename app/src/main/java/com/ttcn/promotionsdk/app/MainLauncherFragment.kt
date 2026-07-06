package com.ttcn.promotionsdk.app

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ttcn.promotionsdk.app.databinding.FragmentMainLauncherBinding
import com.ttcn.promotionsdk.app.headless.DemoHeadlessFragment
import com.ttcn.promotionsdk.app.mock.promotion.PromotionMockApi
import com.ttcn.promotionsdk.app.theme.ThemePreferenceManager
import com.ttcn.promotionsdk.app.theme.ThemePreviewFragment
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.config.SdkEnvironment
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.PromotionSDKOptions
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
            if (ENABLE_PROMOTION_MOCK) {
                PromotionMockApi.ensureStarted()
                if (PromotionContainer.isInitialized()) {
                    Log.d(TAG, "PromotionContainer initialized, calling PromotionSDK.release()")
                    PromotionSDK.release()
                }
                initPromotionSdk(
                    baseUrl = PromotionMockApi.baseUrl,
                    customerId = MOCK_CUSTOMER_ID,
                    accessToken = MOCK_ACCESS_TOKEN,
                    apiKey = MOCK_API_KEY,
                    environment = SdkEnvironment.PROD,
                )
            } else {
                ensureRealPromotionSdkInitialized()
            }

            Log.d(TAG, "Opening MyPromotion via PromotionSDK.openMyPromotion")
            PromotionSDK.openMyPromotion(requireActivity(), R.id.layoutRoot)
        }
    }

    private fun ensureRealPromotionSdkInitialized() {
        val missingCustomerId = PromotionContainer.isInitialized() &&
            PromotionContainer.requestContextProvider.getCustomerId().isNullOrBlank()
        if (missingCustomerId) {
            Log.d(TAG, "SDK initialized without customerId, calling PromotionSDK.release()")
            PromotionSDK.release()
        }
        if (!PromotionContainer.isInitialized()) {
            initPromotionSdk(
                baseUrl = REAL_PROMOTION_BASE_URL,
                customerId = REAL_CUSTOMER_ID,
                accessToken = REAL_ACCESS_TOKEN,
                apiKey = REAL_API_KEY,
                environment = SdkEnvironment.STAGING,
            )
        }
    }

    private fun initPromotionSdk(
        baseUrl: String,
        customerId: String,
        accessToken: String,
        apiKey: String,
        environment: SdkEnvironment,
    ) {
        Log.d(TAG, "PromotionSDK.init with baseUrl=$baseUrl customerId=$customerId")
        val savedTheme = ThemePreferenceManager(requireContext()).load()
        PromotionSDK.init(
            requireContext(),
            PromotionSDKOptions(
                config = PromotionSDKConfig(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    environment = environment,
                    requestContextProvider = object : PromotionRequestContextProvider {
                        override fun getCustomerId(): String = customerId

                        override fun getService(): String? = null

                        override fun getAccessToken(): String = accessToken

                        override fun getLanguage(): String = "vi-VN"
                    },
                ),
                theme = savedTheme?.let(PromotionSDKTheme::from) ?: PromotionSDK.getTheme(),
            ),
        )
        Log.d(TAG, "PromotionSDK.init done with baseUrl=$baseUrl")
    }

    private companion object {
        const val TAG = "PromotionLauncher"

        const val ENABLE_PROMOTION_MOCK = false

        private const val MOCK_CUSTOMER_ID = "CUS-001"
        private const val MOCK_ACCESS_TOKEN = "mock-access-token"
        private const val MOCK_API_KEY = "mock-api-key"

        private const val REAL_PROMOTION_BASE_URL = "https://staging1.viettelmoney.vn/"
        private const val REAL_CUSTOMER_ID = "5"
        private const val REAL_ACCESS_TOKEN = ""
        private const val REAL_API_KEY = "staging-api-key"
    }
}
