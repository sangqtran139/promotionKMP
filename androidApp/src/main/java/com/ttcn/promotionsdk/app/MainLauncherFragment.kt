package com.ttcn.promotionsdk.app

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ttcn.promotionsdk.app.databinding.FragmentMainLauncherBinding
import com.ttcn.promotionsdk.app.headless.DemoHeadlessFragment
import com.ttcn.promotionsdk.app.theme.ThemePreviewFragment
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.api.PromotionApiResult
import com.ttcn.promotionsdk.app.base.AppBaseFragment
import kotlinx.coroutines.launch

class MainLauncherFragment : AppBaseFragment<FragmentMainLauncherBinding>() {

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
            // Gọi thẳng SDK — không wrapper.
            PromotionSDK.openMyPromotion(requireActivity(), R.id.layoutRoot)
        }

        binding.btnOpenPromotionDetail.setOnClickListener {
            openPromotionDetailDirect()
        }

        // Demo cơ chế refresh: ghi token rác vào kho của app mà KHÔNG báo SDK, rồi mở "My promotion".
        //
        //   request đầu → 401 → SDK gọi DemoTokenSource.refreshToken() → app login lại → báo true
        //                     → SDK tự chạy lại request → màn lên bình thường, KHÔNG toast lỗi
        //
        // Xem logcat tag LoginService để đọc lại toàn bộ chuỗi đó.
        binding.btnExpireToken.setOnClickListener {
            expireTokenForDemo()
            Toast.makeText(
                requireContext(),
                "Token đã hỏng. Mở \"My promotion\" — SDK sẽ tự lấy token mới và thử lại.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-wire mỗi lần màn hiện lại (vd pop từ màn Thanh toán) — sự kiện SDK là 1-1,
        // màn Thanh toán có thể đã chiếm onVoucherApplied/onVoucherCleared. Đối ứng
        // `ViewController.viewWillAppear` bên iOS.
        wirePromotionEvents()
    }

    /** Gán closure lên callback dùng chung — đối ứng `ViewController.wirePromotionEvents` bên iOS. */
    private fun wirePromotionEvents() {
        DemoPromotionCallback.onCountChanged = { count -> Log.d(TAG, "Voucher khả dụng: $count") }
        DemoPromotionCallback.onCleared = { Log.d(TAG, "Voucher đã bị huỷ") }
        // Redemption đã chuyển sang màn Thanh toán (nút "Thanh toán") — màn ngoài không tự redeem nữa.
        DemoPromotionCallback.onApplied = { voucherId -> Log.d(TAG, "Voucher đã áp: $voucherId") }
        // User chọn dịch vụ trong bottom sheet → host tự điều hướng.
        DemoPromotionCallback.onService = { sel ->
            Log.d(TAG, "Đã chọn dịch vụ: ${sel.productName} (${sel.productId}) voucher: ${sel.voucherId}")
        }
        // Chỉ bắn khi `refreshToken` đã báo `false` — phiên chết thật. Có cơ chế refresh chạy đúng
        // thì callback này phải HIẾM; thấy nó nổ đều là dấu hiệu refresh của app đang sai.
        DemoPromotionCallback.onExpired = {
            Log.w(TAG, "Token hết hạn và app không lấy lại được — nên đưa user về màn đăng nhập")
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
            // Headless `api` trả DTO công khai — dùng thẳng, không cần lớp map.
            val voucherId = when (val r = PromotionSDK.api.getVouchers(page = 0)) {
                is PromotionApiResult.Success -> r.data.vouchers.firstOrNull()?.id ?: FALLBACK_VOUCHER_ID
                is PromotionApiResult.Failure -> FALLBACK_VOUCHER_ID
            }
            Log.d(TAG, "openPromotionDetail(voucherId=$voucherId)")
            // `returnVoucherOnApply` mặc định true → nút "Áp dụng" trả voucher về đúng lời gọi này,
            // không qua PromotionSDKCallback singleton. Truyền false nếu muốn SDK tự mở chọn dịch vụ.
            PromotionSDK.openPromotionDetail(
                voucherId = voucherId,
                activity = requireActivity(),
                containerViewId = R.id.layoutRoot,
                onVoucherApplied = { detail ->
                    // Cả object PromotionVoucherDetail — khỏi gọi thêm api.getVoucherDetail().
                    Log.d(TAG, "onVoucherApplied(id=${detail.id}, codes=${detail.codes})")
                    Toast.makeText(
                        requireContext(),
                        "Đã chọn: ${detail.title} — HSD ${detail.expireDate ?: "không giới hạn"}",
                        Toast.LENGTH_LONG,
                    ).show()
                },
            )
        }
    }

    private companion object {
        const val TAG = "PromotionLauncher"

        /** Ngoài đời host đã có sẵn id (payload notification) nên không cần bước fetch ở trên. */
        const val FALLBACK_VOUCHER_ID = "VOUCHER-DEMO-001"
    }
}
