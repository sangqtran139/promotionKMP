//
//  TokenLoadingViewController.swift
//  PromotionSDKDemo
//
//  Cổng khởi động của app demo — SOI GƯƠNG `PromotionTokenLoadingFragment` bên Android:
//  lấy token đăng nhập (progress bar) → init SDK → bơm context demo → mới vào màn chính.
//  Login lỗi thì hiện lý do + nút "Thử lại", KHÔNG đi tiếp bằng token rỗng.
//
//  Vì sao cần cổng này: SDK khởi tạo bất đồng bộ sau login. Trước đây màn chính hiện ngay từ
//  `viewDidLoad` nên bấm sớm là rơi vào SDK chưa `initialize` — màn không mở được và widget rỗng.
//

import UIKit
import PRM

final class TokenLoadingViewController: UIViewController {

    // MARK: - Demo data (giả lập host cung cấp) — đối ứng `initSdk`/`updateDemoContext` bên Android

    /// Base URL Promotion BFF — host cấu hình.
    private static let baseUrl = "http://125.235.38.229:8080"

    /// Danh mục dịch vụ HOST cung cấp (cho bottom sheet "Chọn dịch vụ") — đối ứng `demoServices` bên Android.
    ///
    /// `serviceCode` phải khớp **`applicableProducts.productId`** của voucher (`sku` KHÔNG được dùng
    /// để so — xem `servicesForApplicableProducts`), nên ở đây là UUID chứ không phải mã "P-FOOD-001".
    ///
    /// Một `productId` gắn nhiều SKU (vd `…0011` = BH 2 chiều + gói doanh nghiệp, `…0003` = V120 + V90)
    /// nhưng list bị `distinctBy { serviceCode }` → khai **một dòng mỗi productId**, tên gộp các SKU.
    /// Khai theo từng SKU thì dòng thứ hai bị loại âm thầm.
    private let demoServices: [PromotionAvailableService] = [
        PromotionAvailableService(serviceCode: "019a7000-0002-0000-0000-000000000002", serviceName: "Data Viettel MIMAX125 - không giới hạn", serviceType: "TELCO",     iconUrl: "https://picsum.photos/seed/mimax125/96"),
        PromotionAvailableService(serviceCode: "019a7000-0002-0000-0000-000000000003", serviceName: "Gói cước V120 / V90",                    serviceType: "TELCO",     iconUrl: "https://picsum.photos/seed/goicuoc/96"),
        PromotionAvailableService(serviceCode: "019a7000-0002-0000-0000-000000000010", serviceName: "BH xe máy Vespa 1 năm",                  serviceType: "INSURANCE", iconUrl: "https://picsum.photos/seed/vespa/96"),
        PromotionAvailableService(serviceCode: "019a7000-0002-0000-0000-000000000011", serviceName: "BH ô tô (2 chiều / doanh nghiệp)",       serviceType: "INSURANCE", iconUrl: "https://picsum.photos/seed/bhoto/96"),
        PromotionAvailableService(serviceCode: "019a7000-0002-0000-0000-000000000013", serviceName: "Combo đồ uống đóng chai",                serviceType: "BEVERAGE",  iconUrl: "https://picsum.photos/seed/douong/96")
    ]

    // MARK: - UI

    private let progressBar: UIProgressView = {
        let v = UIProgressView(progressViewStyle: .bar)
        v.translatesAutoresizingMaskIntoConstraints = false
        v.progressTintColor = .systemBlue
        v.trackTintColor = .secondarySystemFill
        return v
    }()

    private let spinner: UIActivityIndicatorView = {
        let v = UIActivityIndicatorView(style: .large)
        v.translatesAutoresizingMaskIntoConstraints = false
        v.hidesWhenStopped = true
        return v
    }()

    private let statusLabel: UILabel = {
        let l = UILabel()
        l.translatesAutoresizingMaskIntoConstraints = false
        l.textAlignment = .center
        l.numberOfLines = 0
        l.font = .systemFont(ofSize: 15)
        l.textColor = .secondaryLabel
        return l
    }()

    private lazy var retryButton: UIButton = {
        let b = UIButton(type: .system)
        b.translatesAutoresizingMaskIntoConstraints = false
        b.setTitle("Thử lại", for: .normal)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = .systemBlue
        b.layer.cornerRadius = 10
        b.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        b.isHidden = true
        b.addTarget(self, action: #selector(startLogin), for: .touchUpInside)
        return b
    }()

    /// Chạy progress bar "vô định" (UIProgressView không có chế độ indeterminate như Android).
    private var progressTimer: Timer?

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Demo PromotionSDK"
        view.backgroundColor = .systemBackground
        setupLayout()
        startLogin()
    }

