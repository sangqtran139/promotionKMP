//
//  MyPromotionViewController.swift
//  PromotionSDK
//
//  Created by thachlh on 6/5/26.
//

import UIKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMKotlinBridge

final class MyPromotionViewController: PRMBaseViewController<MyPromotionViewModel> {

    // MARK: - UI Components
    @IBOutlet private weak var promotionsTableview: PRMRefreshTableView!
    @IBOutlet private weak var expiringPromotionTabView: PromotionTabView!
    @IBOutlet private weak var allPromotionTabView: PromotionTabView!
    @IBOutlet private weak var headerView: PromotionHeaderView!
    @IBOutlet private weak var containerView: PRMBaseView!
    
    // Shimmer skeleton phủ CẢ 2 tab chip + list lúc tải trang đầu (giống Android).
    private let shimmerOverlay: UIView = {
        let view = UIView()
        view.backgroundColor = Colors.tokenDark02
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()
    private lazy var shimmerView: PRMShimmerReplicatorView = {
        let view = PRMShimmerReplicatorView(
            itemSize: .fixedHeight(PRMPromotionCardShimmerCell.itemHeight),
            cellProvider: { PRMPromotionCardShimmerCell() }
        )
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    // Skeleton hàng tab-chip (mặc định 3 chip) — hiện cùng shimmer list lúc tải trang đầu.
    private let tabShimmerStack: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        return stack
    }()
    private var tabShimmerChips: [PRMShimmerView] = []
    // Empty view khi list rỗng (đã tải xong) — khớp Android ctlNoResult (ảnh + tiêu đề + mô tả).
    private lazy var emptyView: PromotionSearchNoResultView = {
        let view = PromotionSearchNoResultView()
        view.thumbnailImage = UIImage.sdk("prm_il_chua_co_uu_dai_new_vers")
        view.title = PromotionUIStrings.emptyPromotionsTitle
        view.descriptionString = PromotionUIStrings.emptyPromotionsDescription
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()
    var onClose: (() -> Void)?
    /// Forward callback "Áp dụng" từ màn chi tiết (đẩy tiếp tới SDK boundary).
    var onApplyVoucher: ((String) -> Void)?

    // MARK: - Dynamic tabs (render N tab từ API thay vì 2 tab cố định)
    /// Nguồn dữ liệu list.
    private var promotionItems: [MyPromotionCellViewModel] = []
    private weak var tabContainer: UIView?
    // Cuộn ngang khi tổng bề rộng tab vượt màn (container XIB không pin trailing).
    private let tabScrollView: UIScrollView = {
        let scroll = UIScrollView()
        scroll.showsHorizontalScrollIndicator = false
        scroll.translatesAutoresizingMaskIntoConstraints = false
        return scroll
    }()
    private let tabStackView: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.spacing = 8
        // Canh giữa để chip cao cố định (chipHeight) thay vì kéo đầy chiều cao container → nhỏ gọn hơn.
        stack.alignment = .center
        stack.distribution = .fill
        stack.translatesAutoresizingMaskIntoConstraints = false
        return stack
    }()
    /// Chiều cao cố định của tab chip.
    private let tabChipHeight: CGFloat = 28
    private var tabViewsByCode: [String: PromotionTabView] = [:]
    private var currentTabCodes: [String] = []

    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if isMovingFromParent {
            onClose?()
        }
    }
    
    // MARK: - Setup UI
    override func setupUI() {
        self.containerView.backgroundColor = Colors.tokenDark02
        self.configHeaderView()
        self.configTabViews()
        self.configTableView()
        self.configShimmer()
        self.configEmptyView()
        super.setupUI()
    }

