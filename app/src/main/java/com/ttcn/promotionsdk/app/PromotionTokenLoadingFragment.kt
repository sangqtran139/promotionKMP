package com.ttcn.promotionsdk.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ttcn.promotionsdk.app.databinding.FragmentTokenLoadingBinding
import com.ttcn.promotionsdk.core.config.AvailableService
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.PromotionSDKOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PromotionTokenLoadingFragment : Fragment() {

    private var binding: FragmentTokenLoadingBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTokenLoadingBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.btnRetry.setOnClickListener { startLogin() }
        startLogin()
    }

    private fun startLogin() {
        val b = binding ?: return
        b.tvStatus.text = "Đang lấy token đăng nhập"
        b.progressBar.visibility = View.VISIBLE
        b.btnRetry.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = PromotionTestLoginManager().loginAndGetAccessToken()
                Log.d(TAG, "accessToken: $token")

                initSdk(token)

                binding?.tvStatus?.text = "Lấy token thành công"
                delay(500)
                navigateToLauncher()
            } catch (e: Exception) {
                Log.e(TAG, "Login failed: ${e.message}")
                binding?.tvStatus?.text = e.message ?: "Lấy token thất bại"
                binding?.progressBar?.visibility = View.GONE
                binding?.btnRetry?.visibility = View.VISIBLE
            }
        }
    }

    private fun initSdk(token: String) {
        if (PromotionContainer.isInitialized()) PromotionSDK.release()
        PromotionSDK.init(
            requireContext(),
            PromotionSDKOptions(
                config = PromotionSDKConfig(
                    apiKey = "demo",
                    baseUrl = "https://staging1.viettelmoney.vn",
                    requestContextProvider = object : PromotionRequestContextProvider {
                        override fun getCustomerId(): String = "CUST-001"
                        override fun getService(): String? = null
                        override fun getAccessToken(): String = token
                        override fun getLanguage(): String = "vi-VN"
                        override fun getOrderId(): String? = "123"
                        override fun getOrderValue(): String? = "123"
                    },
                    availableServices = listOf(
                        AvailableService(
                            serviceCode = "P-FOOD-001",
                            serviceName = "Mua đồ ăn 1",
                            serviceType = "SKU-FOOD-001",
                            iconUrl = "https://cdn.promix.test/products/food-001.png"
                        ),
                        AvailableService(
                            serviceCode = "P-FOOD-002",
                            serviceName = "Mua đồ ăn 1",
                            serviceType = "SKU-FOOD-002",
                            iconUrl = "https://cdn.promix.test/products/food-002.png"
                        ),
                        AvailableService(
                            serviceCode = "P-ALC-001",
                            serviceName = "Mua rượu",
                            serviceType = "SKU-ALCOHOL-001",
                            iconUrl = "https://cdn.promix.test/products/alcohol-001.png"
                        ),
                    ),
                ),
            )
        )
    }

    private fun navigateToLauncher() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.layoutRoot, MainLauncherFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private companion object {
        const val TAG = "TokenLoading"
    }
}