    deinit { progressTimer?.invalidate() }

    private func setupLayout() {
        view.addSubview(progressBar)
        view.addSubview(spinner)
        view.addSubview(statusLabel)
        view.addSubview(retryButton)

        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -60),

            progressBar.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 40),
            progressBar.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -40),
            progressBar.topAnchor.constraint(equalTo: spinner.bottomAnchor, constant: 24),

            statusLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            statusLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            statusLabel.topAnchor.constraint(equalTo: progressBar.bottomAnchor, constant: 20),

            retryButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            retryButton.topAnchor.constraint(equalTo: statusLabel.bottomAnchor, constant: 24),
            retryButton.widthAnchor.constraint(equalToConstant: 140),
            retryButton.heightAnchor.constraint(equalToConstant: 44)
        ])
    }

    // MARK: - Login (đối ứng `PromotionTokenLoadingFragment.startLogin`)

    @objc private func startLogin() {
        statusLabel.text = "Đang lấy token đăng nhập...."
        setLoading(true)

        LoginService.shared.login { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let login):
                self.initSdk(token: login.accessToken)
                self.updateDemoContext()

                self.statusLabel.text = "Lấy token thành công"
                self.setLoading(false)
                self.showTokenAlert(msisdn: login.username, token: login.accessToken) { [weak self] in
                    self?.navigateToLauncher()
                }
            case .failure(let error):
                print("[Demo] Login lỗi: \(error)")
                self.statusLabel.text = "Lấy token thất bại\n\(error)"
                self.setLoading(false)
                self.retryButton.isHidden = false
            }
        }
    }

    /// Gọi THẲNG PromotionSDK — không qua wrapper.
    /// Lần đầu: initialize(...) đầy đủ (chốt field cố định baseUrl/environment/language/theme).
    /// Login lại (đã init rồi): chỉ updateSession(...) với field động — không lặp lại config cố định.
    private func initSdk(token: String) {
        if PromotionSDK.isInitialized() {
            PromotionSDK.updateSession(
                accessToken: token,
                availableServices: demoServices,
            )
        } else {
            PromotionSDK.initialize(
                accessToken: token,
                baseUrl: Self.baseUrl,
                availableServices: demoServices,
                callback: DemoPromotionCallback.shared,
            )
        }
    }

    private func updateDemoContext() {
        PromotionSDK.updateContext(
            orderId: "ORD-DEMO-001",
            orderValue: "500000",
            // TEST: để nil (khớp Android demo) — kiểm tra detail có load + nút "Sử dụng ngay" hiện không.
            serviceCode: "TKBAOVIET",
            metaData: nil
        )
    }

    /// Popup xác nhận login THẬT đã call: hiện msisdn + token (preview) + nút Copy full token.
    /// Tắt popup mới sang màn chính — SDK lúc này chắc chắn đã `initialize`.
    private func showTokenAlert(msisdn: String, token: String, onDismiss: @escaping () -> Void) {
        let preview = token.count > 60 ? "\(token.prefix(40))…\(token.suffix(12))" : token
        let message = "msisdn: \(msisdn)\n\n"
            + "accessToken (\(token.count) ký tự):\n\(preview)\n\n"
            + "→ đã truyền vào SDK làm Bearer token."
        let alert = UIAlertController(title: "Login OK — token đã lấy", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Copy token", style: .default) { _ in
            UIPasteboard.general.string = token
            onDismiss()
        })
        alert.addAction(UIAlertAction(title: "OK", style: .cancel) { _ in onDismiss() })
        present(alert, animated: true)
    }

    private func navigateToLauncher() {
        // Thay root của navigation → không back ngược lại được màn loading (giống `replace` Android).
        navigationController?.setViewControllers([ViewController()], animated: true)
    }

    // MARK: - Helpers

    private func setLoading(_ loading: Bool) {
        retryButton.isHidden = true
        if loading {
            spinner.startAnimating()
            startProgressAnimation()
        } else {
            spinner.stopAnimating()
            stopProgressAnimation(completed: true)
        }
    }

    private func startProgressAnimation() {
        progressBar.isHidden = false
        progressBar.setProgress(0, animated: false)
        progressTimer?.invalidate()
        progressTimer = Timer.scheduledTimer(withTimeInterval: 0.15, repeats: true) { [weak self] _ in
            guard let self else { return }
            // Bò dần tới 90% rồi dừng — hoàn tất khi login trả về.
            let next = self.progressBar.progress + 0.02
            self.progressBar.setProgress(min(next, 0.9), animated: true)
        }
    }

    private func stopProgressAnimation(completed: Bool) {
        progressTimer?.invalidate()
        progressTimer = nil
        progressBar.setProgress(completed ? 1 : 0, animated: true)
    }
}
