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

        // Điền sẵn DUY NHẤT số của lượt đăng nhập gần nhất (rỗng nếu chưa từng). PIN luôn để trống:
        // `DemoLoginPrefs` cố ý không lưu nó xuống đĩa, và không còn hằng demo nào để điền hộ.
        binding!!.edtMsisdn.setText(DemoLoginPrefs.lastMsisdn(requireContext()))

        // Hai nút, hai vai rõ ràng: "Gửi OTP" gọi API lần 1, "Xác nhận OTP" gọi lần 2 với
        // `requestId` lấy từ lần 1. Tách ra để luôn có đường xác nhận kể cả khi lần 1 hỏng.
        binding!!.btnRetry.setOnClickListener { askOtp() }
        binding!!.btnConfirmOtp.setOnClickListener {
            val requestId = otpRequestId
            if (requestId == null) {
                binding?.tvStatus?.text = "Chưa có requestId — bấm \"Gửi OTP\" trước để server gửi mã."
            } else {
                verifyOtp(requestId)
            }
        }

        // KHÔNG tự `askOtp()` khi mở màn nữa. Số điện thoại giờ do người dùng nhập, mà tự gửi thì:
        //  - mỗi lần mở app là một lượt OTP về số đang điền sẵn, kể cả khi định đăng nhập số khác;
        //  - server đếm số lượt không hoàn tất, quá 5 lần liên tiếp là khoá tài khoản một phút.
        // Người dùng chủ động bấm "Gửi OTP" sau khi đã kiểm số.
    }

    /** `null` = chưa xin OTP hoặc vừa hỏng; khác null = đang chờ người dùng nhập mã. */
    private var otpRequestId: String? = null

    /**
     * Bộ đăng nhập của lượt xin OTP **đang chờ**, chụp lại đúng lúc bấm "Gửi OTP".
     *
     * Không đọc lại từ ô lúc bấm "Xác nhận OTP": server gắn mã với đúng cặp msisdn+PIN đã tạo ra
     * `requestId`, mà giữa hai lần bấm người dùng hoàn toàn có thể sửa ô số. Gửi lệch là mất một
     * lượt trong hạn 5 lần sai mà không hiểu vì sao.
     */
    private var pendingCredentials: Pair<String, String>? = null

    /**
     * Bước 1 — xin OTP. Server gửi mã về số **người dùng vừa nhập** rồi trả `requestId`.
     *
     * Bấm một lần là một lần gửi OTP, và server đếm số lần không hoàn tất (quá 5 lần liên tiếp thì
     * khoá một phút). Vì vậy không tự gọi lại ở bất kỳ nhánh lỗi nào.
     */
    private fun askOtp() {
        val b = binding ?: return
        // Gõ `09…` hay `84…` đều nhận — chuẩn hoá về dạng server đòi (`84…`) ngay tại đây, và ghi
        // ngược lại vào ô để người dùng thấy đúng số sắp được gửi đi.
        val msisdn = normalizeMsisdn(b.edtMsisdn.text?.toString().orEmpty())
        val pin = b.edtPin.text?.toString()?.trim().orEmpty()
        if (msisdn.isEmpty() || pin.isEmpty()) {
            b.tvStatus.text = "Nhập đủ số điện thoại và mã PIN rồi bấm Gửi OTP"
            return
        }
        b.edtMsisdn.setText(msisdn)

        // Đổi số so với lượt trước → `requestId` cũ thuộc về tài khoản khác, dùng lại là gửi OTP của
        // người này sang phiên của người kia. Vứt đi để bước 1 xin phiên mới hẳn.
        if (pendingCredentials?.first != msisdn) otpRequestId = null
        pendingCredentials = msisdn to pin

        // GIỮ `otpRequestId` cũ (cùng số), không xoá: nếu server không cấp mã mới (OTP trước còn
        // hiệu lực) thì cái đang giữ vẫn là đường duy nhất để gọi bước 2.
        b.tvStatus.text = "Đang gửi OTP tới $msisdn"
        b.progressBar.visibility = View.VISIBLE
        b.btnRetry.visibility = View.GONE

        // Lấy context TRƯỚC khi vào coroutine: người dùng bấm back trong lúc chờ mạng là fragment
        // detach, và `requireContext()` sau `await` sẽ ném `IllegalStateException`.
        // `applicationContext` để không giữ Activity sống theo lời gọi.
        val appContext = requireContext().applicationContext

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val challenge = LoginService().requestOtp(
                    msisdn = msisdn,
                    pin = pin,
                    previousRequestId = otpRequestId,
                )
                // Nhớ số SAU khi server nhận — số gõ sai thì không đáng điền sẵn cho lần sau.
                DemoLoginPrefs.rememberMsisdn(appContext, msisdn)
                when (challenge) {
                    is OtpChallenge.Token -> finishLogin(challenge.accessToken)
                    is OtpChallenge.NeedOtp -> {
                        otpRequestId = challenge.requestId
                        binding?.apply {
                            tvStatus.text = challenge.message
                            progressBar.visibility = View.GONE
                            edtOtp.text?.clear()
                            edtOtp.requestFocus()
                            btnRetry.text = "Gửi lại OTP"
                            btnRetry.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                showFailure(e)
            }
        }
    }

    /** Bước 2 — gửi mã người dùng vừa nhập. */
    private fun verifyOtp(requestId: String) {
        val b = binding ?: return
        val otp = b.edtOtp.text?.toString()?.trim().orEmpty()
        if (otp.isEmpty()) {
            b.tvStatus.text = "Nhập mã OTP đã nhận rồi bấm Xác nhận OTP"
            return
        }
        val (msisdn, pin) = pendingCredentials ?: run {
            b.tvStatus.text = "Bấm \"Gửi OTP\" trước để server gửi mã."
            return
        }

        b.tvStatus.text = "Đang xác thực OTP"
        b.progressBar.visibility = View.VISIBLE
        b.btnRetry.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                finishLogin(LoginService().submitOtp(requestId, otp, msisdn = msisdn, pin = pin))
            } catch (e: Exception) {
                // Ở LẠI màn nhập OTP: mã có thể chỉ gõ nhầm, xin mã mới là tốn thêm một lượt
                // trong hạn 5 lần. Người dùng sửa rồi bấm lại.
                Log.e(TAG, "Xác thực OTP thất bại: ${e.message}")
                binding?.apply {
                    tvStatus.text = e.message ?: "Xác thực OTP thất bại"
                    progressBar.visibility = View.GONE
                    btnRetry.text = "Gửi lại OTP"
                    btnRetry.visibility = View.VISIBLE
                }
            }
        }
    }

    private suspend fun finishLogin(token: String) {
        demoAccessToken = token
        Log.d(TAG, "accessToken: $token")

        initSdk()
        updateDemoContext()

        binding?.apply {
            tvStatus.text = "Lấy token thành công"
            progressBar.visibility = View.GONE
        }
        delay(500)
        navigateToLauncher()
    }

    private fun showFailure(e: Exception) {
        Log.e(TAG, "Login failed: ${e.message}")
        // KHÔNG xoá `otpRequestId`: bước 1 hỏng không làm mã đã gửi mất hiệu lực.
        binding?.apply {
            tvStatus.text = e.message ?: "Lấy token thất bại"
            progressBar.visibility = View.GONE
            // "Gửi lại OTP" chỉ đúng khi đã từng có mã được gửi. Từ khi PIN do người dùng nhập,
            // bước 1 còn hỏng vì sai PIN — lúc đó chưa có OTP nào để mà "gửi lại", và chữ đó khiến
            // người dùng tưởng mã đã về máy rồi ngồi chờ tin nhắn không bao giờ tới.
            btnRetry.text = if (otpRequestId == null) "Gửi OTP" else "Gửi lại OTP"
            btnRetry.visibility = View.VISIBLE
        }
    }

    // Gọi THẲNG PromotionSDK — không qua wrapper.
    // `updateSession` đã bị bỏ: **lúc nào vào app cũng initialize() lại**, lần nào cũng áp đủ cấu
    // hình host truyền (kể cả baseUrl/environment/language). SDK không còn khoá field nào.
    // `tokenSource` là cách DUY NHẤT token đi vào SDK — không có tham số `accessToken` nào nữa.
    // [DemoTokenSource] (ở LoginService.kt) cài đặt cả hai hàm: `currentToken()` cho mọi request, và
    // `refreshToken()` cho lúc SDK ăn 401. Xem KDoc của nó.
    private fun initSdk() {
        PromotionSDK.initialize(
            context = requireContext(),
            tokenSource = DemoTokenSource,
            baseUrl = DEMO_BASE_URL,
            availableServices = demoServices,
            callback = DemoPromotionCallback,
        )
    }

    /**
     * Danh mục dịch vụ HOST cung cấp cho bottom sheet "Chọn dịch vụ" — đối ứng `demoServices` bên iOS.
     *
     * `productId` phải khớp **`applicableProducts.productId`** của voucher (`sku` KHÔNG được dùng
     * để so — xem `servicesForApplicableProducts`), nên ở đây là UUID chứ không phải mã "P-FOOD-001".
     *
     * Một `productId` gắn nhiều SKU (vd `…0011` = BH 2 chiều + gói doanh nghiệp, `…0003` = V120 + V90)
     * nhưng list bị `distinctBy { productId }` → khai **một dòng mỗi productId**, tên gộp các SKU.
     * Khai theo từng SKU thì dòng thứ hai bị loại âm thầm.
     */
    private val demoServices = listOf(
        PromotionAvailableService("P-ALC-001", "Data Viettel MIMAX125 - không giới hạn", "TELCO", "https://picsum.photos/seed/mimax125/96"),
        PromotionAvailableService("P-BILL-001", "Gói cước V120 / V90", "TELCO", "https://picsum.photos/seed/goicuoc/96"),
        PromotionAvailableService("P-FOOD-001", "BH xe máy Vespa 1 năm", "INSURANCE", "https://picsum.photos/seed/vespa/96"),
        PromotionAvailableService("P-FOOD-002", "BH ô tô", "INSURANCE", "https://picsum.photos/seed/bhoto/96"),
        PromotionAvailableService("P-FOOD-003", "Combo đồ uống đóng chai", "BEVERAGE", "https://picsum.photos/seed/douong/96"),
    )

    private fun updateDemoContext() {
        PromotionSDK.updateOrderInfo(
            orderId = "ORD-DEMO-001",
            productId = "TKBAOVIET",
            orderValue = "500000",
            metaData = "channel=MOBILE_APP",
            productName = "Tài khoản Bảo Việt",
            productCategory = "INSURANCE",
            quantity = 1,
            unitPrice = "500000",
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
