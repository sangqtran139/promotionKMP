//
//  DemoHeadlessViewController.swift
//  PromotionSDKDemo
//
//  Chế độ headless: đối tác tự dựng UI, chỉ gọi `PromotionSDK.api`.
//
//  Soi gương `DemoHeadlessFragment` + `DemoHeadlessViewModel` bên Android: cùng 5 bước, cùng thứ tự,
//  cùng chuỗi log. Sửa một bên thì sửa cả hai.
//

import UIKit
import PRM

final class DemoHeadlessViewController: UIViewController {

    // MARK: - Đọc lại giá trị đã set qua PromotionSDK.updateContext()

    private var orderId: String { PromotionSDK.currentOrderId ?? "" }
    private var orderValue: String { PromotionSDK.currentOrderValue ?? "" }

    // MARK: - State

    private var logLines: [String] = []
    /// Giữ voucherId lấy từ search để các bước sau dùng
    private var firstVoucherId: String?
    private var validatedVoucherIds: [String] = []

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
        logSdkContext()
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

    private func logSdkContext() {
        let session = PromotionSDK.session
        emit("── SDK context ──────────────────────")
        emit("   customerId  : \(session?.customerId ?? "(null)")")
        emit("   language    : \(session?.language ?? "(null)")")
        emit("   orderId     : \(PromotionSDK.currentOrderId ?? "(null)")")
        emit("   orderValue  : \(PromotionSDK.currentOrderValue ?? "(null)")")
        emit("   serviceCode : \(PromotionSDK.currentServiceCode ?? "(null)")")
        emit("─────────────────────────────────────")
    }

    // MARK: - Step 1: Search vouchers