    private func configEmptyView() {
        self.view.addSubview(emptyView)
        // Căn giữa THEO VIEW (không theo table — table lệch xuống dưới header+tabs). Khớp màn Tìm kiếm.
        // Nhưng hạ ưu tiên để màn NHỎ, ràng buộc "không đè chips" bên dưới thắng.
        let centerY = emptyView.centerYAnchor.constraint(equalTo: self.view.centerYAnchor)
        centerY.priority = .defaultHigh
        NSLayoutConstraint.activate([
            emptyView.centerXAnchor.constraint(equalTo: self.view.centerXAnchor),
            centerY,
            // Màn nhỏ: chặn đỉnh empty view không trồi lên trên đỉnh list (list nằm dưới chips) → không đè thanh chip.
            emptyView.topAnchor.constraint(greaterThanOrEqualTo: promotionsTableview.topAnchor, constant: 8),
            emptyView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor, constant: 24),
            emptyView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: -24)
        ])
    }

    private func configShimmer() {
        self.view.addSubview(shimmerOverlay)
        shimmerOverlay.addSubview(tabShimmerStack)
        shimmerOverlay.addSubview(shimmerView)

        // Mặc định 3 chip skeleton cho hàng tab.
        (0..<3).forEach { _ in
            let chip = makeTabShimmerChip()
            tabShimmerChips.append(chip)
            tabShimmerStack.addArrangedSubview(chip)
        }

        let tabArea: UIView = tabContainer ?? containerView
        NSLayoutConstraint.activate([
            // Phủ từ đỉnh tab chip xuống đáy table → che cả tab + list.
            shimmerOverlay.topAnchor.constraint(equalTo: tabArea.topAnchor),
            shimmerOverlay.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            shimmerOverlay.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            shimmerOverlay.bottomAnchor.constraint(equalTo: promotionsTableview.bottomAnchor),

            // Hàng tab-chip skeleton — canh giữa theo vùng tab thật.
            tabShimmerStack.leadingAnchor.constraint(equalTo: shimmerOverlay.leadingAnchor, constant: 16),
            tabShimmerStack.centerYAnchor.constraint(equalTo: tabArea.centerYAnchor),

            // List skeleton bắt đầu DƯỚI hàng tab.
            shimmerView.topAnchor.constraint(equalTo: tabArea.bottomAnchor, constant: 8),
            shimmerView.leadingAnchor.constraint(equalTo: shimmerOverlay.leadingAnchor),
            shimmerView.trailingAnchor.constraint(equalTo: shimmerOverlay.trailingAnchor),
            shimmerView.bottomAnchor.constraint(equalTo: shimmerOverlay.bottomAnchor)
        ])
    }

    /// 1 chip skeleton (bo tròn) cho hàng tab — dùng PRMShimmerView để nhấp nháy như card skeleton.
    private func makeTabShimmerChip() -> PRMShimmerView {
        let chip = PRMShimmerView()
        chip.backgroundColor = Colors.tokenDark05
        chip.layer.cornerRadius = 16
        chip.clipsToBounds = true
        chip.translatesAutoresizingMaskIntoConstraints = false
        chip.widthAnchor.constraint(equalToConstant: 72).isActive = true
        chip.heightAnchor.constraint(equalToConstant: 32).isActive = true
        return chip
    }
    
    private func configTableView() {
        self.promotionsTableview.registerCell(MyPromotionCell.self)
        self.promotionsTableview.rowHeight = UITableView.automaticDimension
        // Bind list bằng dataSource cổ điển + reloadData khi mảng đổi.
        self.promotionsTableview.dataSource = self
        // Bật infinite scroll: kéo tới đáy -> `.loadMore`.
        self.promotionsTableview.hasInfinityScrolling = true
        // Kéo-làm-mới / cuộn-tới-đáy → action, qua closure của PRMRefreshTableView (không Combine).
        self.promotionsTableview.onRefresh = { [weak self] in self?.viewModel.dispatch(MyPromotionIntentRefresh.shared) }
        self.promotionsTableview.onLoadMore = { [weak self] in self?.viewModel.dispatch(MyPromotionIntentLoadMore.shared) }
    }
    
    private func configHeaderView() {
        self.headerView.titleText = "Ưu đãi của tôi"
        self.headerView.delegate = self
    }
    
    private func configTabViews() {
        // Lấy container đang chứa 2 tab cố định (XIB) → thay bằng scrollView ngang chứa stack N tab động từ API.
        guard let container = allPromotionTabView?.superview else { return }
        self.tabContainer = container
        allPromotionTabView?.removeFromSuperview()
        expiringPromotionTabView?.removeFromSuperview()

        container.addSubview(tabScrollView)
        tabScrollView.addSubview(tabStackView)

        var constraints: [NSLayoutConstraint] = [
            // ScrollView phủ kín container.
            tabScrollView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            tabScrollView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            tabScrollView.topAnchor.constraint(equalTo: container.topAnchor),
            tabScrollView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
            // Stack bám contentLayoutGuide để cuộn ngang; cao bằng vùng nhìn (không cuộn dọc).
            tabStackView.leadingAnchor.constraint(equalTo: tabScrollView.contentLayoutGuide.leadingAnchor),
            tabStackView.trailingAnchor.constraint(equalTo: tabScrollView.contentLayoutGuide.trailingAnchor),
            tabStackView.topAnchor.constraint(equalTo: tabScrollView.contentLayoutGuide.topAnchor),
            tabStackView.bottomAnchor.constraint(equalTo: tabScrollView.contentLayoutGuide.bottomAnchor),
            tabStackView.heightAnchor.constraint(equalTo: tabScrollView.frameLayoutGuide.heightAnchor)
        ]
        // Chặn bề rộng container theo cạnh phải màn (XIB không pin trailing) để scrollView cuộn thay vì tràn.
        if let parent = container.superview {
            constraints.append(container.trailingAnchor.constraint(equalTo: parent.trailingAnchor, constant: -20))
        }
        NSLayoutConstraint.activate(constraints)
    }

    /// Render tabs động: chỉ dựng lại view khi tập code đổi; còn lại cập nhật label/count + focus.
    ///
    /// - Parameter showCount: `false` khi **chưa load xong danh sách** → chỉ hiện tên tab, không hiện
    ///   "(số lượng)" (TLNV MOB_001 control #3). Đối ứng Android `submitTabs(..., showCount)`.
    private func renderTabs(_ tabs: [MyPromotionTab], selectedCode: String, showCount: Bool) {
        let codes = tabs.map { $0.code }
        if codes != currentTabCodes {
            tabStackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
            tabViewsByCode.removeAll()
            for tab in tabs {
                let tabView = PromotionTabView()
                let code = tab.code
                tabView.onTapped = { [weak self] in self?.viewModel.dispatch(MyPromotionIntentSelectTab(tabCode: code)) }
                tabStackView.addArrangedSubview(tabView)
                tabViewsByCode[code] = tabView
            }
            currentTabCodes = codes
        }
        for tab in tabs {
            let tabView = tabViewsByCode[tab.code]
            // MyPromotionTab.count là Int32 (store đã quyết định, non-null) — khỏi unwrap.
            tabView?.titleText = showCount ? "\(tab.label) (\(tab.count))" : tab.label
            tabView?.isFocusedState = (tab.code == selectedCode)
        }
    }
    
    // MARK: - Bind ViewModel
    //
    // Đối ứng `MyPromotionFragment.observeData` bên Android: một `render(state)` cho toàn bộ bề mặt,
    // một nhánh effect, rồi kích `loadInitialIfNeeded`. Gán `onState` là nhận ngay state hiện tại.
    override func bindViewModel() {
        super.bindViewModel()

        viewModel.onState = { [weak self] state in self?.render(state) }
        viewModel.onEffect = { [weak self] effect in self?.handle(effect) }

        viewModel.dispatch(MyPromotionIntentLoadInitialIfNeeded.shared)
    }

    private func render(_ state: MyPromotionState) {
        // `isLoading` của store là "đang gọi API"; màn chỉ che shimmer khi CHƯA có gì để hiện —
        // đối ứng `state.isLoading && state.vouchers.isEmpty` bên Android.
        let showShimmer = state.isLoading && state.vouchers.isEmpty
        // Loading state — hiện shimmer (che cả tab + list), ẩn khi data về.
        shimmerOverlay.isHidden = !showShimmer
        if showShimmer {
            shimmerView.startAnimating()
            tabShimmerChips.forEach { $0.startAnimating() }
        } else {
            shimmerView.stopAnimating()
            tabShimmerChips.forEach { $0.stopAnimating() }
        }

        if state.isRefreshing {
            promotionsTableview.startRefreshing()
        } else {
            promotionsTableview.stopRefreshing()
        }

        if state.isLoadingMore {
            promotionsTableview.startLoadingMore()
        } else {
            promotionsTableview.stopLoadingMore()
        }

        // Còn trang hay không -> infinite scroll dừng khi hết.
        promotionsTableview.isHasMorePage = !state.isLastPage

        // Render tabs động từ API (label + count) + focus theo tab đang chọn.
        renderTabs(state.tabs, selectedCode: state.selectedTabCode ?? "all", showCount: !state.isLoading)

        // Map model của store → cell view model của iOS ngay tại chỗ dùng.
        promotionItems = state.vouchers.map {
            MyPromotionCellViewModel(voucher: $0.source,
                                     isEnabled: $0.isEnabled,
                                     expiringInDays: $0.expiringInDays?.intValue)
        }
        promotionsTableview.reloadData()

        // Empty view khi list rỗng (đã tải xong) — khớp Android ctlNoResult.
        emptyView.isHidden = !(!state.isLoading && state.isEmpty)
    }

    /// Lỗi nghiệp vụ → toast (đồng nhất Android `showToast`).
    private func handle(_ effect: PRMEffect) {
        if let error = effect as? PRMEffectShowError {
            PromotionToast.show(PromotionUIStrings.errorMessage(error.errorCode), in: view)
        }
    }

    /// Giống `MyPromotionFragment.showServiceSelector` bên Android — cùng bottom sheet, cùng sự kiện host.
    private func showServiceSelector(voucherId: String, services: [ServiceSelectorItem]) {
        ServiceSelectorBottomSheet.present(from: self, services: services) { [weak self] service in
            PromotionSDK.getCallback()?.onServiceSelected(selection: PromotionServiceSelection(
                voucherId: voucherId,
                serviceCode: service.serviceCode,
                serviceName: service.serviceName,
                serviceType: service.serviceType,
                iconUrl: service.iconUrl
            ))
        }
    }
}

// MARK: - UITableViewDataSource
extension MyPromotionViewController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        promotionItems.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueCell(MyPromotionCell.self, for: indexPath)
        cell.delegate = self
        cell.bindData(promotionItems[indexPath.row])
        return cell
    }
}

//MARK: - PromotionHeaderViewDelegate
extension MyPromotionViewController: PromotionHeaderViewDelegate {
    func didTapBackButton() {
        viewModel.routeToParent()
    }
    
    func didTapSearchButton() {
        viewModel.routeToSearch()
    }
}

extension MyPromotionViewController: MyPromotionCellDelegate {
    func myPromotionCellDidTap(_ cell: MyPromotionCell, id: String) {
        viewModel.openDetail(voucherId: id)
    }

    func myPromotionCellDidTapUse(_ cell: MyPromotionCell, voucherId: String) {
        showServiceSelector(voucherId: voucherId, services: viewModel.serviceOptions(voucherId: voucherId))
    }
}
