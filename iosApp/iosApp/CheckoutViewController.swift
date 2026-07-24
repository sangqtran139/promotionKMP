//
//  CheckoutViewController.swift
//  PromotionSDKDemo
//
//  Màn "Thanh toán" của host: nhúng widget chọn ưu đãi (custom view của SDK) + nút
//  "Thanh toán" gọi API redemption với voucher user đã áp trên widget.
//
//  Luồng: user áp voucher trên widget → SDK phát `onVoucherApplied` → màn này lưu lại
//  voucherId → bấm "Thanh toán" → `PromotionSDK.api.createRedemption`. Gọi THẲNG SDK, không wrapper.
//

import UIKit
import PRM

final class CheckoutViewController: UIViewController {

    private let orderId: String
    private let orderValue: String

    /// Voucher user đã áp trên widget (nil = chưa áp / đã huỷ). Nút "Thanh toán" redeem cái này.
    private var appliedVoucherId: String?

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

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // Màn này sở hữu sự kiện áp/huỷ voucher trong lúc hiển thị (callback SDK là 1-1).
        let events = DemoPromotionCallback.shared
        events.onApplied = { [weak self] voucherId in
            self?.appliedVoucherId = voucherId
        }
        events.onCleared = { [weak self] in
            self?.appliedVoucherId = nil
        }
    }

    // MARK: - Layout

    private func setupLayout() {
        // Widget chọn ưu đãi (custom view SDK) — truyền order tại đây, không re-init SDK.
        let widget = PromotionSDK.createEndowView(from: self, orderId: orderId, orderValue: orderValue)
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

    @objc private func payTapped() {
        guard let voucherId = appliedVoucherId else {
            showAlert("Chưa áp dụng ưu đãi", "Vui lòng chọn và áp dụng một ưu đãi trên widget trước khi thanh toán.")
            return
        }
        payButton.isEnabled = false
        // Headless `api` trả DTO công khai — gọi thẳng.
        PromotionSDK.api.createRedemption(orderId: orderId, orderValue: orderValue, voucherIds: [voucherId]) { [weak self] result in
            self?.payButton.isEnabled = true
            switch result {
            case .success(let redemption):
                self?.showAlert("Thanh toán thành công", "Đã tạo phiên redemption.\nsessionId: \(redemption.sessionId)")
            case .failure(let error):
                self?.showAlert("Thanh toán lỗi", error.localizedDescription)
            }
        }
    }

    // MARK: - Helpers

    private func showAlert(_ title: String, _ message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
