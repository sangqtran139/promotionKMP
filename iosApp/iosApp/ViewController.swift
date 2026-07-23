//
//  ViewController.swift
//  PromotionSDKDemo
//
//  Demo tích hợp PRMSDK SDK QUA wrapper `PromotionManager` (pattern anti-corruption):
//  luồng chính (widget / danh sách / redemption / sự kiện) chỉ gọi PromotionManager, KHÔNG
//  chạm SDK trực tiếp. Chỉ 2 công cụ Playground bên dưới mới gọi thẳng API tĩnh `PRMSDK`.
//

import UIKit
import PromotionSDK   // chỉ còn cần cho 2 màn Playground (gọi API tĩnh). Luồng chính không dùng type SDK.

class ViewController: UIViewController {

    // MARK: - Demo data (giả lập host cung cấp)
    //
    // Login + `PromotionManager.start(...)` nằm ở `TokenLoadingViewController` (cổng khởi động) —
    // màn này chỉ hiện SAU KHI SDK đã initialize, nên không cần gác `isReady` ở từng nút.

    /// Đơn hàng ở màn thanh toán — truyền vào widget để validate voucher khi "Áp dụng".
    private let demoOrder = OrderContext(id: "ORDER-001", value: "500000")
    /// Id dự phòng cho nút "Mở thẳng chi tiết": khách chưa có voucher, hoặc API lỗi, vẫn vào được màn
    /// chi tiết để xem layout. Màn tự fetch theo id này rồi hiện shimmer → lỗi. Chỉ dùng ở demo.
    private let fallbackVoucherId = "VOUCHER-DEMO-001"

    private var promotions: PromotionServing { PromotionManager.shared }

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
        title = "Demo PRMSDK"
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
        promotions.onVoucherCountChanged = { count in
            print("[Demo] Voucher khả dụng: \(count)")
        }
        promotions.onVoucherCleared = {
            print("[Demo] Voucher đã bị huỷ")
        }
        // Redemption đã chuyển sang màn Thanh toán (nút "Thanh toán") — màn ngoài không tự redeem nữa.
        promotions.onVoucherApplied = { voucherId in
            print("[Demo] Voucher đã áp: \(voucherId)")
        }
        // User chọn dịch vụ trong bottom sheet → host tự điều hướng.
        promotions.onServiceSelected = { [weak self] sel in
            self?.showAlert("Đã chọn dịch vụ", "\(sel.name) (\(sel.code))\nvoucher: \(sel.voucherId)")
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
        stackView.addArrangedSubview(makeButton("▶  API Playground (Request / Response)", action: #selector(openPlaygroundTapped), color: .systemIndigo))
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

    // MARK: - Actions (luồng chính — qua manager)

    @objc private func showMyPromotionsTapped() {
        promotions.openMyPromotions(from: self)
    }

    /// Mở thẳng màn chi tiết, KHÔNG qua danh sách — mô phỏng host bấm vào push notification/deeplink.
    ///
    /// Ưu tiên `voucherId` thật (voucher đầu tiên của khách) để màn hiển thị dữ liệu đầy đủ. Nhưng
    /// khách chưa có voucher, hoặc API lỗi, thì **vẫn vào** bằng `fallbackVoucherId` — mục đích của
    /// nút này là xem được màn chi tiết, không phải kiểm tra API. Ngoài đời host đã có sẵn id
    /// (trong payload notification) nên không cần bước fetch này.
    @objc private func openPromotionDetailTapped() {
        promotions.fetchVouchers(keyword: nil, serviceCode: nil, tab: nil, page: 0) { [weak self] result in
            guard let self else { return }
            let voucherId = (try? result.get())?.mine.first?.id ?? self.fallbackVoucherId
            self.promotions.openPromotionDetail(voucherId: voucherId, from: self)
        }
    }

    @objc private func openCheckoutTapped() {
        navigationController?.pushViewController(CheckoutViewController(order: demoOrder), animated: true)
    }

    // MARK: - Playground (công cụ demo — gọi thẳng API tĩnh PRMSDK, gác bằng isReady)

    @objc private func openPlaygroundTapped() {
        guard PromotionManager.shared.isReady else { return }
        navigationController?.pushViewController(APIPlaygroundViewController(), animated: true)
    }

    @objc private func openThemePlaygroundTapped() {
        guard PromotionManager.shared.isReady else { return }
        navigationController?.pushViewController(ThemePreviewViewController(), animated: true)
    }

    // MARK: - Helpers

    private func showAlert(_ title: String, _ message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }

}
