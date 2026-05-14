package com.ttcn.promotionsdk.app

import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import com.ttcn.promotionsdk.app.databinding.LayoutMainBinding
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

        if (!PromotionContainer.isInitialized()) {
            PromotionSDK.init(
                this,
                PromotionSDKOptions(
                    config = PromotionSDKConfig(
                        apiKey = "demo",
                        baseUrl = "https://example.com"
                    )
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
