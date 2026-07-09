//
//  ViewController.swift
//  VDSPromotionDemoApp
//
//  Demo tích hợp PromotionSDK SDK QUA wrapper `PromotionManager` (pattern anti-corruption):
//  luồng chính (widget / danh sách / redemption / sự kiện) chỉ gọi PromotionManager, KHÔNG
//  chạm SDK trực tiếp. Chỉ 2 công cụ Playground bên dưới mới dùng `rawSDK` (cần instance thật).
//

import UIKit
import PromotionSDKUI   // chỉ còn cần cho 2 màn Playground (rawSDK). Luồng chính không dùng type SDK.

class ViewController: UIViewController {

    // MARK: - Demo data (giả lập host cung cấp)

    /// customerId + token thường có sau khi user login. Demo hardcode.
    private let demoCustomerId = "CUST-001"
    private let demoToken: String? = nil
    /// Đơn hàng ở màn thanh toán — truyền vào widget để validate voucher khi "Áp dụng".
    private let demoOrder = OrderContext(id: "ORDER-001", value: "500000")

    /// Danh mục dịch vụ HOST cung cấp (map sang bottom sheet "Chọn dịch vụ").
    /// 3 mã đầu trùng applicableProducts voucher ACTIVE → sẽ hiện; 2 mã cuối bị lọc bỏ (minh hoạ mapping).
    private let demoServices: [AvailableService] = [
        AvailableService(code: "P-FOOD-001", name: "Combo gà rán",        type: "FOOD",    iconUrl: "https://picsum.photos/seed/food1/96"),
        AvailableService(code: "P-FOOD-002", name: "Mì Ý sốt bò",         type: "FOOD",    iconUrl: "https://picsum.photos/seed/food2/96"),
        AvailableService(code: "P-ALC-001",  name: "Bia lon 330ml",       type: "ALCOHOL", iconUrl: "https://picsum.photos/seed/beer/96"),
        AvailableService(code: "P-TELCO-001", name: "Nạp tiền điện thoại", type: "TELCO",   iconUrl: "https://picsum.photos/seed/telco/96"),
        AvailableService(code: "P-BILL-001",  name: "Thanh toán hoá đơn",  type: "BILL",    iconUrl: "https://picsum.photos/seed/bill/96")
    ]

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
        title = "Demo PromotionSDK"
        view.backgroundColor = .systemBackground

        // (1) Đăng nhập lấy token thật (giả lập host), rồi khởi tạo SDK qua manager.
        loginThenStartPromotions()
        setupLayout()
    }

    /// Giả lập host: gọi API đăng nhập (2 bước) lấy accessToken + msisdn, rồi start SDK.
    /// Login lỗi → fallback token=nil, customerId hardcode để demo vẫn chạy (BFF có thể 401).
    private func loginThenStartPromotions() {
        LoginService.shared.login { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let login):
                // customerId = msisdn (số điện thoại) — ngoài đời host truyền vào.
                self.promotions.start(customerId: login.username, token: login.accessToken, availableServices: self.demoServices)
                self.showTokenAlert(customerId: login.username, token: login.accessToken)
            case .failure(let error):
                print("[Demo] Login lỗi: \(error) — chạy fallback không token")
                self.promotions.start(customerId: self.demoCustomerId, token: self.demoToken, availableServices: self.demoServices)
                self.showAlert("Login thất bại", "\(error)\n\nChạy fallback token = nil.")
            }
        }
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

    @objc private func openCheckoutTapped() {
        navigationController?.pushViewController(CheckoutViewController(order: demoOrder), animated: true)
    }

    // MARK: - Playground (công cụ demo — cần instance SDK thật qua rawSDK)

    @objc private func openPlaygroundTapped() {
        guard let sdk = PromotionManager.shared.rawSDK else { return }
        navigationController?.pushViewController(APIPlaygroundViewController(sdk: sdk), animated: true)
    }

    @objc private func openThemePlaygroundTapped() {
        guard let sdk = PromotionManager.shared.rawSDK else { return }
        navigationController?.pushViewController(ThemePreviewViewController(sdk: sdk), animated: true)
    }

    // MARK: - Helpers

    private func showAlert(_ title: String, _ message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }

    /// Popup xác nhận login THẬT đã call: hiện customerId + token (preview) + nút Copy full token.
    private func showTokenAlert(customerId: String, token: String) {
        let preview = token.count > 60 ? "\(token.prefix(40))…\(token.suffix(12))" : token
        let message = "customerId (msisdn): \(customerId)\n\n"
            + "accessToken (\(token.count) ký tự):\n\(preview)\n\n"
            + "→ đã truyền vào SDK làm Bearer token."
        let alert = UIAlertController(title: "Login OK — token đã lấy", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Copy token", style: .default) { _ in
            UIPasteboard.general.string = token
        })
        alert.addAction(UIAlertAction(title: "OK", style: .cancel))
        present(alert, animated: true)
    }
}
