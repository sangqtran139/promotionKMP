//
//  APIPlaygroundViewController.swift
//  VDSPromotionDemoApp
//
//  Màn hình demo gọi từng API, hiển thị request/response đầy đủ.
//

import UIKit
import PromotionSDKUI

// MARK: - APIPlaygroundViewController

final class APIPlaygroundViewController: UIViewController {

    // MARK: - UI

    private lazy var tableView: UITableView = {
        let tv = UITableView(frame: .zero, style: .insetGrouped)
        tv.translatesAutoresizingMaskIntoConstraints = false
        tv.dataSource = self
        tv.delegate = self
        tv.rowHeight = UITableView.automaticDimension
        tv.estimatedRowHeight = 200
        tv.register(APICardCell.self, forCellReuseIdentifier: "APICardCell")
        return tv
    }()

    // MARK: - State

    private let sdk: PromotionSDK
    private var cards: [APICard] = []
    /// Trang voucher "của tôi" cho demo load more — tăng dần mỗi lần bấm getVouchers.
    private var getVouchersMyPage = 0
    /// Trang nhóm "của tôi" (Eligible) cho demo load more findEligible.
    private var findEligibleMyPage = 0

    // MARK: - Init

    init(sdk: PromotionSDK) {
        self.sdk = sdk
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError() }

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "API Playground"
        view.backgroundColor = .systemGroupedBackground
        setupTableView()
        buildCards()
        tableView.reloadData()
    }

    private func setupTableView() {
        view.addSubview(tableView)
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    // MARK: - Cards

    private func buildCards() {
        cards = [
            makeGetVouchersCard(),
            makeFindEligibleCard(),
            makeGetDetailCard(),
            makeValidateDiscountsCard(),
            makeCreateRedemptionCard()
        ]
    }

    private func makeGetDetailCard() -> APICard {
        let requestJSON = formatJSON([
            "voucherId": "VCH-2026-099",
            "customerId": "0386863534",
            "serviceCode": "(tuỳ chọn)"
        ])

        return APICard(
            title: "getVoucherDetail",
            method: "GET",
            description: "Lấy chi tiết 1 voucher (merchant, mô tả, hướng dẫn, hiệu lực, trạng thái).",
            requestJSON: requestJSON,
            responseJSON: nil,
            isLoading: false
        ) { [weak self] _, index in
            self?.callGetVoucherDetail(cardIndex: index)
        }
    }

    private func makeFindEligibleCard() -> APICard {
        let requestJSON = formatJSON([
            "orderId": "ORDER-1234",
            "orderValue": "500000",
            "items": [
                ["skuId": "SKU-01", "productId": "P-01", "quantity": 1, "unitPrice": "500000"]
            ],
            "tabCode": "(tuỳ chọn)",
            "myOffers": ["page": "0 → tăng dần mỗi lần bấm", "size": 10] as [String: Any],
            "otherOffers": ["page": 0, "size": 10] as [String: Any]
        ])

        return APICard(
            title: "findEligible",
            method: "POST",
            description: "Tìm ưu đãi đủ điều kiện cho đơn (2 nhóm my+other). Cần orderId/orderValue + items để lấy campaign theo SKU.",
            requestJSON: requestJSON,
            responseJSON: nil,
            isLoading: false
        ) { [weak self] _, index in
            self?.callFindEligible(cardIndex: index)
        }
    }

    private func makeGetVouchersCard() -> APICard {
        let requestJSON = formatJSON([
            "customerId": "0386863534",
            "keyword": "(tuỳ chọn)",
            "serviceCode": "(tuỳ chọn)",
            "tab": "all",
            "page": "0 → tăng dần mỗi lần bấm (demo load more)",
            "size": 10
        ])

        return APICard(
            title: "getVouchers",
            method: "GET",
            description: "Lấy voucher 'của tôi' (Search Customer Vouchers). Mỗi lần bấm tăng page để demo load more.",
            requestJSON: requestJSON,
            responseJSON: nil,
            isLoading: false
        ) { [weak self] _, index in
            self?.callGetVouchers(cardIndex: index)
        }
    }

    private func makeValidateDiscountsCard() -> APICard {
        let requestJSON = formatJSON([
            "customerId": "0386863534",
            "orderId": "ORDER-1234",
            "orderValue": "500000",
            "items": [
                ["objectType": "CAMPAIGN", "objectId": "VOUCHER-001"]
            ],
            // SDK tự build thêm các fields bên dưới khi gọi API:
            "customerInfo": [
                "customerId": "0386863534",
                "customerType": "",
                "segment": "",
                "tier": ""
            ] as [String: Any],
            "orderInfo": [
                "orderId": "ORDER-1234",
                "orderValue": "500000",
                "currency": "VND",
                "channel": "MOBILE"
            ] as [String: Any],
            "discountRequests": [
                ["objectType": "CAMPAIGN", "objectId": "VOUCHER-001", "priority": 1]
            ],
            "validationOptions": [
                "checkBudgetAvailability": true,
                "optimizeOrder": true,
                "explainLevel": "BASIC",
                "includeAlternatives": false
            ] as [String: Any]
        ])

        return APICard(
            title: "validateDiscounts",
            method: "POST",
            description: "Kiểm tra tính hợp lệ của voucher trước khi cho phép user xác nhận đơn hàng.",
            requestJSON: requestJSON,
            responseJSON: nil,
            isLoading: false
        ) { [weak self] card, index in
            self?.callValidateDiscounts(cardIndex: index)
        }
    }

    private func makeCreateRedemptionCard() -> APICard {
        let requestJSON = formatJSON([
            "customerId": "0386863534",
            "orderId": "ORDER-1234",
            "orderValue": "500000",
            "items": [
                ["objectType": "CAMPAIGN", "objectId": "VOUCHER-001"]
            ],
            // SDK tự build thêm các fields bên dưới khi gọi API:
            "customerInfo": [
                "customerId": "0386863534",
                "customerType": "",
                "segment": "",
                "tier": ""
            ] as [String: Any],
            "orderInfo": [
                "orderId": "ORDER-1234",
                "orderValue": "500000",
                "currency": "VND",
                "channel": "MOBILE"
            ] as [String: Any],
            "selectedRedeemables": [
                ["objectType": "CAMPAIGN", "objectId": "VOUCHER-001", "priority": 1, "expectedDiscount": ""]
            ],
            "sessionOptions": [
                "holdBudget": true,
                "validateOnly": false,
                "autoConfirm": false
            ] as [String: Any]
        ])

        return APICard(
            title: "createRedemption",
            method: "POST",
            description: "Tạo phiên thanh toán với voucher đã chọn. Trả về sessionId để gọi API thanh toán backend.",
            requestJSON: requestJSON,
            responseJSON: nil,
            isLoading: false
        ) { [weak self] card, index in
            self?.callCreateRedemption(cardIndex: index)
        }
    }

    // MARK: - API Calls

    private func callGetVouchers(cardIndex: Int) {
        setLoading(true, at: cardIndex)
        let myPage = getVouchersMyPage
        sdk.useCases.getVouchers(
            myPage: myPage,
            mySize: 10
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let r):
                let mapItems: ([PromotionVoucher]) -> [[String: Any]] = { vouchers in
                    vouchers.prefix(5).map { ["id": $0.id, "merchant": $0.merchantName, "title": $0.title] }
                }
                let responseJSON = self.formatJSON([
                    "requestedPage": myPage,
                    "myVouchers": [
                        "count": r.myVouchers.count,
                        "isLastPage": r.myIsLastPage,
                        "items": mapItems(r.myVouchers)
                    ] as [String: Any]
                ])
                self.setResponse(responseJSON, at: cardIndex, isError: false)
                // Demo load more: lần bấm sau lấy trang kế tiếp; hết thì quay về 0.
                self.getVouchersMyPage = r.myIsLastPage ? 0 : myPage + 1
            case .failure(let error):
                self.setResponse(self.formatError(error), at: cardIndex, isError: true)
            }
        }
    }

    private func callFindEligible(cardIndex: Int) {
        setLoading(true, at: cardIndex)
        let myPage = findEligibleMyPage
        let items = [PromotionOrderItem(skuId: "SKU-01", productId: "P-01", quantity: 1, unitPrice: "500000")]
        sdk.useCases.findEligible(
            orderId: "ORDER-1234",
            orderValue: "500000",
            items: items,
            myPage: myPage,
            mySize: 10,
            otherPage: 0,
            otherSize: 10
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let r):
                let mapItems: ([PromotionEligibleOffer]) -> [[String: Any]] = { offers in
                    offers.prefix(5).map {
                        ["id": $0.id, "name": $0.name, "usable": $0.usable,
                         "estimatedDiscount": $0.estimatedDiscount ?? "",
                         "ineligibleReason": $0.ineligibleReason ?? ""]
                    }
                }
                let responseJSON = self.formatJSON([
                    "requestedMyPage": myPage,
                    "myOffers": [
                        "count": r.myOffers.count,
                        "isLastPage": r.myIsLastPage,
                        "items": mapItems(r.myOffers)
                    ] as [String: Any],
                    "otherOffers": [
                        "count": r.otherOffers.count,
                        "isLastPage": r.otherIsLastPage,
                        "items": mapItems(r.otherOffers)
                    ] as [String: Any]
                ])
                self.setResponse(responseJSON, at: cardIndex, isError: false)
                self.findEligibleMyPage = r.myIsLastPage ? 0 : myPage + 1
            case .failure(let error):
                self.setResponse(self.formatError(error), at: cardIndex, isError: true)
            }
        }
    }

    private func callGetVoucherDetail(cardIndex: Int) {
        setLoading(true, at: cardIndex)
        sdk.useCases.getVoucherDetail(voucherId: "VC-ACT-0002") { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let d):
                let responseJSON = self.formatJSON([
                    "id": d.id,
                    "merchantName": d.merchantName,
                    "title": d.title,
                    "status": d.status,
                    "displayStatusLabel": d.displayStatusLabel ?? "",
                    "guideline": String(d.guideline.prefix(120))
                ])
                self.setResponse(responseJSON, at: cardIndex, isError: false)
            case .failure(let error):
                self.setResponse(self.formatError(error), at: cardIndex, isError: true)
            }
        }
    }

    private func callValidateDiscounts(cardIndex: Int) {
        setLoading(true, at: cardIndex)
        sdk.useCases.validateDiscounts(
            orderId: "ORDER-1234",
            orderValue: "500000",
            voucherIds: ["VOUCHER-001"]
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let r):
                let items = r.items.map { item -> [String: Any] in
                    return [
                        "objectId": item.objectId,
                        "discountAmount": item.discountAmount,
                        "isValid": item.isValid,
                        "eligibilityStatus": item.eligibilityStatus
                    ]
                }
                let responseJSON = self.formatJSON([
                    "overallValid": r.overallValid,
                    "totalDiscountAmount": r.totalDiscountAmount,
                    "finalAmount": r.finalAmount,
                    "items": items
                ])
                self.setResponse(responseJSON, at: cardIndex, isError: false)
            case .failure(let error):
                self.setResponse(self.formatError(error), at: cardIndex, isError: true)
            }
        }
    }

    private func callCreateRedemption(cardIndex: Int) {
        setLoading(true, at: cardIndex)
        sdk.useCases.createRedemption(
            orderId: "ORDER-1234",
            orderValue: "500000",
            voucherIds: ["VOUCHER-001"]
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let r):
                var dict: [String: Any] = [
                    "sessionId": r.sessionId,
                    "totalDiscount": r.totalDiscount,
                    "finalAmount": r.finalAmount
                ]
                if !r.validationErrors.isEmpty {
                    dict["validationErrors"] = r.validationErrors.map { ["code": $0.code, "message": $0.message] }
                }
                let responseJSON = self.formatJSON(dict)
                self.setResponse(responseJSON, at: cardIndex, isError: false)
            case .failure(let error):
                self.setResponse(self.formatError(error), at: cardIndex, isError: true)
            }
        }
    }

    // MARK: - State Helpers

    private func setLoading(_ loading: Bool, at index: Int) {
        cards[index].isLoading = loading
        if loading {
            cards[index].responseJSON = nil
            cards[index].isError = false
        }
        let indexPath = IndexPath(row: 0, section: index)
        tableView.reloadRows(at: [indexPath], with: .none)
    }

    private func setResponse(_ json: String, at index: Int, isError: Bool) {
        cards[index].isLoading = false
        cards[index].responseJSON = json
        cards[index].isError = isError
        let indexPath = IndexPath(row: 0, section: index)
        tableView.reloadRows(at: [indexPath], with: .automatic)
    }

    // MARK: - Formatting

    private func formatJSON(_ dict: [String: Any]) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: dict, options: .prettyPrinted),
              let str = String(data: data, encoding: .utf8) else {
            return "{}"
        }
        return str
    }

    private func formatError(_ error: PromotionSDKError) -> String {
        var dict: [String: Any] = [
            "error": true,
            "type": errorType(error),
            "message": error.errorDescription ?? error.localizedDescription
        ]
        if let code = error.serverCode { dict["serverCode"] = code }
        return formatJSON(dict)
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
}

