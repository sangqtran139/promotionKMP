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
                val token = LoginService().login()
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
                accessToken = token,
                availableServices = demoServices,
            )
        } else {
            PromotionSDK.initialize(
                context = requireContext(),
                accessToken = token,
                baseUrl = DEMO_BASE_URL,
                availableServices = demoServices,
                callback = DemoPromotionCallback,
            )
        }
    }

    /**
     * Danh mục dịch vụ HOST cung cấp cho bottom sheet "Chọn dịch vụ" — đối ứng `demoServices` bên iOS.
     *
     * `serviceCode` phải khớp **`applicableProducts.productId`** của voucher (`sku` KHÔNG được dùng
     * để so — xem `servicesForApplicableProducts`), nên ở đây là UUID chứ không phải mã "P-FOOD-001".
     *
     * Một `productId` gắn nhiều SKU (vd `…0011` = BH 2 chiều + gói doanh nghiệp, `…0003` = V120 + V90)
     * nhưng list bị `distinctBy { serviceCode }` → khai **một dòng mỗi productId**, tên gộp các SKU.
     * Khai theo từng SKU thì dòng thứ hai bị loại âm thầm.
     */
    private val demoServices = listOf(
        PromotionAvailableService("019a7000-0002-0000-0000-000000000002", "Data Viettel MIMAX125 - không giới hạn", "TELCO", "https://picsum.photos/seed/mimax125/96"),
        PromotionAvailableService("019a7000-0002-0000-0000-000000000003", "Gói cước V120 / V90", "TELCO", "https://picsum.photos/seed/goicuoc/96"),
        PromotionAvailableService("019a7000-0002-0000-0000-000000000010", "BH xe máy Vespa 1 năm", "INSURANCE", "https://picsum.photos/seed/vespa/96"),
        PromotionAvailableService("019a7000-0002-0000-0000-000000000011", "BH ô tô (2 chiều / doanh nghiệp)", "INSURANCE", "https://picsum.photos/seed/bhoto/96"),
        PromotionAvailableService("019a7000-0002-0000-0000-000000000013", "Combo đồ uống đóng chai", "BEVERAGE", "https://picsum.photos/seed/douong/96"),
    )

    private fun updateDemoContext() {
        PromotionSDK.updateContext(
            orderId = "ORD-DEMO-001",
            orderValue = "500000",
            // TEST: để null (khớp iOS demo) — kiểm tra detail có load + nút "Sử dụng ngay" hiện không.
            serviceCode = "TKBAOVIET",
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
        const val DEMO_BASE_URL = "http://125.235.38.229:8080"
    }
}
