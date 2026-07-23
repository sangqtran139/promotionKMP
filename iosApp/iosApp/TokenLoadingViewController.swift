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

final class TokenLoadingViewController: UIViewController {

    // MARK: - Demo data (giả lập host cung cấp) — đối ứng `initSdk`/`updateDemoContext` bên Android

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
        title = "Demo PRMSDK"
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
                // customerId = msisdn (số điện thoại) — ngoài đời host truyền vào.
                self.initSdk(customerId: login.username, token: login.accessToken)
                self.updateDemoContext()

                self.statusLabel.text = "Lấy token thành công"
                self.setLoading(false)
                self.showTokenAlert(customerId: login.username, token: login.accessToken) { [weak self] in
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

    /// Đi qua wrapper `PromotionManager` (anti-corruption) — không gọi `PRMSDK` trực tiếp.
    private func initSdk(customerId: String, token: String) {
        promotions.start(customerId: customerId, token: token, availableServices: demoServices)
    }

    private func updateDemoContext() {
        promotions.updateContext(
            orderId: "ORD-DEMO-001",
            orderValue: "500000",
            // TEST: để nil (khớp Android demo) — kiểm tra detail có load + nút "Sử dụng ngay" hiện không.
            serviceCode: nil,
            metaData: nil
        )
    }

    /// Popup xác nhận login THẬT đã call: hiện customerId + token (preview) + nút Copy full token.
    /// Tắt popup mới sang màn chính — SDK lúc này chắc chắn đã `initialize`.
    private func showTokenAlert(customerId: String, token: String, onDismiss: @escaping () -> Void) {
        let preview = token.count > 60 ? "\(token.prefix(40))…\(token.suffix(12))" : token
        let message = "customerId (msisdn): \(customerId)\n\n"
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
