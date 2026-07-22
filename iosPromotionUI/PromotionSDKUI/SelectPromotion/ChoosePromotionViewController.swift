//
//  ChoosePromotionViewController.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//

import UIKit
import Combine
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionViewController: PRMBaseViewController<ChoosePromotionViewModel> {

    // MARK: - UI Components
    @IBOutlet private weak var searchTextField: PRMSearchTextField!
    @IBOutlet private weak var totalLabel: UILabel!
    @IBOutlet private weak var voucherLabel: UILabel!
    @IBOutlet private weak var totalVoucherView: UIStackView!
    @IBOutlet private weak var promotionsTableView: UITableView!
    
    // MARK: - Properties
    private var sections: [ChoosePromotionViewModel.PromotionSection] = []
    /// Danh sách voucher đang chọn — sẵn sàng multi-select; hiện tại gate đơn nên thường 0/1.
    private var currentSelectedPromotions: [EligibleOffer] = []

    /// Callback trả về danh sách promotion đã chọn về host (tương tự Android onApplyVoucher).
    var onApplyVoucher: (([EligibleOffer]) -> Void)?

    /// Forward callback "Áp dụng" từ màn chi tiết (đẩy tiếp tới SDK boundary).
    var onApplyVoucherFromDetail: ((String) -> Void)?
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    // MARK: - Setup UI
    override func setupUI() {
        super.setupUI()
        self.configSearchTextField()
        self.configTableView()
        self.totalVoucherView.isHidden = true
        self.wireApplyButton()
        self.configShimmer()
    }

    // Shimmer skeleton phủ lên table trong lúc tải trang đầu (giống Android).
    private lazy var shimmerView: PRMShimmerReplicatorView = {
        let view = PRMShimmerReplicatorView(
            itemSize: .fixedHeight(PRMPromotionCardShimmerCell.itemHeight),
            cellProvider: { PRMPromotionCardShimmerCell() }
        )
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()

    private func configShimmer() {
        view.addSubview(shimmerView)
        NSLayoutConstraint.activate([
            shimmerView.topAnchor.constraint(equalTo: promotionsTableView.topAnchor),
            shimmerView.leadingAnchor.constraint(equalTo: promotionsTableView.leadingAnchor),
            shimmerView.trailingAnchor.constraint(equalTo: promotionsTableView.trailingAnchor),
            shimmerView.bottomAnchor.constraint(equalTo: promotionsTableView.bottomAnchor)
        ])
    }

    private func wireApplyButton() {
        // Apply button là sibling của totalVoucherView trong cùng parent stack
        let applyButton = totalVoucherView.superview?.subviews.first { $0 is UIButton } as? UIButton
        applyButton?.addTarget(self, action: #selector(didTapApplyButton), for: .touchUpInside)
        // Nút "Áp dụng" ở màn này là UIButton thường (không phải PRMButton) → áp button token thủ công.
        applyThemeToApplyButton(applyButton)
    }

    /// Áp `button` token cho nút "Áp dụng" (null-safe — bỏ qua field nil để giữ default).
    private func applyThemeToApplyButton(_ button: UIButton?) {
        guard let button, let token = PRMThemeRegistry.shared.button() else { return }
        if let bg = token.backgroundColor { button.backgroundColor = bg }
        if let textColor = token.textColor { button.setTitleColor(textColor, for: .normal) }
        if let radius = token.cornerRadius {
            button.layer.cornerRadius = radius
            button.clipsToBounds = true
        }
    }

    @objc private func didTapApplyButton() {
        guard !currentSelectedPromotions.isEmpty else { return }
        onApplyVoucher?(currentSelectedPromotions)
    }
    
    private func configSearchTextField() {
        self.searchTextField.returnKeyType = .search
        self.searchTextField.delegate = self
    }
    
    private func configTableView() {
        self.promotionsTableView.registerCell(ChoosePromotionItemCell.self)
        self.promotionsTableView.register(SeeMoreCell.self, forCellReuseIdentifier: "SeeMoreCell")
        self.promotionsTableView.rowHeight = UITableView.automaticDimension
        self.promotionsTableView.separatorStyle = .none
        
        self.promotionsTableView.dataSource = self
        self.promotionsTableView.delegate = self
        // Grouped style → header KHÔNG dính (cuộn theo list). Khử khoảng trống thừa của grouped:
        // top (tableHeaderView tí hon) — footer xử lý ở heightForFooterInSection.
        self.promotionsTableView.backgroundColor = Colors.tokenDark05
        self.promotionsTableView.tableHeaderView = UIView(frame: CGRect(x: 0, y: 0, width: 0, height: CGFloat.leastNormalMagnitude))

        if #available(iOS 15.0, *) {
            self.promotionsTableView.sectionHeaderTopPadding = 0
        }
    }
    
    private let seeMoreMyRelay = PassthroughSubject<Void, Never>()
    private let loadMoreOtherRelay = PassthroughSubject<Void, Never>()
    private let toggleSelectionRelay = PassthroughSubject<String, Never>()

    // MARK: - Bind ViewModel
    override func bindViewModel() {
        super.bindViewModel()

        let input = ChoosePromotionViewModel.Input(
            searchText: searchTextField.textPublisher,
            toggleSelectionRelay: toggleSelectionRelay,
            seeMoreMyRelay: seeMoreMyRelay,
            loadMoreOtherRelay: loadMoreOtherRelay
        )

        let output = viewModel.transform(input: input)

        output.sections
            .sink { [weak self] sections in
                self?.sections = sections
                self?.promotionsTableView.reloadData()
            }
            .store(in: &cancellables)

        output.isLoading
            .sink { [weak self] loading in
                guard let self = self else { return }
                self.shimmerView.isHidden = !loading
                if loading {
                    self.shimmerView.startAnimating()
                } else {
                    self.shimmerView.stopAnimating()
                }
            }
            .store(in: &cancellables)

        // Tạm thời ẩn thông tin "Đã chọn voucher" — chỉ track danh sách voucher đang chọn cho nút Áp dụng.
        output.selectedPromotions
            .sink { [weak self] promotions in
                self?.currentSelectedPromotions = promotions
            }
            .store(in: &cancellables)

        // Lỗi nghiệp vụ → Confirmation Dialog (đồng nhất Android — trước đây Choose iOS nuốt lỗi).
        output.errorCode
            .sink { [weak self] code in
                guard let self = self else { return }
                PRMConfirmationDialog.showError(PromotionUIStrings.errorMessage(code), in: self.view)
            }
            .store(in: &cancellables)
    }
    
    //MARK: - Action
    @IBAction private func didTapBackButton(_ sender: Any) {
        self.viewModel.routeToParent()
    }
}

// MARK: - SelectPromotionItemCellDelegate
extension ChoosePromotionViewController: SelectPromotionItemCellDelegate {
    func selectPromotionItemCellDidTap(_ cell: ChoosePromotionItemCell, id: String) {
        self.viewModel.input.toggleSelectionRelay.send(id)
    }

    func selectPromotionItemCellDidTapButton(_ cell: ChoosePromotionItemCell, id: String) {
        self.viewModel.routeToDetail(id: id)
    }
}

// MARK: - UITextFieldDelegate
extension ChoosePromotionViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
}

