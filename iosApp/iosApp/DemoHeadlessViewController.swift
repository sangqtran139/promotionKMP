//
//  DemoHeadlessViewController.swift
//  PromotionSDKDemo
//
//  Chế độ headless: đối tác tự dựng UI, chỉ gọi `PromotionSDK.api`.
//
//  Soi gương `DemoHeadlessFragment` bên Android: view mỏng, forward tap sang `DemoHeadlessViewModel`,
//  chỉ giữ `appendLog` / `clearLog` để render. Logic 5 bước nằm ở VM. Sửa một bên thì sửa cả hai.
//

import UIKit
import PromotionKit

final class DemoHeadlessViewController: UIViewController {

    private let viewModel = DemoHeadlessViewModel()
    private var logLines: [String] = []

    // MARK: - UI

    private lazy var searchVouchersButton = makeButton("1. Search Vouchers", action: #selector(searchVouchersTapped))
    private lazy var findEligibleButton = makeButton("2. Find Eligible", action: #selector(findEligibleTapped))
    private lazy var getDetailButton = makeButton("3. Get Voucher Detail", action: #selector(getVoucherDetailTapped))
    private lazy var validateButton = makeButton("4. Validate Discounts", action: #selector(validateDiscountsTapped))
    private lazy var createRedemptionButton = makeButton("5. Create Redemption", action: #selector(createRedemptionTapped))

    private lazy var clearLogButton: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle("Clear log", for: .normal)
        btn.titleLabel?.font = .systemFont(ofSize: 15, weight: .medium)
        btn.addTarget(self, action: #selector(clearLogTapped), for: .touchUpInside)
        return btn
    }()

    private let logTextView: UITextView = {
        let tv = UITextView()
        tv.font = .monospacedSystemFont(ofSize: 12, weight: .regular)
        tv.textColor = .label
        tv.backgroundColor = .secondarySystemBackground
        tv.isEditable = false
        tv.textContainerInset = UIEdgeInsets(top: 12, left: 12, bottom: 12, right: 12)
        tv.translatesAutoresizingMaskIntoConstraints = false
        return tv
    }()

    private let stackView: UIStackView = {
        let sv = UIStackView()
        sv.axis = .vertical
        sv.spacing = 16
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    private var actionButtons: [UIButton] {
        [searchVouchersButton, findEligibleButton, getDetailButton, validateButton, createRedemptionButton]
    }

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Headless API Demo"
        view.backgroundColor = .systemBackground
        setupLayout()
        bindViewModel()
        viewModel.logSdkContext()
    }

    private func setupLayout() {
        view.addSubview(stackView)
        view.addSubview(logTextView)

        actionButtons.forEach { stackView.addArrangedSubview($0) }

        // "Clear log" canh phải, không giãn full width — đối xứng `layout_gravity="end"` bên Android.
        let clearRow = UIStackView(arrangedSubviews: [UIView(), clearLogButton])
        clearRow.axis = .horizontal
        stackView.addArrangedSubview(clearRow)

        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24),
            stackView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            stackView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),

            logTextView.topAnchor.constraint(equalTo: stackView.bottomAnchor, constant: 8),
            logTextView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            logTextView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            logTextView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24)
        ])
    }

    /// Đối ứng `DemoHeadlessFragment.observeData`: quan sát `onLoading` / `onLog` từ VM.
    private func bindViewModel() {
        viewModel.onLoading = { [weak self] loading in self?.setLoading(loading) }
        viewModel.onLog = { [weak self] line in self?.appendLog(line) }
    }

    // MARK: - Actions (forward sang VM — đối ứng setOnClickListener bên Android)

    @objc private func searchVouchersTapped() { viewModel.searchVouchers() }
    @objc private func findEligibleTapped() { viewModel.findEligible() }
    @objc private func getVoucherDetailTapped() { viewModel.getVoucherDetail() }
    @objc private func validateDiscountsTapped() { viewModel.validateDiscounts() }
    @objc private func createRedemptionTapped() { viewModel.createRedemption() }
    @objc private func clearLogTapped() { clearLog() }

    // MARK: - Log (đối ứng `appendLog` / `clearLog` bên Android)

    private func appendLog(_ line: String) {
        logLines.append(line)
        logTextView.text = logLines.joined(separator: "\n")
        scrollLogToBottom()
    }

    private func clearLog() {
        logLines.removeAll()
        logTextView.text = ""
    }

    private func setLoading(_ loading: Bool) {
        actionButtons.forEach {
            $0.isEnabled = !loading
            $0.alpha = loading ? 0.5 : 1.0
        }
    }

    private func scrollLogToBottom() {
        let length = (logTextView.text as NSString).length
        guard length > 0 else { return }
        logTextView.scrollRangeToVisible(NSRange(location: length - 1, length: 1))
    }

    // MARK: - Factory

    private func makeButton(_ title: String, action: Selector) -> UIButton {
        let btn = UIButton(type: .system)
        btn.setTitle(title, for: .normal)
        btn.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        btn.backgroundColor = .systemBlue
        btn.setTitleColor(.white, for: .normal)
        btn.layer.cornerRadius = 10
        btn.heightAnchor.constraint(equalToConstant: 48).isActive = true
        btn.addTarget(self, action: action, for: .touchUpInside)
        return btn
    }
}
