//
//  CheckoutViewController.swift
//  PromotionSDKDemo
//
//  Màn "Thanh toán" của host: nhúng widget chọn ưu đãi (custom view của SDK) + nút
//  "Thanh toán" gọi `PromotionSDK.confirmRedemption`.
//
//  Luồng: user áp voucher trên widget → SDK giữ state → bấm "Thanh toán" →
//  `PromotionSDK.confirmRedemption`. Đối ứng `binding.offerWidget.confirmRedemption(...)` bên Android
//  (docs/features/OfferWidget.md §2).
//
//  **Đây là UI mode** — host nhúng widget thì SDK giữ danh sách ưu đãi đang áp, host KHÔNG tự nhớ.
//  Trước đây màn này cache `appliedVoucherId` từ callback `onVoucherApplied` rồi gọi API headless
//  `PromotionSDK.api.createRedemption`, tức trộn hai chế độ tích hợp. Bốn hệ quả, đều đã bỏ:
//
//    1. Callback chỉ bắn theo TRANSITION sang APPLIED. Màn này dựng lại trong khi store vẫn đang áp
//       → cache `nil` mà callback không bắn lại → báo "Chưa áp dụng ưu đãi" trong khi widget hiện
//       rành rành là đã áp. Đổi sang voucher khác lúc đang APPLIED cũng không có transition → cache
//       giữ id CŨ → redeem nhầm voucher.
//    2. Chưa áp ưu đãi nào thì spec bảo cho đi tiếp (`onSuccess` ngay, không gọi mạng) — bản cũ chặn
//       bằng alert.
//    3. `INSUFFICIENT_BUDGET`: `confirmRedemption` tự validate lại để widget hiện giá mới rồi mới
//       báo lỗi. Đường headless không có bước đó, widget đứng im với giá cũ.
//    4. Đơn nhiều voucher: bản cũ gửi đúng MỘT (`[voucherId]`), nay gửi hết những gì đang áp.
//
//  `PromotionSDK.api.*` (headless) là dành cho host **tự dựng UI**, không nhúng widget — xem
//  DemoHeadlessViewController.
//

import UIKit
import PromotionKit

final class CheckoutViewController: UIViewController {

    private let orderId: String
    private let orderValue: String

    private let payButton: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle("Thanh toán", for: .normal)
        btn.titleLabel?.font = .systemFont(ofSize: 17, weight: .semibold)
        btn.backgroundColor = .systemGreen
        btn.setTitleColor(.white, for: .normal)
        btn.layer.cornerRadius = 12
        btn.translatesAutoresizingMaskIntoConstraints = false
        return btn
    }()

    init(orderId: String, orderValue: String) {
        self.orderId = orderId
        self.orderValue = orderValue
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Thanh toán"
        view.backgroundColor = .systemBackground
        setupLayout()
        payButton.addTarget(self, action: #selector(payTapped), for: .touchUpInside)
    }

    // Không cần nghe `onVoucherApplied` / `onVoucherCleared` ở đây nữa: state ưu đãi đang áp do SDK
    // giữ, `confirmRedemption` đọc thẳng lúc bấm. Hai callback đó vẫn là API thật của SDK (báo host
    // cập nhật tổng tiền, badge…) — xem ViewController.swift để biết cách nghe.

    // MARK: - Layout

    private func setupLayout() {
        // Widget chọn ưu đãi (custom view SDK) — truyền order tại đây, không re-init SDK.
        let widget = PromotionSDK.createOfferWidget(from: self, orderId: orderId, orderValue: orderValue)
        widget.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(widget)
        view.addSubview(payButton)

        NSLayoutConstraint.activate([
            widget.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            widget.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            widget.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24),

            payButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            payButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            payButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            payButton.heightAnchor.constraint(equalToConstant: 52)
        ])
    }

    // MARK: - Actions

    /// KHÔNG chặn khi chưa áp ưu đãi: `confirmRedemption` tự trả `onSuccess` ngay và không gọi mạng
    /// (docs/features/OfferWidget.md §2, bước 1). SDK không có quyền chặn thanh toán của host — đơn
    /// không dùng ưu đãi vẫn phải đi tiếp được.
    @objc private func payTapped() {
        payButton.isEnabled = false
        PromotionSDK.confirmRedemption(
            onSuccess: { [weak self] in
                self?.payButton.isEnabled = true
                self?.proceedPayment()
            },
            onError: { [weak self] error in
                self?.payButton.isEnabled = true
                // `FeatureDisabled`: SDK đã tự hiện popup — host chỉ dừng luồng, đừng báo lần hai.
                if case .featureDisabled = error { return }
                self?.showAlert("Thanh toán lỗi", error.errorDescription ?? "Đã có lỗi xảy ra.")
            }
        )
    }

    /// Chỗ host chạy tiếp luồng thanh toán thật của mình. Đối ứng `proceedPayment()` bên Android.
    private func proceedPayment() {
        showAlert("Thanh toán thành công", "Đã tạo phiên redemption cho các ưu đãi đang áp.")
    }

    // MARK: - Helpers

    /// SDK chỉ trả **mã lỗi**; câu hiển thị là chuỗi của host. Đối ứng `mapErrorMessage` bên Android.
    private static func errorMessage(_ code: String) -> String {
        switch code {
        case "missing_customer_id": return "Thiếu thông tin khách hàng."
        case "INSUFFICIENT_BUDGET": return "Ưu đãi đã hết ngân sách. Số tiền giảm trên widget vừa được cập nhật lại."
        default: return "Có lỗi xảy ra, vui lòng thử lại."
        }
    }

    private func showAlert(_ title: String, _ message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
