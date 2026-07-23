package com.ttcn.promotionsdk.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

    // Đi qua wrapper PromotionManager (anti-corruption) — không gọi PRMSDK trực tiếp.
    private fun initSdk(token: String) {
        PromotionManager.start(
            requireContext(),
            customerId = "CUST-001",
            token = token,
            availableServices = listOf(
                AvailableService("P-FOOD-001", "Mua đồ ăn 1", "SKU-FOOD-001", "https://cdn.promix.test/products/food-001.png"),
                AvailableService("P-FOOD-002", "Mua đồ ăn 1", "SKU-FOOD-002", "https://cdn.promix.test/products/food-002.png"),
                AvailableService("P-ALC-001", "Mua rượu", "SKU-ALCOHOL-001", "https://cdn.promix.test/products/alcohol-001.png"),
            ),
        )
    }

    private fun updateDemoContext() {
        PromotionManager.updateContext(
            orderId = "ORD-DEMO-001",
            orderValue = "500000",
            // TEST: để null (khớp iOS demo) — kiểm tra detail có load + nút "Sử dụng ngay" hiện không.
            serviceCode = null,
            metaData = null,
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
