//
//  ChoosePromotionViewController.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//

import UIKit
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionViewController: PRMBaseViewController<ChoosePromotionViewModel> {

    // MARK: - UI Components
    @IBOutlet private weak var searchTextField: PRMSearchTextField!
    @IBOutlet private weak var totalLabel: UILabel!
    @IBOutlet private weak var voucherLabel: UILabel!
    @IBOutlet private weak var totalVoucherView: UIStackView!
    @IBOutlet private weak var promotionsTableView: UITableView!
    /// Nút "Áp dụng" — tìm bằng `wireApplyButton()` (không có outlet trong xib), giữ lại để
    /// `updateApplyButtonState` bật/tắt theo selection.
    private weak var applyButton: UIButton?

    /// Spinner "đang tải thêm" ở đáy list khi cuộn tới cuối nhóm "Ưu đãi khác" — đối ứng
    /// `ChoosePromotionListItem.Loading` mà Android nối vào cuối danh sách. Giống hệt cách màn
    /// "Tìm ưu đãi" làm (`SearchMyPromotionViewController.loadMoreSpinner`).
    private let loadMoreSpinner: UIActivityIndicatorView = {
        let v = UIActivityIndicatorView(style: .medium)
        v.hidesWhenStopped = true
        return v
    }()

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

    /// Tìm không ra kết quả — **cùng component** với màn "Tìm ưu đãi"
    /// (`SearchMyPromotionViewController.searchNoResultView`), kể cả ảnh và chuỗi mặc định.
    /// Chèn dưới thanh đáy như shimmer để không che nút "Áp dụng".
    private lazy var searchNoResultView: PromotionSearchNoResultView = {
        let noResult = PromotionSearchNoResultView()
        noResult.thumbnailImage = UIImage.sdk("prm_ic_search_no_result")
        noResult.isHidden = true
        if let bottomBar = totalVoucherView.superview {
            view.insertSubview(noResult, belowSubview: bottomBar)
        } else {
            view.addSubview(noResult)
        }
        noResult.makeAnchor { make in
            make.centerY(equalTo: self.view.centerYAnchor)
                .centerX(equalTo: self.view.centerXAnchor)
                .leading(equalTo: self.view.leadingAnchor, constant: 16)
        }
        return noResult
    }()

    private func configShimmer() {
        // Nền ĐỤC, trùng nền table: shimmer phủ lên table nên phải che hẳn danh sách cũ, nếu không
        // lúc tìm kiếm user thấy kết quả cũ lộ xuyên qua skeleton. Đối ứng `shimmerOverlay` ở
        // `MyPromotionViewController`.
        shimmerView.backgroundColor = Colors.tokenDark05
        // Chèn NGAY DƯỚI thanh đáy (chứa nút "Áp dụng") thay vì `addSubview` (luôn trên cùng):
        // shimmer bám theo bounds của table, mà table chạy xuống tận đáy màn nên nó phủ luôn nút.
        if let bottomBar = totalVoucherView.superview {
            view.insertSubview(shimmerView, belowSubview: bottomBar)
        } else {
            view.addSubview(shimmerView)
        }
        NSLayoutConstraint.activate([
            shimmerView.topAnchor.constraint(equalTo: promotionsTableView.topAnchor),
            shimmerView.leadingAnchor.constraint(equalTo: promotionsTableView.leadingAnchor),
            shimmerView.trailingAnchor.constraint(equalTo: promotionsTableView.trailingAnchor),
            shimmerView.bottomAnchor.constraint(equalTo: promotionsTableView.bottomAnchor)
        ])
    }

    private func wireApplyButton() {
        // Apply button là sibling của totalVoucherView trong cùng parent stack
        applyButton = totalVoucherView.superview?.subviews.first { $0 is UIButton } as? UIButton
        applyButton?.addTarget(self, action: #selector(didTapApplyButton), for: .touchUpInside)
        // Nút "Áp dụng" ở màn này là UIButton thường (không phải PRMButton) → áp button token thủ công.
        applyThemeToApplyButton(applyButton)
    }

    /**
     Chưa chọn voucher nào → **disable** nút "Áp dụng". `applySelected()` lọc ra danh sách rỗng rồi
     `guard` bỏ qua, nên để nút bấm được chỉ tạo cảm giác app treo. Đối ứng
     `ChoosePromotionFragment.updateApplyButtonState` bên Android.

     Android có sẵn nền disabled trong `PRMButton`; nút bên này là `UIButton` thường nên hạ `alpha`.
     */
    private func updateApplyButtonState(_ state: ChoosePromotionViewModel.Display) {
        let canApply = state.selectedCount > 0
        applyButton?.isEnabled = canApply
        applyButton?.alpha = canApply ? 1 : 0.5
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

    /// Bấm "Áp dụng" → trả offers đang chọn cho widget (`EndowStore` validate). VM lọc từ store,
    /// VC không tự đọc state. Đối ứng `ChoosePromotionFragment.onApplyClicked` bên Android.
    @objc private func didTapApplyButton() {
        let offers = viewModel.selectedOffers()
        guard !offers.isEmpty else { return }
        onApplyVoucher?(offers)
    }

    /// Màn Chi tiết (mở từ đây) bấm "Áp dụng" → tick voucher đó. Router gọi vào, VC chỉ forward.
    /// Đối ứng Android `ChoosePromotionFragment.listenApplyFromDetail`.
    func selectVoucherFromDetail(_ voucherId: String) {
        guard !voucherId.isEmpty else { return }
        viewModel.dispatch(ChoosePromotionIntentSetPreSelected(ids: [voucherId]))
    }
    
    private func configSearchTextField() {
        self.searchTextField.returnKeyType = .search
        self.searchTextField.delegate = self
        // Gõ mỗi ký tự → `queryChanged` (store lo debounce). Target-action thay `textPublisher` Combine.
        self.searchTextField.addTarget(self, action: #selector(searchTextChanged), for: .editingChanged)
    }

    @objc private func searchTextChanged(_ sender: UITextField) {
        viewModel.query(sender.text ?? "")
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

        viewModel.onDisplay = { [weak self] display in self?.render(display) }
        viewModel.onEffect = { [weak self] effect in self?.handle(effect) }

        viewModel.loadInitialIfNeeded()
    }

    private func render(_ state: ChoosePromotionViewModel.Display) {
        sections = state.sections
        promotionsTableView.reloadData()

        updateApplyButtonState(state)

        // Thanh "Đã chọn N voucher" — trạng thái do VM/store quyết định (đối ứng Android
        // `updateApplyButtonState`).
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

        // Gõ từ khoá mà không ra gì → view "không tìm thấy" thay cho list (đối ứng `showNoResult`
        // bên Android). List rỗng lúc không tìm kiếm thì để list trống như cũ.
        searchNoResultView.isHidden = !state.showsNoResult
        promotionsTableView.isHidden = state.showsNoResult

        renderLoadMore(state.isLoadingMoreOther)
    }

    /// Gắn/gỡ spinner ở đáy list. Chỉ đụng `tableFooterView` khi trạng thái thật sự đổi để không
    /// bắt table layout lại mỗi lần render. Giống `SearchMyPromotionViewController.renderLoadMore`.
    private func renderLoadMore(_ isLoadingMore: Bool) {
        let isShowing = promotionsTableView.tableFooterView === loadMoreSpinner
        guard isShowing != isLoadingMore else { return }

        if isLoadingMore {
            loadMoreSpinner.frame = CGRect(x: 0, y: 0, width: promotionsTableView.bounds.width, height: 44)
            promotionsTableView.tableFooterView = loadMoreSpinner
            loadMoreSpinner.startAnimating()
        } else {
            loadMoreSpinner.stopAnimating()
            promotionsTableView.tableFooterView = nil
        }
    }

    /// Lỗi nghiệp vụ → toast (đồng nhất Android `showToast(mapPromotionError(code))`).
    private func handle(_ effect: PRMEffect) {
        if let error = effect as? PRMEffectShowError {
            PromotionToast.show(PromotionUIStrings.errorMessage(error.errorCode), in: view)
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
        self.viewModel.dispatch(ChoosePromotionIntentToggleSelection(id: id))
    }

    func selectPromotionItemCellDidTapButton(_ cell: ChoosePromotionItemCell, id: String) {
        self.viewModel.openDetail(voucherId: id)
    }
}

// MARK: - UITextFieldDelegate (search action on return key)
extension ChoosePromotionViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        viewModel.dispatch(ChoosePromotionIntentSearch.shared)
        textField.resignFirstResponder()
        return true
    }

    func textField(_ textField: UITextField,
                   shouldChangeCharactersIn range: NSRange,
                   replacementString string: String) -> Bool {
        PromotionSearchLimit.shouldChange(textField, range: range, replacement: string)
    }
}

// MARK: - UITableViewDataSource & UITableViewDelegate
extension ChoosePromotionViewController: UITableViewDataSource, UITableViewDelegate {

    /// Kích thước vạch ngăn hai nhóm — chốt cứng theo thiết kế, bằng đúng bản Android
    /// (`prm_item_section_divider.xml`: cao 16dp, cách trên/dưới 8dp).
    fileprivate static let dividerHeight: CGFloat = 16
    fileprivate static let dividerGap: CGFloat = 8
    fileprivate static var dividerFooterHeight: CGFloat { dividerGap + dividerHeight + dividerGap }

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
            // Tên asset có tiền tố `prm_` (xem Assets.xcassets); thiếu tiền tố thì `UIImage.sdk` trả nil
            // và hàng mất hẳn icon.
            cell.configure(
                title: isCollapse ? "Thu gọn" : "Xem thêm",
                image: UIImage.sdk(isCollapse ? "prm_ic_up_arrow" : "prm_ic_down_arrow")
            )

            cell.action = { [weak self] in
                self?.viewModel.dispatch(ChoosePromotionIntentSeeMoreMy.shared)
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
            viewModel.dispatch(ChoosePromotionIntentLoadMoreOtherVouchers.shared)
        }
    }

    /// Grouped style tự thêm footer ~kích thước — khử về gần 0 để các section sát nhau.
    /// Riêng section có section khác đứng sau thì footer mang **vạch ngăn**: 8 + 16 + 8 = 32pt.
    /// Đối ứng `ChoosePromotionListItem.SectionDivider` bên Android.
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return hasDividerAfter(section) ? Self.dividerFooterHeight : .leastNormalMagnitude
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        guard hasDividerAfter(section) else { return UIView() }

        let container = UIView()
        container.backgroundColor = .clear
        let bar = UIView()
        bar.backgroundColor = Colors.tokenDark02          // #FBFBFB, khớp `color_FBFBFB` bên Android
        bar.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(bar)
        NSLayoutConstraint.activate([
            bar.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            bar.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            bar.topAnchor.constraint(equalTo: container.topAnchor, constant: Self.dividerGap),
            bar.heightAnchor.constraint(equalToConstant: Self.dividerHeight)
        ])
        return container
    }

    /// Vạch chỉ kẻ khi **còn section phía sau** — danh sách chỉ có một nhóm thì không kẻ (đối ứng
    /// nhánh `items.isNotEmpty()` bên Android). Vạch nằm ở footer nên tự đứng sau hàng "Xem thêm"
    /// khi hàng đó hiện, khoảng cách không đổi giữa hai trường hợp.
    private func hasDividerAfter(_ section: Int) -> Bool {
        section < sections.count - 1
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
