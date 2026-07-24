//
//  ChoosePromotionViewController.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//

import UIKit
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
        // Qua ViewModel (đối ứng Android `ValidateAndApply` → effect `ApplySelectedOffers`);
        // VM lọc offers đang chọn từ store, VC không tự đọc state.
        viewModel.handleAction(.validateAndApply)
    }
    
    private func configSearchTextField() {
        self.searchTextField.returnKeyType = .search
        self.searchTextField.delegate = self
        // Gõ mỗi ký tự → `queryChanged` (store lo debounce). Target-action thay `textPublisher` Combine.
        self.searchTextField.addTarget(self, action: #selector(searchTextChanged), for: .editingChanged)
    }

    @objc private func searchTextChanged(_ sender: UITextField) {
        viewModel.handleAction(.queryChanged(sender.text ?? ""))
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
    
    // MARK: - Bind ViewModel
    //
    // Đối ứng `ChoosePromotionFragment.observeData` bên Android: một `render(state)` cho toàn bộ bề
    // mặt, một nhánh effect, rồi `start()` (seed pre-select + preload).
    override func bindViewModel() {
        super.bindViewModel()

        viewModel.onState = { [weak self] state in self?.render(state) }
        viewModel.onEffect = { [weak self] effect in self?.handle(effect) }

        viewModel.handleAction(.loadInitial)
    }

    private func render(_ state: ChoosePromotionViewModel.UiState) {
        sections = state.sections
        promotionsTableView.reloadData()

        // Thanh "Đã chọn N voucher" — trạng thái do VM/store quyết định (đối ứng Android
        // `updateApplyButtonState`); trước đây iOS ẩn cứng nên bật multi-select là lệch.
        // Số tiền giảm để trống: Android cũng không set `txtReducedPrice`.
        totalVoucherView.isHidden = !state.showsSelectedCount
        if state.showsSelectedCount {
            voucherLabel.text = PromotionUIStrings.selectedVoucherCount(state.selectedCount)
            totalLabel.text = ""
        }

        shimmerView.isHidden = !state.isLoading
        if state.isLoading {
            shimmerView.startAnimating()
        } else {
            shimmerView.stopAnimating()
        }
    }

    private func handle(_ effect: ChoosePromotionViewModel.Effect) {
        switch effect {
        // Lỗi nghiệp vụ → toast (đồng nhất Android `showToast(mapPromotionError(code))`).
        case .showError(let code):
            PRMToast.show(PromotionUIStrings.errorMessage(code), in: view)
        // Bấm "Áp dụng" → trả offers đang chọn cho widget (EndowStore validate) — đối ứng
        // `ChoosePromotionFragment` xử lý effect `ApplySelectedOffers` bên Android.
        case .applySelectedOffers(let offers):
            onApplyVoucher?(offers)
        }
    }
    
    //MARK: - Action
    @IBAction private func didTapBackButton(_ sender: Any) {
        self.viewModel.routeToParent()
    }
}

// MARK: - SelectPromotionItemCellDelegate
extension ChoosePromotionViewController: SelectPromotionItemCellDelegate {
    func selectPromotionItemCellDidTap(_ cell: ChoosePromotionItemCell, id: String) {
        self.viewModel.handleAction(.toggleSelection(id))
    }

    func selectPromotionItemCellDidTapButton(_ cell: ChoosePromotionItemCell, id: String) {
        self.viewModel.handleAction(.openDetail(id))
    }
}

// MARK: - UITextFieldDelegate (search action on return key)
extension ChoosePromotionViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        viewModel.handleAction(.search)
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
                self?.viewModel.handleAction(.seeMoreMy)
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
            viewModel.handleAction(.loadMoreOtherVouchers)
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