// MARK: - UITableViewDataSource & UITableViewDelegate
extension ChoosePromotionViewController: UITableViewDataSource, UITableViewDelegate {
    func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let sectionData = sections[section]
        return sectionData.items.count + (sectionData.seeMoreState != .none ? 1 : 0)
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let sectionData = sections[indexPath.section]

        if sectionData.seeMoreState != .none && indexPath.row == sectionData.items.count {
            guard let cell = tableView.dequeueReusableCell(withIdentifier: "SeeMoreCell", for: indexPath) as? SeeMoreCell else {

                return UITableViewCell()
            }
            // "Ưu đãi của tôi": Xem thêm (lộ thêm/load page kế) hoặc Thu gọn (đã hiện hết).
            let isCollapse = sectionData.seeMoreState == .collapse
            cell.button.setTitle(isCollapse ? "Thu gọn" : "Xem thêm", for: .normal)
            cell.button.setImage(UIImage.sdk(isCollapse ? "ic_up_arrow" : "ic_down_arrow"), for: .normal)

            cell.action = { [weak self] in
                self?.seeMoreMyRelay.send(())
            }
            return cell
        }
        
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "ChoosePromotionItemCell", for: indexPath) as? ChoosePromotionItemCell else {
            return UITableViewCell()
        }
        let cellViewModel = sectionData.items[indexPath.row]
        cell.delegate = self
        cell.bindData(cellViewModel)
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let sectionData = sections[indexPath.section]
        // Chỉ "Ưu đãi khác" phân trang theo cuộn: sắp hiện item cuối -> gọi page kế.
        // VM tự bỏ qua nếu đã hết trang hoặc đang tải.
        guard sectionData.type == .otherPromotions else { return }
        if indexPath.row == sectionData.items.count - 1 {
            loadMoreOtherRelay.send(())
        }
    }

    // Grouped style tự thêm footer ~kích thước — khử về gần 0 để các section sát nhau.
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return .leastNormalMagnitude
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        let title = sections[section].title
        
        // Section header "Ưu đãi của tôi / khác" là nội dung Android render dạng tab → map tabUnderline token.
        let tabToken = PRMThemeRegistry.shared.tabUnderline()
        let headerView = UIView()
        // Nền xám ĐỤC (trùng nền list) → gray liền mạch nhưng che content khi header plain dính/nổi
        // lúc cuộn (tránh thấy card xuyên qua = "dim").
        headerView.backgroundColor = tabToken?.backgroundColor ?? Colors.tokenDark05

        let label = UILabel()
        label.text = title
        label.font = Typography.fontBold16
        label.textColor = tabToken?.activeTextColor ?? Colors.tokenDark100

        headerView.addSubview(label)
        label.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 16),
            label.trailingAnchor.constraint(equalTo: headerView.trailingAnchor, constant: -16),
            label.topAnchor.constraint(equalTo: headerView.topAnchor, constant: 4),
            label.bottomAnchor.constraint(equalTo: headerView.bottomAnchor, constant: -4)
        ])
        
        return headerView
    }
}
