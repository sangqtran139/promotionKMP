package com.ttcn.promotionsdk.app

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.ttcn.promotionsdk.app.databinding.FragmentMainLauncherBinding
import com.ttcn.promotionsdk.app.headless.DemoHeadlessFragment
import com.ttcn.promotionsdk.app.theme.ThemePreviewFragment
import com.ttcn.prm.ui.base.PRMBaseFragment
import kotlinx.coroutines.launch

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
            Log.d(TAG, "Opening MyPromotion via PromotionManager")
            PromotionManager.openMyPromotions(requireActivity(), R.id.layoutRoot)
        }

        binding.btnOpenPromotionDetail.setOnClickListener {
            Log.d(TAG, "Opening PromotionDetail via PromotionManager")
            openPromotionDetailDirect()
        }
    }

    /**
     * Mở thẳng màn chi tiết, KHÔNG qua danh sách — mô phỏng host bấm push notification / deeplink.
     *
     * Ưu tiên voucherId thật (voucher đầu tiên của khách) để màn có dữ liệu đầy đủ; khách chưa có
     * voucher hoặc API lỗi thì **vẫn vào** bằng [FALLBACK_VOUCHER_ID] — mục đích của nút là xem được
     * màn chi tiết, không phải kiểm tra API. Đối xứng với nút bên demo iOS.
     */
    private fun openPromotionDetailDirect() {
        viewLifecycleOwner.lifecycleScope.launch {
            val voucherId = PromotionManager.fetchVouchers(null, null, null, 0)
                .getOrNull()?.mine?.firstOrNull()?.id ?: FALLBACK_VOUCHER_ID

            Log.d(TAG, "openPromotionDetail(voucherId=$voucherId)")
            PromotionManager.openPromotionDetail(voucherId, requireActivity(), R.id.layoutRoot)
        }
    }

    private companion object {
        const val TAG = "PromotionLauncher"

        /** Ngoài đời host đã có sẵn id (payload notification) nên không cần bước fetch ở trên. */
        const val FALLBACK_VOUCHER_ID = "VOUCHER-DEMO-001"
    }
}
