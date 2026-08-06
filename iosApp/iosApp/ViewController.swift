//
//  ViewController.swift
//  PromotionSDKDemo
//
//  Demo tích hợp PromotionSDK — gọi THẲNG `PromotionSDK` (không wrapper). Sự kiện đi qua
//  `DemoPromotionCallback` (tiện ích demo ~30 dòng cho nhiều màn nghe, không bắt buộc với host).
//

import UIKit
import PRM

class ViewController: UIViewController {

    // MARK: - Demo data (giả lập host cung cấp)
    //
    // Login + `PromotionSDK.initialize(...)` nằm ở `TokenLoadingViewController` (cổng khởi động) —
    // màn này chỉ hiện SAU KHI SDK đã initialize, nên không cần gác `isReady` ở từng nút.

    /// Đơn hàng ở màn thanh toán — truyền vào widget để validate voucher khi "Áp dụng".
    private let demoOrderId = "ORDER-001"
    private let demoOrderValue = "500000"
    /// Id dự phòng cho nút "Mở thẳng chi tiết": khách chưa có voucher, hoặc API lỗi, vẫn vào được màn
    /// chi tiết để xem layout. Màn tự fetch theo id này rồi hiện shimmer → lỗi. Chỉ dùng ở demo.
    private let fallbackVoucherId = "VOUCHER-DEMO-001"

    // MARK: - UI

    private let stackView: UIStackView = {
        let sv = UIStackView()
        sv.axis = .vertical
        sv.spacing = 12
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Demo PromotionSDK"
        view.backgroundColor = .systemBackground
        setupLayout()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // Re-wire mỗi lần màn hiện lại (vd pop từ màn Thanh toán) — sự kiện SDK là 1-1,
        // màn Thanh toán có thể đã chiếm onVoucherApplied/onVoucherCleared.
        wirePromotionEvents()
    }

    private func wirePromotionEvents() {
        // Sự kiện SDK là 1-1: gán closure lên callback dùng chung (màn hiện chiếm quyền nghe).
        let events = DemoPromotionCallback.shared
        events.onCountChanged = { count in
            print("[Demo] Voucher khả dụng: \(count)")
        }
        events.onCleared = {
            print("[Demo] Voucher đã bị huỷ")
        }
        // Redemption đã chuyển sang màn Thanh toán (nút "Thanh toán") — màn ngoài không tự redeem nữa.
        events.onApplied = { voucherId in
            print("[Demo] Voucher đã áp: \(voucherId)")
        }
        // User chọn dịch vụ trong bottom sheet → host tự điều hướng.
        events.onService = { [weak self] sel in
            self?.showAlert("Đã chọn dịch vụ", "\(sel.serviceName) (\(sel.serviceCode))\nvoucher: \(sel.voucherId)")
        }
    }

    // MARK: - Layout

    private func setupLayout() {
        view.addSubview(stackView)
        NSLayoutConstraint.activate([
            stackView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            stackView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            stackView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24)
        ])

