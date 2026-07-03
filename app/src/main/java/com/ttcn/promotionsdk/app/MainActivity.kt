package com.ttcn.promotionsdk.app

import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.ttcn.promotionsdk.app.databinding.LayoutMainBinding
import com.ttcn.promotionsdk.core.config.AvailableService
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.ui.base.PRMBaseActivity
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.PromotionSDKOptions

class MainActivity : PRMBaseActivity<LayoutMainBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater) =
        LayoutMainBinding.inflate(layoutInflater)

    override fun setupUI() {
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = navBarHeight)
            insets
        }

        if (!PromotionContainer.isInitialized()) {
            PromotionSDK.init(
                this,
                PromotionSDKOptions(
                    config = PromotionSDKConfig(
                        apiKey = "demo",
                        baseUrl = "https://staging1.viettelmoney.vn",
                        //token =
                        requestContextProvider = object : PromotionRequestContextProvider {
                            override fun getCustomerId(): String = "CUST-001"
                            override fun getService(): String? = null
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

        if (supportFragmentManager.findFragmentById(R.id.layoutRoot) == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.layoutRoot, MainLauncherFragment())
                .commit()
        }
    }
}