// MARK: - UITableViewDataSource

extension APIPlaygroundViewController: UITableViewDataSource {
    func numberOfSections(in tableView: UITableView) -> Int { cards.count }
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { 1 }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "APICardCell", for: indexPath) as! APICardCell
        cell.configure(with: cards[indexPath.section], index: indexPath.section)
        return cell
    }

    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        let card = cards[section]
        return "[\(card.method)] sdk.useCases.\(card.title)(...)"
    }
}

extension APIPlaygroundViewController: UITableViewDelegate {}

// MARK: - Data Model

struct APICard {
    let title: String
    let method: String
    let description: String
    let requestJSON: String
    var responseJSON: String?
    var isLoading: Bool
    var isError: Bool = false
    let onCall: (APICard, Int) -> Void
}

// MARK: - APICardCell

final class APICardCell: UITableViewCell {

    private let descLabel: UILabel = {
        let l = UILabel()
        l.font = .systemFont(ofSize: 13)
        l.textColor = .secondaryLabel
        l.numberOfLines = 0
        return l
    }()

    private let requestTitleLabel = makeMonoLabel("REQUEST", color: .systemBlue)
    private let requestTextView = makeTextView(background: UIColor.systemBlue.withAlphaComponent(0.06))

    private let responseTitleLabel = makeMonoLabel("RESPONSE", color: .systemGreen)
    private let responseTextView = makeTextView(background: UIColor.systemGreen.withAlphaComponent(0.06))