        stackView.addArrangedSubview(makeButton("Xem danh sách ưu đãi (UI)", action: #selector(showMyPromotionsTapped)))
        stackView.addArrangedSubview(makeButton("🎟  Mở thẳng chi tiết ưu đãi", action: #selector(openPromotionDetailTapped), color: .systemTeal))
        stackView.addArrangedSubview(makeButton("🛒  Mở màn thanh toán", action: #selector(openCheckoutTapped), color: .systemGreen))
        stackView.addArrangedSubview(makeSeparator())
        stackView.addArrangedSubview(makeButton("▶  Headless API Demo", action: #selector(openHeadlessDemoTapped), color: .systemIndigo))
        stackView.addArrangedSubview(makeButton("🎨  Theme Playground (đổi màu từng item)", action: #selector(openThemePlaygroundTapped), color: .systemPurple))
    }

    private func makeButton(_ title: String, action: Selector, color: UIColor = .systemBlue) -> UIButton {
        let btn = UIButton(type: .system)
        btn.setTitle(title, for: .normal)
        btn.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        btn.backgroundColor = color
        btn.setTitleColor(.white, for: .normal)
        btn.layer.cornerRadius = 10
        btn.heightAnchor.constraint(equalToConstant: 44).isActive = true
        btn.addTarget(self, action: action, for: .touchUpInside)
        return btn
    }

    private func makeSeparator() -> UIView {
        let v = UIView()
        v.backgroundColor = .separator
        v.heightAnchor.constraint(equalToConstant: 1).isActive = true
        return v
    }

    // MARK: - Actions (gọi thẳng PromotionSDK — không wrapper)

    @objc private func showMyPromotionsTapped() {
        PromotionSDK.openMyPromotion(from: self)
    }

    /// Mở thẳng màn chi tiết, KHÔNG qua danh sách — mô phỏng host bấm vào push notification/deeplink.
    ///
    /// Ưu tiên `voucherId` thật (voucher đầu tiên của khách) để màn hiển thị dữ liệu đầy đủ. Nhưng
    /// khách chưa có voucher, hoặc API lỗi, thì **vẫn vào** bằng `fallbackVoucherId` — mục đích của
    /// nút này là xem được màn chi tiết, không phải kiểm tra API. Ngoài đời host đã có sẵn id
    /// (trong payload notification) nên không cần bước fetch này.
    @objc private func openPromotionDetailTapped() {
        // Headless `api` trả DTO công khai — dùng thẳng, không cần lớp map.
        PromotionSDK.api.getVouchers(page: 0) { [weak self] result in
            guard let self else { return }
            let voucherId = (try? result.get())?.vouchers.first?.id ?? self.fallbackVoucherId
            // `returnVoucherOnApply` mặc định true → nút "Áp dụng" trả voucher về đúng lời gọi này,
            // không qua PromotionSDKCallback singleton. Truyền false nếu muốn SDK tự mở chọn dịch vụ.
            PromotionSDK.openPromotionDetail(voucherId: voucherId, from: self) { [weak self] detail in
                // Cả object PromotionVoucherDetail — khỏi gọi thêm api.getVoucherDetail().
                // Màn chi tiết đã tự pop → toast hiện trên chính màn này, đối ứng demo Android.
                let expiry = detail.expireDate ?? "không giới hạn"
                self?.showToast("Đã chọn: \(detail.title) — HSD \(expiry)")
            }
        }
    }

    @objc private func openCheckoutTapped() {
        navigationController?.pushViewController(
            CheckoutViewController(orderId: demoOrderId, orderValue: demoOrderValue), animated: true)
    }

    // MARK: - Công cụ demo (gọi thẳng API tĩnh PromotionSDK, gác bằng isInitialized)

    @objc private func openHeadlessDemoTapped() {
        guard PromotionSDK.isInitialized() else { return }
        navigationController?.pushViewController(DemoHeadlessViewController(), animated: true)
    }

    @objc private func openThemePlaygroundTapped() {
        guard PromotionSDK.isInitialized() else { return }
        navigationController?.pushViewController(ThemePreviewViewController(), animated: true)
    }

    // MARK: - Helpers

    private func showAlert(_ title: String, _ message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }

}

// MARK: - Toast (tiện ích demo — KHÔNG chạm SDK)

extension UIViewController {

    /// Toast nổi ở đáy màn, tự tan sau `duration`. Đối ứng `Toast.makeText(...)` bên demo Android.
    ///
    /// Cố tình **không** dùng `PromotionToast` của SDK: đó là type nội bộ (`PRMPromotionUI`), host
    /// thật không nhìn thấy — demo mà xài thì hết vai trò "mô phỏng host". Cũng không dùng
    /// `UIAlertController` như `showAlert`: alert chặn tương tác và phải bấm OK, không phải toast.
    func showToast(_ message: String, duration: TimeInterval = 2) {
        let label = PaddedLabel()
        label.text = message
        label.numberOfLines = 0
        label.textAlignment = .center
        label.font = .systemFont(ofSize: 14)
        label.textColor = .white
        label.backgroundColor = UIColor.black.withAlphaComponent(0.8)
        label.layer.cornerRadius = 8
        label.clipsToBounds = true
        label.alpha = 0
        label.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -32),
            label.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 24),
            label.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -24),
        ])

        UIView.animate(withDuration: 0.2, animations: { label.alpha = 1 }) { _ in
            UIView.animate(withDuration: 0.3, delay: duration, animations: { label.alpha = 0 }) { _ in
                label.removeFromSuperview()
            }
        }
    }
}

/// Label có padding — `UILabel` thường không có `contentInset`, chữ sẽ dính sát mép nền toast.
private final class PaddedLabel: UILabel {

    private let inset = UIEdgeInsets(top: 10, left: 16, bottom: 10, right: 16)

    override func drawText(in rect: CGRect) {
        super.drawText(in: rect.inset(by: inset))
    }

    override var intrinsicContentSize: CGSize {
        let size = super.intrinsicContentSize
        return CGSize(
            width: size.width + inset.left + inset.right,
            height: size.height + inset.top + inset.bottom
        )
    }
}
