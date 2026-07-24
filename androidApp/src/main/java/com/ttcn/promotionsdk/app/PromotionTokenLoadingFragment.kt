package com.ttcn.promotionsdk.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ttcn.prm.entry.PromotionAvailableService
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.PromotionSDKCallback
import com.ttcn.prm.entry.PromotionServiceSelection
import com.ttcn.promotionsdk.app.databinding.FragmentTokenLoadingBinding
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
                updateDemoContext()

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

    // Gọi THẲNG PromotionSDK — không qua wrapper.
    // Lần đầu: initialize(...) đầy đủ (chốt field cố định baseUrl/environment/language/theme).
    // Login lại (đã init rồi): chỉ updateSession(...) với field động — không lặp lại config cố định.
    private fun initSdk(token: String) {
        if (PromotionSDK.isInitialized()) {
            PromotionSDK.updateSession(
                customerId = "CUST-001",
                accessToken = token,
                availableServices = demoServices,
            )
        } else {
            PromotionSDK.initialize(
                context = requireContext(),
                customerId = "CUST-001",
                accessToken = token,
                baseUrl = DEMO_BASE_URL,
                availableServices = demoServices,
                callback = demoCallback,
            )
        }
    }

    private val demoServices = listOf(
        PromotionAvailableService("P-FOOD-001", "Mua đồ ăn 1", "SKU-FOOD-001", "https://cdn.promix.test/products/food-001.png"),
        PromotionAvailableService("P-FOOD-002", "Mua đồ ăn 1", "SKU-FOOD-002", "https://cdn.promix.test/products/food-002.png"),
        PromotionAvailableService("P-ALC-001", "Mua rượu", "SKU-ALCOHOL-001", "https://cdn.promix.test/products/alcohol-001.png"),
    )

    private fun updateDemoContext() {
        PromotionSDK.updateContext(
            orderId = "ORD-DEMO-001",
            orderValue = "500000",
            // TEST: để null (khớp iOS demo) — kiểm tra detail có load + nút "Sử dụng ngay" hiện không.
            serviceCode = null,
            metaData = null,
        )
    }

    /** Host chỉ implement sự kiện mình cần — các method khác có default rỗng. */
    private val demoCallback = object : PromotionSDKCallback {
        override fun onServiceSelected(selection: PromotionServiceSelection) {
            Log.d(TAG, "onServiceSelected: ${selection.serviceName} (voucher ${selection.voucherId})")
        }
        override fun onVoucherApplied(voucherId: String) { Log.d(TAG, "onVoucherApplied: $voucherId") }
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
        const val DEMO_BASE_URL = "http://125.235.38.229:8080"
    }
}