    private let callButton: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle("▶ Gọi API", for: .normal)
        btn.titleLabel?.font = .systemFont(ofSize: 15, weight: .semibold)
        btn.backgroundColor = .systemBlue
        btn.setTitleColor(.white, for: .normal)
        btn.layer.cornerRadius = 10
        btn.heightAnchor.constraint(equalToConstant: 44).isActive = true
        return btn
    }()

    private let loadingIndicator: UIActivityIndicatorView = {
        let v = UIActivityIndicatorView(style: .medium)
        v.hidesWhenStopped = true
        return v
    }()

    private let stackView: UIStackView = {
        let sv = UIStackView()
        sv.axis = .vertical
        sv.spacing = 10
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    private var cardIndex: Int = 0
    private var onCall: ((APICard, Int) -> Void)?
    private var currentCard: APICard?

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        setupLayout()
    }

    required init?(coder: NSCoder) { fatalError() }

    private func setupLayout() {
        contentView.addSubview(stackView)
        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14),
            stackView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stackView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            stackView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -14)
        ])

        let callRow = UIStackView(arrangedSubviews: [callButton, loadingIndicator])
        callRow.axis = .horizontal
        callRow.spacing = 10
        callRow.alignment = .center

        [descLabel,
         requestTitleLabel, requestTextView,
         callRow,
         responseTitleLabel, responseTextView
        ].forEach { stackView.addArrangedSubview($0) }

        callButton.addTarget(self, action: #selector(didTapCall), for: .touchUpInside)
    }

    func configure(with card: APICard, index: Int) {
        currentCard = card
        cardIndex = index
        onCall = card.onCall

        descLabel.text = card.description

        requestTextView.text = card.requestJSON
        requestTextView.isHidden = false
        requestTitleLabel.isHidden = false

        if card.isLoading {
            callButton.isEnabled = false
            callButton.alpha = 0.5
            loadingIndicator.startAnimating()
            responseTitleLabel.isHidden = true
            responseTextView.isHidden = true
        } else {
            callButton.isEnabled = true
            callButton.alpha = 1.0
            loadingIndicator.stopAnimating()

            if let resp = card.responseJSON {
                responseTitleLabel.isHidden = false
                responseTextView.isHidden = false
                responseTextView.text = resp
                responseTextView.textColor = card.isError ? .systemRed : .label
                responseTitleLabel.textColor = card.isError ? .systemRed : .systemGreen
                responseTitleLabel.text = card.isError ? "ERROR" : "RESPONSE"
            } else {
                responseTitleLabel.isHidden = true
                responseTextView.isHidden = true
            }
        }
    }

    @objc private func didTapCall() {
        guard let card = currentCard else { return }
        onCall?(card, cardIndex)
    }

    // MARK: - Factory

    private static func makeMonoLabel(_ text: String, color: UIColor) -> UILabel {
        let l = UILabel()
        l.text = text
        l.font = .monospacedSystemFont(ofSize: 11, weight: .bold)
        l.textColor = color
        return l
    }

    private static func makeTextView(background: UIColor) -> UITextView {
        let tv = UITextView()
        tv.font = .monospacedSystemFont(ofSize: 12, weight: .regular)
        tv.textColor = .label
        tv.backgroundColor = background
        tv.layer.cornerRadius = 8
        tv.isEditable = false
        tv.isScrollEnabled = false
        tv.textContainerInset = UIEdgeInsets(top: 10, left: 10, bottom: 10, right: 10)
        return tv
    }
}