    @objc private func searchVouchersTapped() {
        setLoading(true)
        PromotionSDK.api.getVouchers(page: 0, size: Self.pageSize) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let page):
                let vouchers = page.vouchers
                self.firstVoucherId = vouchers.first?.id
                self.emit("✅ getVouchers")
                self.emit("   vouchers: \(vouchers.count) items (lastPage=\(page.isLastPage))")
                vouchers.prefix(Self.maxLogItems).forEach { self.emit("   - [\($0.id)] \($0.title)") }
                if vouchers.count > Self.maxLogItems {
                    self.emit("   ... +\(vouchers.count - Self.maxLogItems) more")
                }
            case .failure(let error):
                self.emit(self.formatError("getVouchers", error))
            }
            self.setLoading(false)
        }
    }

    // MARK: - Step 2: Find eligible

    @objc private func findEligibleTapped() {
        setLoading(true)
        PromotionSDK.api.findEligible(
            orderId: orderId,
            orderValue: orderValue,
            items: demoOrderItems(),
            myPage: 0,
            mySize: Self.pageSize,
            otherPage: 0,
            otherSize: Self.pageSize
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let data):
                self.emit("✅ findEligible")
                self.emit("   myOffers   : \(data.myOffers.count) items (lastPage=\(data.myIsLastPage))")
                data.myOffers.prefix(Self.maxLogItems).forEach {
                    self.emit("   - [\($0.id)] \($0.name) usable=\($0.usable)")
                }
                self.emit("   otherOffers: \(data.otherOffers.count) items (lastPage=\(data.otherIsLastPage))")
                data.otherOffers.prefix(Self.maxLogItems).forEach {
                    self.emit("   - [\($0.id)] \($0.name) usable=\($0.usable)")
                }
            case .failure(let error):
                self.emit(self.formatError("findEligible", error))
            }
            self.setLoading(false)
        }
    }

    // MARK: - Step 3: Get voucher detail

    @objc private func getVoucherDetailTapped() {
        guard let voucherId = requireVoucherId() else { return }
        setLoading(true)
        PromotionSDK.api.getVoucherDetail(voucherId: voucherId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let detail):
                self.emit("✅ getVoucherDetail [\(detail.id)]")
                self.emit("   title      : \(detail.title)")
                self.emit("   merchant   : \(detail.merchantName)")
                self.emit("   expires    : \(detail.expireDate ?? "(null)")")
                self.emit("   status     : \(detail.status)")
            case .failure(let error):
                self.emit(self.formatError("getVoucherDetail", error))
            }
            self.setLoading(false)
        }
    }

    // MARK: - Step 4: Validate discounts

    @objc private func validateDiscountsTapped() {
        guard let voucherId = requireVoucherId() else { return }
        setLoading(true)
        PromotionSDK.api.validateDiscounts(
            orderId: orderId,
            orderValue: orderValue,
            voucherIds: [voucherId]
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let data):
                self.validatedVoucherIds = data.items.filter { $0.isValid }.map { $0.objectId }
                self.emit("✅ validateDiscounts")
                self.emit("   overallValid       : \(data.overallValid)")
                self.emit("   totalDiscountAmount: \(data.totalDiscountAmount)")
                self.emit("   finalAmount        : \(data.finalAmount)")
                data.items.forEach {
                    self.emit("   [\($0.objectId)] valid=\($0.isValid) discount=\($0.discountAmount)")
                }
            case .failure(let error):
                self.emit(self.formatError("validateDiscounts", error))
            }
            self.setLoading(false)
        }
    }

    // MARK: - Step 5: Create redemption

    @objc private func createRedemptionTapped() {
        guard !validatedVoucherIds.isEmpty else {
            emit("⚠️ Chưa validate, hãy Validate trước")
            return
        }
        setLoading(true)
        PromotionSDK.api.createRedemption(
            orderId: orderId,
            orderValue: orderValue,
            voucherIds: validatedVoucherIds
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let data):
                self.emit("✅ createRedemption")
                self.emit("   sessionId    : \(data.sessionId)")
                self.emit("   totalDiscount: \(data.totalDiscount)")
                self.emit("   finalAmount  : \(data.finalAmount)")
                self.emit("   hasErrors    : \(!data.validationErrors.isEmpty)")
                data.validationErrors.forEach { self.emit("   ⚠️ \($0.code): \($0.message)") }
            case .failure(let error):
                self.emit(self.formatError("createRedemption", error))
            }
            self.setLoading(false)
        }
    }

    private func requireVoucherId() -> String? {
        guard let firstVoucherId else {
            emit("⚠️ Chưa có voucherId, hãy Search trước")
            return nil
        }
        return firstVoucherId
    }

    /// Dòng đơn hàng giả lập — `findEligible` cần items để lấy campaign theo SKU (rỗng thì chỉ nhận
    /// campaign cấp đơn). Context của SDK không đọc ngược ra `orderItems` được, nên demo tự dựng.
    private func demoOrderItems() -> [PromotionOrderItem] {
        [PromotionOrderItem(
            skuId: Self.demoSkuId,
            productId: Self.demoProductId,
            quantity: 1,
            unitPrice: orderValue
        )]
    }

    // MARK: - Log

    @objc private func clearLogTapped() {
        logLines.removeAll()
        logTextView.text = ""
    }

    private func setLoading(_ loading: Bool) {
        actionButtons.forEach {
            $0.isEnabled = !loading
            $0.alpha = loading ? 0.5 : 1.0
        }
    }

    private func emit(_ msg: String) {
        logLines.append(msg)
        logTextView.text = logLines.joined(separator: "\n")
        scrollLogToBottom()
    }

    private func scrollLogToBottom() {
        let length = (logTextView.text as NSString).length
        guard length > 0 else { return }
        logTextView.scrollRangeToVisible(NSRange(location: length - 1, length: 1))
    }

    /// `❌ tên: [type] message (serverCode=…)` — cùng định dạng với bên Android.
    private func formatError(_ name: String, _ error: PromotionSDKError) -> String {
        let serverCode = error.serverCode.map { " (serverCode=\($0))" } ?? ""
        let message = error.errorDescription ?? error.localizedDescription
        return "❌ \(name): [\(errorType(error))] \(message)\(serverCode)"
    }

    private func errorType(_ error: PromotionSDKError) -> String {
        switch error {
        case .networkFailure:  return "networkFailure"
        case .sessionExpired:  return "sessionExpired"
        case .timeout:         return "timeout"
        case .parseFailed:     return "parseFailed"
        case .featureDisabled: return "featureDisabled"
        case .unknown:         return "unknown"
        }
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

    private static let pageSize = 10
    private static let maxLogItems = 3
    private static let demoSkuId = "SKU-01"
    private static let demoProductId = "P-01"
}
